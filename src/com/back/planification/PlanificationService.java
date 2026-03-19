package com.app.planification;

import com.app.models.Assignation;
import com.app.models.Hotel;
import com.app.models.Reservation;
import com.app.models.Vehicule;
import com.app.planification.TrajetCalculator;
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
        reinitialiserAssignations(date);
        
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
            
            // SPRINT 5 - TACHE 1 : Assignation automatique des réservations non assignées
            // Identifier toutes les réservations non assignées de la journée
            // Sélectionner celles dont l'heure d'arrivée est avant le début du nouvel intervalle de départ
            // Les assigner automatiquement au groupe de l'intervalle actuel
            
            // Trouver l'heure d'arrivée la plus tôt dans ce groupe (début de l'intervalle)
            LocalDateTime debutIntervalle = groupe.stream()
                    .map(Reservation::getDateHeureArrivee)
                    .min(Comparator.naturalOrder())
                    .orElse(heureVol); // fallback
            
            logger.info("Sprint 5 - Tâche 1 : Recherche de réservations non assignées avant " + debutIntervalle);
            
            // Récupérer toutes les réservations non assignées de la journée qui arrivent avant debutIntervalle
            List<Reservation> reservationsNonAssigneesAvant = reservationRepository
                    .findUnassignedByDateAndArrivalBefore(date, debutIntervalle);
            
            // Filtrer celles qui ne sont pas déjà dans le groupe actuel
            reservationsNonAssigneesAvant = reservationsNonAssigneesAvant.stream()
                    .filter(r -> groupe.stream().noneMatch(gr -> gr.getIdReservation() == r.getIdReservation()))
                    .collect(Collectors.toList());
            
            if (!reservationsNonAssigneesAvant.isEmpty()) {
                logger.info("Sprint 5 - Tâche 1 : " + reservationsNonAssigneesAvant.size() + 
                           " réservations non assignées trouvées avant " + debutIntervalle + 
                           ", ajoutées au groupe actuel");
                
                // Ajouter ces réservations au groupe actuel
                groupe.addAll(reservationsNonAssigneesAvant);
                
                // Retrier le groupe par heure d'arrivée pour maintenir l'ordre
                groupe.sort(Comparator.comparing(Reservation::getDateHeureArrivee));
            }
            
            // Filtrer les réservations déjà assignées
            List<Reservation> reservationsAAssigner = groupe.stream()
                    .filter(r -> assignationRepository.hasPassagersRestants(r.getIdReservation()))
                    .collect(Collectors.toList());
            
            if (reservationsAAssigner.isEmpty()) {
                logger.info("Toutes les réservations du groupe " + heureVol + " sont déjà assignées.");
                continue;
            }
            
            // Calculer le total de personnes pour ce groupe
            int totalPersonnesGroupe = reservationsAAssigner.stream()
                    .mapToInt(Reservation::getNbrPers)
                    .sum();
            
            logger.info("Groupe vol " + heureVol + " : " + reservationsAAssigner.size() + 
                       " réservations, " + totalPersonnesGroupe + " personnes");
            
            // RG7: Trier les réservations par nombre de passagers décroissant
            // RG11: En cas d'égalité de nombre, tri alphabétique par nom d'hôtel
            // Récupérer les noms d'hôtels pour le tri
            Map<Integer, String> hotelNoms = hotelRepository.findAllHotels().stream()
                    .collect(Collectors.toMap(
                        hotel -> hotel.getIdHotel(),
                        hotel -> hotel.getNom()
                    ));
            
            List<Reservation> reservationsTriees = reservationsAAssigner.stream()
                    .sorted((r1, r2) -> {
                        // Tri primaire : nombre de personnes décroissant
                        int compareNbr = Integer.compare(r2.getNbrPers(), r1.getNbrPers());
                        if (compareNbr != 0) {
                            return compareNbr;
                        }
                        
                        // Tri secondaire : ordre alphabétique par nom d'hôtel (RG11)
                        String nom1 = hotelNoms.getOrDefault(r1.getHotelId(), "Hotel#" + r1.getHotelId());
                        String nom2 = hotelNoms.getOrDefault(r2.getHotelId(), "Hotel#" + r2.getHotelId());
                        return nom1.compareTo(nom2);
                    })
                    .collect(Collectors.toList());
            
            logger.info("Réservations triées (RG7+RG11) : " + 
                       reservationsTriees.stream()
                           .map(r -> r.getClientId() + " (" + r.getNbrPers() + " pers, " + 
                                     hotelNoms.getOrDefault(r.getHotelId(), "Hotel#" + r.getHotelId()) + ")")
                           .collect(Collectors.joining(", ")));

            
            // RG8: Remplissage progressif des véhicules
            assignerAvecRemplissageProgressif(reservationsTriees, heureVol, date, tousVehicules, 
                                             vehiculesHeureRetour, vehiculePlans, result, tempsAttente);
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

    // NOUVEAU Sprint 7 : remplir un véhicule entamé avec UNE réservation "plus bas" (liste vers le bas)
    // choisie par : reste >= placesRestantes et delta minimal.
    private void remplirVehiculeEntameAvecReservationPlusBas(
            EtatVehiculeGroupe etatVehicule,
            List<Reservation> reservationsTriees,
            int startIndex,
            Map<Integer, Integer> restantsParReservation,
            LocalDate date) {

        if (etatVehicule == null || etatVehicule.capaciteRestante <= 0) {
            return;
        }

        int placesRestantes = etatVehicule.capaciteRestante;

        Reservation meilleure = null;
        int meilleurDelta = Integer.MAX_VALUE;

        for (int j = startIndex; j < reservationsTriees.size(); j++) {
            Reservation candidate = reservationsTriees.get(j);
            int restants = restantsParReservation.getOrDefault(candidate.getIdReservation(), 0);

            if (restants < placesRestantes) {
                continue; // règle: il faut au moins de quoi remplir le reste
            }

            int delta = restants - placesRestantes;
            if (delta < meilleurDelta) {
                meilleurDelta = delta;
                meilleure = candidate;
                if (delta == 0) {
                    break; // best possible
                }
            }
        }

        if (meilleure == null) {
            return; // pas de réservation compatible => on laisse le véhicule partiellement vide
        }

        int resaId = meilleure.getIdReservation();
        int restantsAvant = restantsParReservation.getOrDefault(resaId, 0);

        // On split la réservation choisie pour EXACTEMENT remplir les places restantes
        etatVehicule.ajouterReservation(meilleure, placesRestantes);
        enregistrerAssignation(meilleure, etatVehicule.vehicule, date, placesRestantes);

        restantsParReservation.put(resaId, Math.max(0, restantsAvant - placesRestantes));
    }

    // Choisit un véhicule entamé CAPABLE de prendre tout le besoin (pas de split)
    // plus proche (min(capaciteRestante - besoin)), puis tie-breakers.
    private EtatVehiculeGroupe choisirVehiculeEntameCapable(
            Collection<EtatVehiculeGroupe> etats,
            int besoin,
            Map<Integer, VehiculePlanDTO> vehiculePlans) {

        List<EtatVehiculeGroupe> capables = etats.stream()
                .filter(e -> e.capaciteRestante >= besoin)
                .collect(Collectors.toList());

        if (capables.isEmpty()) {
            return null;
        }

        int meilleurDelta = capables.stream()
                .mapToInt(e -> e.capaciteRestante - besoin)
                .min()
                .orElse(Integer.MAX_VALUE);

        List<EtatVehiculeGroupe> plusProches = capables.stream()
                .filter(e -> (e.capaciteRestante - besoin) == meilleurDelta)
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

    // Choisit un véhicule entamé pour un SPLIT (donc il est forcément < besoin)
    // on veut le "plus proche en dessous" => capacitéRestante max (delta minimal).
    private EtatVehiculeGroupe choisirVehiculeEntamePourSplit(
            Collection<EtatVehiculeGroupe> etats,
            int besoin,
            Map<Integer, VehiculePlanDTO> vehiculePlans) {

        List<EtatVehiculeGroupe> dispo = etats.stream()
                .filter(e -> e.capaciteRestante > 0 && e.capaciteRestante < besoin)
                .collect(Collectors.toList());

        if (dispo.isEmpty()) {
            return null;
        }

        int meilleurDelta = dispo.stream()
                .mapToInt(e -> besoin - e.capaciteRestante)
                .min()
                .orElse(Integer.MAX_VALUE);

        List<EtatVehiculeGroupe> plusProches = dispo.stream()
                .filter(e -> (besoin - e.capaciteRestante) == meilleurDelta)
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

    // Tie-breaker entre 2 véhicules (utilisé quand delta capacité égal)
    // true => choisir v1, false => choisir v2
    private boolean departagerVehicules(Vehicule v1, Vehicule v2, Map<Integer, VehiculePlanDTO> vehiculePlans) {
        int trajets1 = vehiculePlans.containsKey(v1.getIdVehicule())
                ? vehiculePlans.get(v1.getIdVehicule()).getNombreVoyages()
                : 0;
        int trajets2 = vehiculePlans.containsKey(v2.getIdVehicule())
                ? vehiculePlans.get(v2.getIdVehicule()).getNombreVoyages()
                : 0;

        if (trajets1 != trajets2) {
            return trajets1 < trajets2;
        }

        boolean d1 = "D".equals(v1.getTypeCarburant());
        boolean d2 = "D".equals(v2.getTypeCarburant());
        if (d1 != d2) {
            return d1; // diesel prioritaire
        }

        return random.nextBoolean(); // random si encore égalité
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

    // IMPORTANT: on respecte l'ordre déjà trié en amont (RG7 + éventuel tri hôtel).
    List<Reservation> reservationsTriees = reservations;

    Map<Integer, EtatVehiculeGroupe> etatsGroupe = new LinkedHashMap<>();

    // Restants locaux (pour pouvoir "entamer" une réservation plus bas dans la liste)
    Map<Integer, Integer> restantsParReservation = new LinkedHashMap<>();
    for (Reservation r : reservationsTriees) {
        int restants = assignationRepository.getPassagersRestantsByReservationId(r.getIdReservation());
        restantsParReservation.put(r.getIdReservation(), restants);
    }

    for (int i = 0; i < reservationsTriees.size(); i++) {
        Reservation reservation = reservationsTriees.get(i);
        int reservationId = reservation.getIdReservation();

        int passagersRestants = restantsParReservation.getOrDefault(reservationId, 0);
        int besoin = passagersRestants;
        while (passagersRestants > 0) {
            // Véhicules non encore utilisés dans ce groupe + dispo selon heure retour (Sprint 6)
            List<Vehicule> vehiculesNonUtilises = tousVehicules.stream()
                    .filter(v -> !etatsGroupe.containsKey(v.getIdVehicule()))
                    .filter(v -> estVehiculeDisponible(v, vehiculesHeureRetour, heureVol))
                    .collect(Collectors.toList());

            // 1) Règle Sprint 7: PAS de split si un véhicule unique peut absorber TOUT le reste
            EtatVehiculeGroupe entameCapable = choisirVehiculeEntameCapable(
                    etatsGroupe.values(), passagersRestants, vehiculePlans
            );

            Vehicule nouveauCapable = null;
            
            List<Vehicule> candidatsCapables = vehiculesNonUtilises.stream()
                    .filter(v -> v.getNbrPlaces() >= besoin)
                    .collect(Collectors.toList());
            if (!candidatsCapables.isEmpty()) {
                nouveauCapable = choisirAvecTieBreakers(candidatsCapables, besoin, true, vehiculePlans);
            }

            if (entameCapable != null || nouveauCapable != null) {
                // Choisir le "plus proche" du besoin parmi (entamé vs nouveau)
                boolean prendreEntame = false;
                if (entameCapable != null && nouveauCapable == null) {
                    prendreEntame = true;
                } else if (entameCapable != null) {
                    int deltaEntame = entameCapable.capaciteRestante - passagersRestants;
                    int deltaNouveau = nouveauCapable.getNbrPlaces() - passagersRestants;

                    if (deltaEntame < deltaNouveau) {
                        prendreEntame = true;
                    } else if (deltaEntame == deltaNouveau) {
                        // tie-breakers: nb trajets min, diesel, random
                        prendreEntame = departagerVehicules(entameCapable.vehicule, nouveauCapable, vehiculePlans);
                    }
                }

                if (prendreEntame) {
                    // Assigner en 1 fois (pas de split)
                    entameCapable.ajouterReservation(reservation, passagersRestants);
                    enregistrerAssignation(reservation, entameCapable.vehicule, date, passagersRestants);
                    restantsParReservation.put(reservationId, 0);

                    // NOUVEAU Sprint 7: si le véhicule est encore entamé, on remplit son reste avec une réservation plus bas
                    remplirVehiculeEntameAvecReservationPlusBas(entameCapable, reservationsTriees, i + 1, restantsParReservation, date);

                    passagersRestants = 0;
                    break;
                } else {
                    // Assigner en 1 fois sur un nouveau véhicule (pas de split)
                    Vehicule vehiculeChoisi = nouveauCapable;
                    EtatVehiculeGroupe etat = new EtatVehiculeGroupe(vehiculeChoisi);
                    etatsGroupe.put(vehiculeChoisi.getIdVehicule(), etat);

                    etat.ajouterReservation(reservation, passagersRestants);
                    enregistrerAssignation(reservation, vehiculeChoisi, date, passagersRestants);
                    restantsParReservation.put(reservationId, 0);

                    // NOUVEAU Sprint 7: remplir les places restantes du véhicule entamé
                    remplirVehiculeEntameAvecReservationPlusBas(etat, reservationsTriees, i + 1, restantsParReservation, date);

                    passagersRestants = 0;
                    break;
                }
            }

            // 2) Sinon: split obligatoire (aucun véhicule unique ne peut absorber tout le reste)
            if (vehiculesNonUtilises.isEmpty() && etatsGroupe.values().stream().noneMatch(e -> e.capaciteRestante > 0)) {
                // plus aucun véhicule utilisable dans ce groupe -> on stop, le reste sera repris au groupe suivant (Sprint 5/7)
                break;
            }

            // Choisir le meilleur véhicule "partiel" (le plus proche en dessous => max capacité)
            EtatVehiculeGroupe entamePartiel = choisirVehiculeEntamePourSplit(etatsGroupe.values(), passagersRestants, vehiculePlans);

            Vehicule nouveauPartiel = null;
            if (!vehiculesNonUtilises.isEmpty()) {
                nouveauPartiel = choisirAvecTieBreakers(vehiculesNonUtilises, passagersRestants, false, vehiculePlans);
            }

            boolean prendreEntamePartiel = false;
            if (entamePartiel != null && nouveauPartiel == null) {
                prendreEntamePartiel = true;
            } else if (entamePartiel != null) {
                int deltaEntame = passagersRestants - entamePartiel.capaciteRestante;
                int deltaNouveau = passagersRestants - nouveauPartiel.getNbrPlaces();

                if (deltaEntame < deltaNouveau) {
                    prendreEntamePartiel = true;
                } else if (deltaEntame == deltaNouveau) {
                    prendreEntamePartiel = departagerVehicules(entamePartiel.vehicule, nouveauPartiel, vehiculePlans);
                }
            }

            if (prendreEntamePartiel) {
                int nbAssignes = Math.min(passagersRestants, entamePartiel.capaciteRestante);
                entamePartiel.ajouterReservation(reservation, nbAssignes);
                enregistrerAssignation(reservation, entamePartiel.vehicule, date, nbAssignes);
                passagersRestants -= nbAssignes;
                restantsParReservation.put(reservationId, passagersRestants);
                continue;
            } else {
                Vehicule vehiculeChoisi = nouveauPartiel;
                if (vehiculeChoisi == null) {
                    break;
                }
                EtatVehiculeGroupe etat = new EtatVehiculeGroupe(vehiculeChoisi);
                etatsGroupe.put(vehiculeChoisi.getIdVehicule(), etat);

                int nbAssignes = Math.min(passagersRestants, vehiculeChoisi.getNbrPlaces());
                etat.ajouterReservation(reservation, nbAssignes);
                enregistrerAssignation(reservation, vehiculeChoisi, date, nbAssignes);
                passagersRestants -= nbAssignes;
                restantsParReservation.put(reservationId, passagersRestants);
            }
        }

        // Sprint 7: NE PAS ajouter à result non-assignees ici,
        // car le reste doit être repris au groupe suivant dans le même run.
        // Les non-assignées finales seront recalculées fin de journée dans planifierJour().
    }

    // Finaliser les voyages du groupe (inchangé)
    for (EtatVehiculeGroupe etat : etatsGroupe.values()) {
        List<Reservation> reservationsVoyage = new ArrayList<>(etat.reservationsVoyage);
        LocalDateTime heureDernierVol = reservationsVoyage.stream()
                .map(Reservation::getDateHeureArrivee)
                .max(Comparator.naturalOrder())
                .orElse(heureVol);

        LocalDateTime heureRetourPrecedente = vehiculesHeureRetour.get(etat.vehicule.getIdVehicule());
        LocalDateTime heureDepartVehicule;

        if (heureRetourPrecedente != null && heureRetourPrecedente.isAfter(heureDernierVol)) {
            heureDepartVehicule = heureRetourPrecedente;
        } else {
            heureDepartVehicule = heureDernierVol;
        }

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