package com.app.planification;

import com.app.models.Assignation;
import com.app.models.Hotel;
import com.app.models.Reservation;
import com.app.models.Vehicule;
import com.app.planification.TrajetCalculator;
import com.app.planification.TrajetComplet;
import com.app.planification.VoyageDTO;
import com.app.repository.AssignationRepository;
import com.app.repository.ReservationRepository;
import com.app.repository.VehiculeRepository;
import com.app.repository.DistanceRepository;
import com.app.repository.ParametreRepository;
import com.app.repository.HotelRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service métier pour la planification automatique des réservations vers les véhicules.
 * Implémente l'algorithme d'assignation selon les règles métier définies.
 */
public class PlanificationService {

    private static final Logger logger = Logger.getLogger(PlanificationService.class.getName());
    
    private final ReservationRepository reservationRepository;
    private final VehiculeRepository vehiculeRepository;
    private final AssignationRepository assignationRepository;
    private final TrajetCalculator trajetCalculator;
    private final HotelRepository hotelRepository;
    private final ParametreRepository parametreRepository;
    private final Random random = new Random();

    // Constantes DB
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/gestion_ticket";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    /**
     * Constructeur par défaut avec connexion DB standard
     */
    public PlanificationService() {
        this.reservationRepository = new ReservationRepository(DB_URL, DB_USER, DB_PASSWORD);
        this.vehiculeRepository = new VehiculeRepository(DB_URL, DB_USER, DB_PASSWORD);
        this.assignationRepository = new AssignationRepository(DB_URL, DB_USER, DB_PASSWORD);

        // Initialiser TrajetCalculator et ParametreRepository
        DistanceRepository distanceRepo = new DistanceRepository(DB_URL, DB_USER, DB_PASSWORD);
        this.parametreRepository = new ParametreRepository(DB_URL, DB_USER, DB_PASSWORD);
        this.hotelRepository = new HotelRepository(DB_URL, DB_USER, DB_PASSWORD);
        this.trajetCalculator = new TrajetCalculator(distanceRepo, parametreRepository, hotelRepository);
    }

    /**
     * Constructeur avec injection des repositories (pour tests)
     */
    public PlanificationService(ReservationRepository reservationRepo,
                                 VehiculeRepository vehiculeRepo,
                                 AssignationRepository assignationRepo,
                                 TrajetCalculator trajetCalc,
                                 HotelRepository hotelRepo,
                                 ParametreRepository parametreRepo) {
        this.reservationRepository = reservationRepo;
        this.vehiculeRepository = vehiculeRepo;
        this.assignationRepository = assignationRepo;
        this.trajetCalculator = trajetCalc;
        this.hotelRepository = hotelRepo;
        this.parametreRepository = parametreRepo;
    }

    /**
     * Assignation automatique : récupère les réservations non assignées
     * dont l'heure d'arrivée est <= debutIntervalle et crée des Assignation
     * en les liant au véhicule/groupage indiqué.
     * Retourne le nombre d'assignations créées.
     */
    public int assignerReservationsNonAssigneesAuIntervalle(LocalDate date, LocalDateTime debutIntervalle, int vehiculeId) {
        List<Reservation> candidats = reservationRepository.findUnassignedByDateAndArrivalBefore(date, debutIntervalle);
        int count = 0;
        for (Reservation r : candidats) {
            try {
                Assignation a = new Assignation();
                a.setReservationId(r.getIdReservation());
                a.setVehiculeId(vehiculeId);
                a.setDateHeurePlanification(LocalDateTime.now());
                a.setNbPersAssigne(r.getNbrPers());
                assignationRepository.save(a);
                count++;
                logger.info("Assignation auto : reservation " + r.getIdReservation() + " -> vehicule " + vehiculeId);
            } catch (Exception e) {
                logger.warning("Échec assignation auto pour réservation " + r.getIdReservation() + " : " + e.getMessage());
            }
        }
        return count;
    }

    /**
     * Méthode principale : Planifie les réservations pour une date donnée.
     * 
     * Règles métier SPRINT 4 :
     * - RG8: Remplissage progressif des véhicules avec plusieurs réservations
     * - Réinitialisation automatique des assignations existantes
     * - Les réservations sont groupées par vol (même date_heure_arrivee)
     * - RG7: Tri par nombre de passagers décroissant
     * 
     * @param date La date de planification
     * @return PlanificationResult contenant les véhicules assignés et les réservations non assignées
     */
    public PlanificationResult planifierJour(LocalDate date) {
        logger.info("Début de la planification pour la date : " + date);
        
        // Sprint 4 - Réinitialisation automatique avant recalcul
        // reinitialiserAssignations(date);
        
        PlanificationResult result = new PlanificationResult();
        
        // Étape 1 : Récupérer les réservations du jour
        List<Reservation> reservations = reservationRepository.findByDate(date);
        logger.info("Nombre de réservations trouvées : " + reservations.size());
        
        if (reservations.isEmpty()) {
            logger.info("Aucune réservation pour cette date.");
            return result;
        }
        
        // Étape 2 : Récupérer tous les véhicules disponibles
        List<Vehicule> tousVehicules = vehiculeRepository.findAll();
        logger.info("Nombre de véhicules disponibles : " + tousVehicules.size());
        
        if (tousVehicules.isEmpty()) {
            logger.warning("Aucun véhicule disponible. Toutes les réservations seront non assignées.");
            result.setReservationsNonAssignees(reservations);
            return result;
        }
        
        // Étape 3 : Map pour suivre l'heure de retour des véhicules (Sprint 3 - Réutilisation)
        // Clé: idVehicule, Valeur: heure de retour à l'aéroport
        Map<Integer, LocalDateTime> vehiculesHeureRetour = new HashMap<>();
        initialiserOccupationVehiculesDepuisAssignationsExistantes(date, reservations, vehiculesHeureRetour);
        logger.info("Réutilisation des véhicules activée (Sprint 3)");

        // Sprint 5 : Récupérer le temps d'attente depuis les paramètres
        int tempsAttente = 0; // Valeur par défaut
        com.app.models.Parametre parametre = parametreRepository.getParametre();
        if (parametre != null) {
            tempsAttente = parametre.getTempsAttente();
            logger.info("Sprint 5 - Temps d'attente récupéré : " + tempsAttente + " minutes");
        } else {
            logger.warning("Sprint 5 - Paramètre non trouvé, utilisation de la valeur par défaut : " + tempsAttente + " minutes");
        }

        // Étape 4 : Sprint 5 - Grouper les réservations par tranche de temps d'attente
        Map<LocalDateTime, List<Reservation>> groupesParVol = grouperParTrancheAttente(reservations, tempsAttente);
        logger.info("Nombre de groupes (tranches) : " + groupesParVol.size());
        
        // Étape 5 : Trier les groupes par heure d'arrivée ASC
        List<Map.Entry<LocalDateTime, List<Reservation>>> groupesTries = groupesParVol.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());
        
        // Map pour regrouper les assignations par véhicule pour le résultat
        Map<Integer, VehiculePlanDTO> vehiculePlans = new LinkedHashMap<>();
        
        // Étape 6 : Pour chaque groupe (vol), essayer d'assigner à UN véhicule
        for (Map.Entry<LocalDateTime, List<Reservation>> entry : groupesTries) {
            LocalDateTime heureVol = entry.getKey();
            List<Reservation> groupe = entry.getValue();
            
            // SPRINT 8 - TACHE 1 : Priorisation des réservations non assignées dans le prochain groupe
            // Identifier toutes les réservations non assignées de la journée
            // Les assigner automatiquement au groupe de l'intervalle actuel EN PRIORITÉ
            
            // Trouver l'heure d'arrivée la plus tôt dans ce groupe (début de l'intervalle)
            LocalDateTime debutIntervalle = groupe.stream()
                    .map(Reservation::getDateHeureArrivee)
                    .min(Comparator.naturalOrder())
                    .orElse(heureVol); // fallback
            
            logger.info("Sprint 8 - Tâche 1 : Priorisation des réservations non assignées avant " + debutIntervalle);
            
            // Récupérer toutes les réservations non assignées de la journée qui arrivent avant debutIntervalle
            List<Reservation> reservationsNonAssigneesAvant = reservationRepository
                    .findUnassignedByDateAndArrivalBefore(date, debutIntervalle);
            
            // Filtrer celles qui ne sont pas déjà dans le groupe actuel
            reservationsNonAssigneesAvant = reservationsNonAssigneesAvant.stream()
                    .filter(r -> groupe.stream().noneMatch(gr -> gr.getIdReservation() == r.getIdReservation()))
                    .collect(Collectors.toList());
            
            // Séparer les non assignées (pour priorisation) des nouvelles réservations du groupe
            List<Reservation> nouvellesResaGroupe = new ArrayList<>(groupe);
            List<Reservation> nonAssigneesTriees = new ArrayList<>();
            
            if (!reservationsNonAssigneesAvant.isEmpty()) {
                logger.info("Sprint 8 - Tâche 1 : " + reservationsNonAssigneesAvant.size() + 
                           " réservations non assignées trouvées avant " + debutIntervalle);
                
                // Trier les non assignées par heure d'arrivée (ASC)
                nonAssigneesTriees = reservationsNonAssigneesAvant.stream()
                        .sorted(Comparator.comparing(Reservation::getDateHeureArrivee))
                        .collect(Collectors.toList());
                
                logger.info("Sprint 8 - Tâche 1 : Non assignées triées : " + 
                           nonAssigneesTriees.stream()
                               .map(r -> r.getClientId() + " (" + r.getDateHeureArrivee() + ")")
                               .collect(Collectors.joining(", ")));
            }
            
            // Garder uniquement les nouvelles réservations du groupe
            groupe = nouvellesResaGroupe;
            
            // Filtrer les réservations déjà assignées
            List<Reservation> reservationsAAssigner = groupe.stream()
                    .filter(r -> assignationRepository.hasPassagersRestants(r.getIdReservation()))
                    .collect(Collectors.toList());
            
            // SPRINT 8 - TACHE 1 : Construire la liste avec priorisation
            // RG7: Trier les réservations par nombre de passagers décroissant
            // RG11: En cas d'égalité de nombre, tri alphabétique par nom d'hôtel
            
            Map<Integer, String> hotelNoms = hotelRepository.findAllHotels().stream()
                    .collect(Collectors.toMap(
                        hotel -> hotel.getIdHotel(),
                        hotel -> hotel.getNom()
                    ));
            
            // Trier les NOUVELLES réservations selon RG7/RG11
            List<Reservation> nouvellesResaTriees = reservationsAAssigner.stream()
                    .sorted((r1, r2) -> {
                        // Tri primaire : passagers RESTANTS décroissant (pas nbrPers original)
                        // Indispensable pour les réservations partiellement assignées (split sprint 7)
                        int restants1 = assignationRepository.getPassagersRestantsByReservationId(r1.getIdReservation());
                        int restants2 = assignationRepository.getPassagersRestantsByReservationId(r2.getIdReservation());
                        int compareNbr = Integer.compare(restants2, restants1);
                        if (compareNbr != 0) {
                            return compareNbr;
                        }
                        
                        // Tri secondaire : ordre alphabétique par nom d'hôtel (RG11)
                        String nom1 = hotelNoms.getOrDefault(r1.getHotelId(), "Hotel#" + r1.getHotelId());
                        String nom2 = hotelNoms.getOrDefault(r2.getHotelId(), "Hotel#" + r2.getHotelId());
                        return nom1.compareTo(nom2);
                    })
                    .collect(Collectors.toList());
            
            // Construire l'ordre final : Non assignées en PREMIER, puis nouvelles réservations
            List<Reservation> reservationsTriees = new ArrayList<>();
            reservationsTriees.addAll(nonAssigneesTriees);        // Priorité 1 : non assignées
            reservationsTriees.addAll(nouvellesResaTriees);       // Priorité 2 : nouvelles
            
            if (!nonAssigneesTriees.isEmpty()) {
                logger.info("Sprint 8 - Tâche 1 : Ordre d'assignation = " +
                           "Non assignées (" + nonAssigneesTriees.size() + ") → Nouvelles (" + nouvellesResaTriees.size() + ")");
            }
            
            if (reservationsTriees.isEmpty()) {
                logger.info("Toutes les réservations du groupe " + heureVol + " sont déjà assignées.");
                continue;
            }

            // SPRINT 8 - TACHE 1 : Assigner les non assignées d'abord, puis les nouvelles
            // Cette séparation garantit que les non assignées ont la priorité sur les véhicules
            
            // Phase 1 : Assigner les réservations NON ASSIGNÉES en priorité
            if (!nonAssigneesTriees.isEmpty()) {
                logger.info("Sprint 8 - Tâche 1 : Phase 1 - Assignation des réservations NON ASSIGNÉES (" + 
                           nonAssigneesTriees.size() + " réservations)");
                assignerAvecRemplissageProgressif(nonAssigneesTriees, heureVol, date, tousVehicules, 
                                                 vehiculesHeureRetour, vehiculePlans, result, tempsAttente);
            }
            
            // Phase 2 : Assigner les réservations NOUVELLES du groupe
            if (!nouvellesResaTriees.isEmpty()) {
                logger.info("Sprint 8 - Tâche 1 : Phase 2 - Assignation des réservations NOUVELLES (" + 
                           nouvellesResaTriees.size() + " réservations)");
                assignerAvecRemplissageProgressif(nouvellesResaTriees, heureVol, date, tousVehicules, 
                                                 vehiculesHeureRetour, vehiculePlans, result, tempsAttente);
            }
        }
        
        // Ajouter tous les plans au résultat
        for (VehiculePlanDTO plan : vehiculePlans.values()) {
            result.addVehiculePlan(plan);
        }
        
        logger.info("Planification terminée. Véhicules assignés: " + result.getNombreVehiculesUtilises() + 
                   ", Réservations non assignées: " + result.getNombreReservationsNonAssignees());
        
        return result;
    }

    /**
     * Initialise l'occupation des véhicules à partir des assignations déjà présentes en base
     * pour la date donnée. Permet de prendre en compte une simulation préalable d'assignation
     * avant de planifier les nouvelles réservations.
     */
    private void initialiserOccupationVehiculesDepuisAssignationsExistantes(
            LocalDate date,
            List<Reservation> reservationsDuJour,
            Map<Integer, LocalDateTime> vehiculesHeureRetour) {
        List<Assignation> assignationsExistantes = assignationRepository.findByDate(date);
        if (assignationsExistantes.isEmpty()) {
            return;
        }

        Map<Integer, Reservation> reservationById = reservationsDuJour.stream()
                .collect(Collectors.toMap(Reservation::getIdReservation, r -> r, (r1, r2) -> r1));

        Map<Integer, List<Reservation>> reservationsParVehicule = new HashMap<>();
        for (Assignation assignation : assignationsExistantes) {
            Reservation reservation = reservationById.get(assignation.getReservationId());
            if (reservation == null) {
                continue;
            }

            List<Reservation> reservationsVehicule = reservationsParVehicule
                    .computeIfAbsent(assignation.getVehiculeId(), key -> new ArrayList<>());

            boolean dejaPresente = reservationsVehicule.stream()
                    .anyMatch(r -> r.getIdReservation() == reservation.getIdReservation());
            if (!dejaPresente) {
                reservationsVehicule.add(reservation);
            }
        }

        for (Map.Entry<Integer, List<Reservation>> entry : reservationsParVehicule.entrySet()) {
            int vehiculeId = entry.getKey();
            List<Reservation> reservationsVehicule = entry.getValue();

            if (reservationsVehicule.isEmpty()) {
                continue;
            }

            LocalDateTime heureDepart = reservationsVehicule.stream()
                    .map(Reservation::getDateHeureArrivee)
                    .max(Comparator.naturalOrder())
                    .orElse(date.atStartOfDay());

            TrajetComplet trajetComplet = trajetCalculator.calculerTrajetComplet(heureDepart, reservationsVehicule);
            LocalDateTime heureRetour = trajetComplet.getHeureRetour();

            LocalDateTime ancienneHeureRetour = vehiculesHeureRetour.get(vehiculeId);
            if (ancienneHeureRetour == null || heureRetour.isAfter(ancienneHeureRetour)) {
                vehiculesHeureRetour.put(vehiculeId, heureRetour);
            }

            logger.info("Véhicule " + vehiculeId + " occupé par assignations existantes jusqu'à " + heureRetour);
        }
    }

    /**
     * SPRINT 3 - Groupe les réservations par vol exact (même date_heure_arrivee).
     * deprecated Remplacé par grouperParTrancheAttente() dans Sprint 5
     */
    @SuppressWarnings("unused")
    private Map<LocalDateTime, List<Reservation>> grouperParVolExact(List<Reservation> reservations) {
        return reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getDateHeureArrivee));
    }

    /**
     * SPRINT 5 - Groupe les réservations par tranche de temps d'attente.
     *
     * Algorithme :
     * 1. Trier les réservations par date_heure_arrivee ASC
     * 2. Premier vol = début de la tranche
     * 3. Fin tranche = début + tempsAttenteMinutes
     * 4. Si vol suivant <= fin tranche → même groupe
     * 5. Si vol suivant > fin tranche → nouveau groupe
     * 6. Clé de la Map = FIN DE TRANCHE (heure de départ effective pour réutilisation véhicules)
     *
     * @param reservations Liste des réservations (non triées)
     * @param tempsAttenteMinutes Durée de la tranche en minutes (ex: 30)
     * @return Map où clé = heure de départ (fin de la tranche), valeur = liste des réservations
     */
    private Map<LocalDateTime, List<Reservation>> grouperParTrancheAttente(List<Reservation> reservations, int tempsAttenteMinutes) {
        // Trier par date_heure_arrivee ASC
        List<Reservation> reservationsTriees = reservations.stream()
                .sorted(Comparator.comparing(Reservation::getDateHeureArrivee))
                .collect(Collectors.toList());

        // Map résultat - LinkedHashMap pour conserver l'ordre d'insertion
        Map<LocalDateTime, List<Reservation>> groupes = new LinkedHashMap<>();

        if (reservationsTriees.isEmpty()) {
            return groupes;
        }

        // Variables pour le groupe courant
        List<Reservation> groupeCourant = new ArrayList<>();
        LocalDateTime debutTranche = null;
        LocalDateTime finTranche = null;

        for (Reservation reservation : reservationsTriees) {
            LocalDateTime heureArrivee = reservation.getDateHeureArrivee();

            if (debutTranche == null) {
                // Première réservation → nouveau groupe
                debutTranche = heureArrivee;
                finTranche = debutTranche.plusMinutes(tempsAttenteMinutes);
                groupeCourant.add(reservation);
                logger.info("Sprint 5 - Nouvelle tranche : " + debutTranche + " → " + finTranche);
            } else if (!heureArrivee.isAfter(finTranche)) {
                // Réservation dans la tranche courante (heureArrivee <= finTranche)
                groupeCourant.add(reservation);
            } else {
                // Hors de la tranche → fermer groupe courant + nouveau groupe
                // Utiliser FIN DE TRANCHE comme clé pour la réutilisation de véhicules
                groupes.put(finTranche, new ArrayList<>(groupeCourant));
                logger.info("Sprint 5 - Groupe fermé : " + groupeCourant.size() + " réservations, départ = " + finTranche);

                // Nouveau groupe
                groupeCourant = new ArrayList<>();
                groupeCourant.add(reservation);
                debutTranche = heureArrivee;
                finTranche = debutTranche.plusMinutes(tempsAttenteMinutes);
                logger.info("Sprint 5 - Nouvelle tranche : " + debutTranche + " → " + finTranche);
            }
        }

        // Ne pas oublier le dernier groupe
        if (!groupeCourant.isEmpty()) {
            // Utiliser FIN DE TRANCHE comme clé pour la réutilisation de véhicules
            groupes.put(finTranche, groupeCourant);
            logger.info("Sprint 5 - Dernier groupe fermé : " + groupeCourant.size() + " réservations, départ = " + finTranche);
        }

        return groupes;
    }

    /**
     * Enregistre une assignation partielle/complète pour une réservation vers un véhicule.
     */
    private void enregistrerAssignation(Reservation reservation, Vehicule vehicule, LocalDate date, int nbPassagersAssignes) {
        LocalDateTime datePlanification = date.atStartOfDay();
        
        if (!assignationRepository.hasPassagersRestants(reservation.getIdReservation())) {
            logger.warning("Réservation " + reservation.getIdReservation() + " complètement assignée, ignorée.");
            return;
        }

        if (nbPassagersAssignes <= 0) {
            logger.warning("Aucun passager à assigner pour la réservation " + reservation.getIdReservation());
            return;
        }
        
        Assignation assignation = new Assignation();
        assignation.setReservationId(reservation.getIdReservation());
        assignation.setVehiculeId(vehicule.getIdVehicule());
        assignation.setDateHeurePlanification(datePlanification);
        assignation.setNbPersAssigne(nbPassagersAssignes);
        
        assignationRepository.save(assignation);
        logger.info("Assignation créée : Réservation " + reservation.getIdReservation() + 
                   " → Véhicule " + vehicule.getReference() +
                   " (" + nbPassagersAssignes + " passager(s))");
    }

    /**
     * Réinitialise les assignations pour une date donnée (SPRINT 4).
     * Supprime toutes les assignations existantes avant un nouveau calcul.
     * 
     * @param date Date de planification
     */
    private void reinitialiserAssignations(LocalDate date) {
        logger.info("Réinitialisation des assignations pour la date : " + date);
        // Les trajets seront automatiquement supprimés par CASCADE si la table existe
        assignationRepository.deleteByDate(date);
        logger.info("Assignations réinitialisées");
    }

    /**
     * RG8: Assigne les réservations avec remplissage progressif des véhicules.
     * Un véhicule peut transporter plusieurs réservations tant que la capacité le permet.
     * 
     * @param reservations Réservations triées par nombre de passagers décroissant
     * @param heureVol Heure du vol
     * @param date Date de planification
     * @param tousVehicules Liste de tous les véhicules disponibles
     * @param vehiculesHeureRetour Map des heures de retour des véhicules
     * @param vehiculePlans Map des plans de véhicules (résultat)
     * @param result Résultat de la planification
     */
    private void assignerAvecRemplissageProgressif(
            List<Reservation> reservations,
            LocalDateTime heureVol,
            LocalDate date,
            List<Vehicule> tousVehicules,
            Map<Integer, LocalDateTime> vehiculesHeureRetour,
            Map<Integer, VehiculePlanDTO> vehiculePlans,
            PlanificationResult result,
            int tempsAttente) {

        // Tri par passagers RESTANTS décroissant (pas nbrPers original) pour gérer les splits
        List<Reservation> reservationsTriees = new ArrayList<>(reservations);
        reservationsTriees.sort((r1, r2) -> {
            int restants1 = assignationRepository.getPassagersRestantsByReservationId(r1.getIdReservation());
            int restants2 = assignationRepository.getPassagersRestantsByReservationId(r2.getIdReservation());
            return Integer.compare(restants2, restants1);
        });

        Map<Integer, EtatVehiculeGroupe> etatsGroupe = new LinkedHashMap<>();

        // reservationsRestantes : liste ordonnée des réservations non encore entièrement traitées ce groupe
        List<Reservation> reservationsRestantes = new ArrayList<>(reservationsTriees);

        while (!reservationsRestantes.isEmpty()) {
            // Choisir la prochaine réservation principale (passagers restants DESC)
            Reservation reservation = choisirProchaineReservationPourSplit(reservationsRestantes, etatsGroupe.values());
            if (reservation == null) {
                break;
            }
            reservationsRestantes.removeIf(r -> r.getIdReservation() == reservation.getIdReservation());

            int passagersRestants = assignationRepository.getPassagersRestantsByReservationId(reservation.getIdReservation());

            while (passagersRestants > 0) {
                final int besoinRestant = passagersRestants;

                // Véhicules "non entamés" disponibles pour ouvrir un nouvel état dans CE groupe
                List<Vehicule> vehiculesDisponibles = tousVehicules.stream()
                        .filter(v -> !etatsGroupe.containsKey(v.getIdVehicule()))
                        .filter(v -> estVehiculeDisponible(v, vehiculesHeureRetour, heureVol))
                        .collect(Collectors.toList());

                // Split autorisé uniquement si AUCUN véhicule unique (entamé ou neuf) ne peut absorber tout le reste
                boolean existeVehiculeEntameCapable = etatsGroupe.values().stream()
                        .anyMatch(e -> e.capaciteRestante >= besoinRestant);
                boolean existeVehiculeNeufCapable = vehiculesDisponibles.stream()
                        .anyMatch(v -> v.getNbrPlaces() >= besoinRestant);
                boolean splitAutorise = !(existeVehiculeEntameCapable || existeVehiculeNeufCapable);

                if (!splitAutorise) {
                    // 1) Priorité : véhicule entamé capable de tout prendre
                    EtatVehiculeGroupe entameCapable = choisirVehiculeEntameCapable(etatsGroupe.values(), passagersRestants, vehiculePlans);
                    if (entameCapable != null) {
                        entameCapable.ajouterReservation(reservation, passagersRestants);
                        enregistrerAssignation(reservation, entameCapable.vehicule, date, passagersRestants);
                        passagersRestants = 0;
                        continue;
                    }

                    // 2) Ouvrir un nouveau véhicule capable (plus proche du besoin + tie-breakers)
                    List<Vehicule> capables = vehiculesDisponibles.stream()
                            .filter(v -> v.getNbrPlaces() >= besoinRestant)
                            .collect(Collectors.toList());
                    Vehicule vehiculeChoisi = choisirAvecTieBreakers(capables, passagersRestants, true, vehiculePlans);
                    if (vehiculeChoisi == null) {
                        break;
                    }
                    EtatVehiculeGroupe etat = new EtatVehiculeGroupe(vehiculeChoisi);
                    etatsGroupe.put(vehiculeChoisi.getIdVehicule(), etat);
                    etat.ajouterReservation(reservation, passagersRestants);
                    enregistrerAssignation(reservation, vehiculeChoisi, date, passagersRestants);
                    passagersRestants = 0;

                    // --- REMPLISSAGE PRIORITAIRE DES ENTAMÉS (Sprint 7) ---
                    // Après avoir ouvert ce nouveau véhicule avec des places restantes,
                    // chercher dans les réservations suivantes celles qui peuvent le remplir,
                    // par ordre de proximité de capacité restante du véhicule.
                    remplirVehiculesEntamesAvecReservationsRestantes(
                            etatsGroupe, reservationsRestantes, date, vehiculePlans, result);
                    continue;
                }

                // splitAutorise == true : répartir sur entamés d'abord, sinon nouveau véhicule
                EtatVehiculeGroupe vehiculeEntame = choisirVehiculeEntame(etatsGroupe.values(), passagersRestants, vehiculePlans);
                if (vehiculeEntame != null) {
                    int nbAssignes = Math.min(passagersRestants, vehiculeEntame.capaciteRestante);
                    vehiculeEntame.ajouterReservation(reservation, nbAssignes);
                    enregistrerAssignation(reservation, vehiculeEntame.vehicule, date, nbAssignes);
                    passagersRestants -= nbAssignes;
                    continue;
                }

                if (vehiculesDisponibles.isEmpty()) {
                    break;
                }
                Vehicule vehiculeChoisi = choisirVehiculePourBesoin(passagersRestants, vehiculesDisponibles, vehiculePlans);
                if (vehiculeChoisi == null) {
                    break;
                }
                EtatVehiculeGroupe etat = new EtatVehiculeGroupe(vehiculeChoisi);
                etatsGroupe.put(vehiculeChoisi.getIdVehicule(), etat);
                int nbAssignes = Math.min(passagersRestants, vehiculeChoisi.getNbrPlaces());
                etat.ajouterReservation(reservation, nbAssignes);
                enregistrerAssignation(reservation, vehiculeChoisi, date, nbAssignes);
                passagersRestants -= nbAssignes;
            }

            mettreAJourReservationNonAssignee(result, reservation, passagersRestants);
        }

        // Calcul de l'heure de départ commune du groupe :
        // Règle : tous les véhicules du groupe partent à la même heure, qui est :
        //   max(
        //     max(dateHeureArrivee de TOUTES les réservations de TOUS les voyages du groupe),
        //     max(heureRetourPrecedente de TOUS les véhicules utilisés dans ce groupe)
        //   )
        // Ainsi, si un véhicule réutilisé revient à 09:24 et que les vols arrivent avant,
        // tout le groupe attend 09:24 pour partir ensemble.
        LocalDateTime heureDepartGroupeCommune = LocalDateTime.MIN;
        for (EtatVehiculeGroupe etatTemp : etatsGroupe.values()) {
            // Max des heures d'arrivée des vols de ce voyage
            LocalDateTime dernierVolTemp = etatTemp.reservationsVoyage.stream()
                    .map(Reservation::getDateHeureArrivee)
                    .max(Comparator.naturalOrder())
                    .orElse(heureVol);
            if (dernierVolTemp.isAfter(heureDepartGroupeCommune)) {
                heureDepartGroupeCommune = dernierVolTemp;
            }
            // Heure de retour du voyage précédent de ce véhicule (s'il existe)
            LocalDateTime retourPrecTemp = vehiculesHeureRetour.get(etatTemp.vehicule.getIdVehicule());
            if (retourPrecTemp != null && retourPrecTemp.isAfter(heureDepartGroupeCommune)) {
                heureDepartGroupeCommune = retourPrecTemp;
            }
        }
        final LocalDateTime heureDepartCommune = heureDepartGroupeCommune;

        // Finaliser les voyages du groupe
        for (EtatVehiculeGroupe etat : etatsGroupe.values()) {
            List<Reservation> reservationsVoyage = new ArrayList<>(etat.reservationsVoyage);

            // Tous les véhicules du groupe partent à la même heure commune
            LocalDateTime heureDepartVehicule = heureDepartCommune;

            TrajetComplet trajetComplet = trajetCalculator.calculerTrajetComplet(heureDepartVehicule, reservationsVoyage);
            vehiculesHeureRetour.put(etat.vehicule.getIdVehicule(), trajetComplet.getHeureRetour());

            VehiculePlanDTO vehiculePlan;
            if (!vehiculePlans.containsKey(etat.vehicule.getIdVehicule())) {
                vehiculePlan = new VehiculePlanDTO(etat.vehicule, new ArrayList<>());
                vehiculePlans.put(etat.vehicule.getIdVehicule(), vehiculePlan);
            } else {
                vehiculePlan = vehiculePlans.get(etat.vehicule.getIdVehicule());
            }

            int numeroVoyage = vehiculePlan.getNombreVoyages() + 1;
            VoyageDTO voyage = new VoyageDTO(
                    numeroVoyage,
                    heureDepartVehicule,
                    trajetComplet.getHeureRetour(),
                    trajetComplet.getDistanceTotale(),
                    reservationsVoyage,
                    trajetComplet.getDetailsTrajet()
            );

            voyage.setPassagersAssignesParReservation(new LinkedHashMap<>(etat.passagersAssignesParReservation));
            vehiculePlan.addVoyage(voyage);
            vehiculePlan.getReservations().addAll(reservationsVoyage);
        }
    }

    /**
     * Sprint 7 - Remplissage prioritaire des véhicules entamés.
     * Après ouverture d'un véhicule avec des places restantes, parcourt les réservations
     * restantes et assigne celles dont le besoin est le plus proche de la capacité disponible.
     * Une réservation complètement traitée est retirée de reservationsRestantes.
     */
    private void remplirVehiculesEntamesAvecReservationsRestantes(
            Map<Integer, EtatVehiculeGroupe> etatsGroupe,
            List<Reservation> reservationsRestantes,
            LocalDate date,
            Map<Integer, VehiculePlanDTO> vehiculePlans,
            PlanificationResult result) {

        boolean progression = true;
        while (progression) {
            progression = false;

            // Trouver le véhicule entamé avec des places restantes
            // Priorité : celui dont la capacité restante est la plus petite (à remplir en premier)
            EtatVehiculeGroupe cible = etatsGroupe.values().stream()
                    .filter(e -> e.capaciteRestante > 0)
                    .min(Comparator.comparingInt(e -> e.capaciteRestante))
                    .orElse(null);

            if (cible == null || reservationsRestantes.isEmpty()) {
                break;
            }

            // Parmi les réservations restantes, trouver celle dont le besoin restant
            // est le plus proche de la capacité disponible du véhicule cible
            final int placesDispos = cible.capaciteRestante;
            Reservation meilleure = reservationsRestantes.stream()
                    .filter(r -> assignationRepository.getPassagersRestantsByReservationId(r.getIdReservation()) > 0)
                    .min(Comparator.comparingInt(r -> {
                        int restants = assignationRepository.getPassagersRestantsByReservationId(r.getIdReservation());
                        return Math.abs(restants - placesDispos);
                    }))
                    .orElse(null);

            if (meilleure == null) {
                break;
            }

            int passagersRestantsMeilleure = assignationRepository.getPassagersRestantsByReservationId(meilleure.getIdReservation());
            int nbAssignes = Math.min(passagersRestantsMeilleure, placesDispos);

            cible.ajouterReservation(meilleure, nbAssignes);
            enregistrerAssignation(meilleure, cible.vehicule, date, nbAssignes);
            int resteApres = passagersRestantsMeilleure - nbAssignes;

            if (resteApres <= 0) {
                // Réservation complètement traitée : retirer de la liste principale
                reservationsRestantes.removeIf(r -> r.getIdReservation() == meilleure.getIdReservation());
            }
            // Sinon elle reste dans reservationsRestantes pour traitement ultérieur

            mettreAJourReservationNonAssignee(result, meilleure, resteApres);
            progression = true;
        }
    }

    private Reservation choisirProchaineReservationPourSplit(List<Reservation> reservationsRestantes,
                                                         Collection<EtatVehiculeGroupe> etatsGroupe) {
        if (reservationsRestantes == null || reservationsRestantes.isEmpty()) {
            return null;
        }

        return reservationsRestantes.stream()
            .max((r1, r2) -> {
                int restants1 = assignationRepository.getPassagersRestantsByReservationId(r1.getIdReservation());
                int restants2 = assignationRepository.getPassagersRestantsByReservationId(r2.getIdReservation());

                if (restants1 != restants2) {
                    return Integer.compare(restants1, restants2); // DESC via max()
                }

                int cmpArrivee = r2.getDateHeureArrivee().compareTo(r1.getDateHeureArrivee());
                if (cmpArrivee != 0) {
                    return cmpArrivee; // DESC via max(): on inverse ici pour favoriser plus tôt si égalité de restants
                }

                return Integer.compare(r2.getIdReservation(), r1.getIdReservation());
            })
            .orElse(reservationsRestantes.get(0));
    }

    private int deltaCapaciteLePlusProche(int passagersRestants,
                                          Collection<EtatVehiculeGroupe> vehiculesEntames) {
        return vehiculesEntames.stream()
                .mapToInt(e -> Math.abs(e.capaciteRestante - passagersRestants))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private void mettreAJourReservationNonAssignee(PlanificationResult result,
                                                   Reservation reservation,
                                                   int passagersRestants) {
        if (result.getReservationsNonAssignees() == null) {
            result.setReservationsNonAssignees(new ArrayList<>());
        }

        List<Reservation> nonAssignees = result.getReservationsNonAssignees();
        nonAssignees.removeIf(r -> r.getIdReservation() == reservation.getIdReservation());

        if (passagersRestants <= 0) {
            return;
        }

        Reservation reste = new Reservation(
                reservation.getIdReservation(),
                reservation.getClientId(),
                passagersRestants,
                reservation.getDateHeureArrivee(),
                reservation.getHotelId());
        nonAssignees.add(reste);
    }

    private boolean estVehiculeDisponible(Vehicule vehicule,
                                          Map<Integer, LocalDateTime> vehiculesHeureRetour,
                                          LocalDateTime heureVolActuel) {
        Integer vehiculeId = vehicule.getIdVehicule();
        if (!vehiculesHeureRetour.containsKey(vehiculeId)) {
            return true;
        }

        LocalDateTime heureRetour = vehiculesHeureRetour.get(vehiculeId);
        return heureRetour.isBefore(heureVolActuel) || heureRetour.isEqual(heureVolActuel);
    }

    private Vehicule choisirVehiculePourBesoin(int passagersRestants,
                                               List<Vehicule> vehiculesDisponibles,
                                               Map<Integer, VehiculePlanDTO> vehiculePlans) {
        List<Vehicule> capables = vehiculesDisponibles.stream()
                .filter(v -> v.getNbrPlaces() >= passagersRestants)
                .collect(Collectors.toList());

        if (!capables.isEmpty()) {
            return choisirAvecTieBreakers(capables, passagersRestants, true, vehiculePlans);
        }

        List<Vehicule> splitCandidats = vehiculesDisponibles.stream()
                .filter(v -> v.getNbrPlaces() > 0)
                .collect(Collectors.toList());

        if (splitCandidats.isEmpty()) {
            return null;
        }

        return choisirAvecTieBreakers(splitCandidats, passagersRestants, false, vehiculePlans);
    }

    private Vehicule choisirAvecTieBreakers(List<Vehicule> candidats,
                                            int passagersRestants,
                                            boolean modeCapable,
                                            Map<Integer, VehiculePlanDTO> vehiculePlans) {
        if (candidats == null || candidats.isEmpty()) {
            return null;
        }

        int meilleurDelta = candidats.stream()
                .mapToInt(v -> modeCapable ? (v.getNbrPlaces() - passagersRestants) : (passagersRestants - v.getNbrPlaces()))
                .filter(delta -> delta >= 0)
                .min()
                .orElse(Integer.MAX_VALUE);

        List<Vehicule> plusProches = candidats.stream()
                .filter(v -> {
                    int delta = modeCapable ? (v.getNbrPlaces() - passagersRestants) : (passagersRestants - v.getNbrPlaces());
                    return delta == meilleurDelta;
                })
                .collect(Collectors.toList());

        int minTrajets = plusProches.stream()
                .mapToInt(v -> vehiculePlans.containsKey(v.getIdVehicule())
                        ? vehiculePlans.get(v.getIdVehicule()).getNombreVoyages()
                        : 0)
                .min()
                .orElse(0);

        List<Vehicule> moinsTrajets = plusProches.stream()
                .filter(v -> {
                    int trajets = vehiculePlans.containsKey(v.getIdVehicule())
                            ? vehiculePlans.get(v.getIdVehicule()).getNombreVoyages()
                            : 0;
                    return trajets == minTrajets;
                })
                .collect(Collectors.toList());

        List<Vehicule> diesels = moinsTrajets.stream()
                .filter(v -> "D".equals(v.getTypeCarburant()))
                .collect(Collectors.toList());

        List<Vehicule> finalistes = !diesels.isEmpty() ? diesels : moinsTrajets;
        return finalistes.get(random.nextInt(finalistes.size()));
    }

    private EtatVehiculeGroupe choisirVehiculeEntame(Collection<EtatVehiculeGroupe> etats,
                                                     int passagersRestants,
                                                     Map<Integer, VehiculePlanDTO> vehiculePlans) {
        List<EtatVehiculeGroupe> disponibles = etats.stream()
                .filter(e -> e.capaciteRestante > 0)
                .collect(Collectors.toList());

        if (disponibles.isEmpty()) {
            return null;
        }

        int meilleurDelta = disponibles.stream()
                .mapToInt(e -> Math.abs(e.capaciteRestante - passagersRestants))
                .min()
                .orElse(Integer.MAX_VALUE);

        List<EtatVehiculeGroupe> plusProches = disponibles.stream()
                .filter(e -> Math.abs(e.capaciteRestante - passagersRestants) == meilleurDelta)
                .collect(Collectors.toList());

        int minTrajets = plusProches.stream()
                .mapToInt(e -> vehiculePlans.containsKey(e.vehicule.getIdVehicule())
                        ? vehiculePlans.get(e.vehicule.getIdVehicule()).getNombreVoyages()
                        : 0)
                .min()
                .orElse(0);

        List<EtatVehiculeGroupe> moinsTrajets = plusProches.stream()
                .filter(e -> {
                    int trajets = vehiculePlans.containsKey(e.vehicule.getIdVehicule())
                            ? vehiculePlans.get(e.vehicule.getIdVehicule()).getNombreVoyages()
                            : 0;
                    return trajets == minTrajets;
                })
                .collect(Collectors.toList());

        List<EtatVehiculeGroupe> diesels = moinsTrajets.stream()
                .filter(e -> "D".equals(e.vehicule.getTypeCarburant()))
                .collect(Collectors.toList());

        List<EtatVehiculeGroupe> finalistes = !diesels.isEmpty() ? diesels : moinsTrajets;
        return finalistes.get(random.nextInt(finalistes.size()));
    }

    // 26032026
    private EtatVehiculeGroupe choisirVehiculeEntameCapable(Collection<EtatVehiculeGroupe> etats,
                                                            int passagersRestants,
                                                            Map<Integer, VehiculePlanDTO> vehiculePlans) {
        List<EtatVehiculeGroupe> capables = etats.stream()
                .filter(e -> e.capaciteRestante >= passagersRestants)
                .collect(Collectors.toList());

        if (capables.isEmpty()) {
            return null;
        }

        // "Plus proche" au-dessus: minimiser (capaciteRestante - besoin)
        int meilleurDelta = capables.stream()
                .mapToInt(e -> (e.capaciteRestante - passagersRestants))
                .min()
                .orElse(Integer.MAX_VALUE);

        List<EtatVehiculeGroupe> plusProches = capables.stream()
                .filter(e -> (e.capaciteRestante - passagersRestants) == meilleurDelta)
                .collect(Collectors.toList());

        // Tie-breaker: nb trajets min
        int minTrajets = plusProches.stream()
                .mapToInt(e -> vehiculePlans.containsKey(e.vehicule.getIdVehicule())
                        ? vehiculePlans.get(e.vehicule.getIdVehicule()).getNombreVoyages()
                        : 0)
                .min()
                .orElse(0);

        List<EtatVehiculeGroupe> moinsTrajets = plusProches.stream()
                .filter(e -> {
                    int trajets = vehiculePlans.containsKey(e.vehicule.getIdVehicule())
                            ? vehiculePlans.get(e.vehicule.getIdVehicule()).getNombreVoyages()
                            : 0;
                    return trajets == minTrajets;
                })
                .collect(Collectors.toList());

        // Diesel prioritaire
        List<EtatVehiculeGroupe> diesels = moinsTrajets.stream()
                .filter(e -> "D".equals(e.vehicule.getTypeCarburant()))
                .collect(Collectors.toList());

        List<EtatVehiculeGroupe> finalistes = !diesels.isEmpty() ? diesels : moinsTrajets;
        return finalistes.get(random.nextInt(finalistes.size()));
    }


    private static class EtatVehiculeGroupe {
        private final Vehicule vehicule;
        private int capaciteRestante;
        private final List<Reservation> reservationsVoyage;
        private final Map<Integer, Integer> passagersAssignesParReservation;

        private EtatVehiculeGroupe(Vehicule vehicule) {
            this.vehicule = vehicule;
            this.capaciteRestante = vehicule.getNbrPlaces();
            this.reservationsVoyage = new ArrayList<>();
            this.passagersAssignesParReservation = new LinkedHashMap<>();
        }

        private void ajouterReservation(Reservation reservation, int nbPassagers) {
            if (nbPassagers <= 0) {
                return;
            }

            if (!passagersAssignesParReservation.containsKey(reservation.getIdReservation())) {
                reservationsVoyage.add(reservation);
            }

            passagersAssignesParReservation.merge(reservation.getIdReservation(), nbPassagers, Integer::sum);
            capaciteRestante -= nbPassagers;
        }
    }
}