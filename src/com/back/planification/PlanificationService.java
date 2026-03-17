package com.app.planification;

import com.app.models.Assignation;
import com.app.models.Hotel;
import com.app.models.Reservation;
import com.app.models.Vehicule;
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
                if (assignationRepository.existsByReservationId(r.getIdReservation())) {
                    continue; // déjà assignée entre-temps
                }

                Assignation a = new Assignation();
                a.setReservationId(r.getIdReservation());
                a.setVehiculeId(vehiculeId);
                a.setDateHeurePlanification(LocalDateTime.now());
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
            
            // Filtrer les réservations déjà assignées
            List<Reservation> reservationsAAssigner = groupe.stream()
                    .filter(r -> !assignationRepository.existsByReservationId(r.getIdReservation()))
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
                                             vehiculesHeureRetour, vehiculePlans, result);
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
     * 6. Clé de la Map = heure du DERNIER vol du groupe (= heure de départ effective)
     *
     * @param reservations Liste des réservations (non triées)
     * @param tempsAttenteMinutes Durée de la tranche en minutes (ex: 30)
     * @return Map où clé = heure de départ (dernier vol du groupe), valeur = liste des réservations
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
                LocalDateTime heureDepart = groupeCourant.get(groupeCourant.size() - 1).getDateHeureArrivee();
                groupes.put(heureDepart, new ArrayList<>(groupeCourant));
                logger.info("Sprint 5 - Groupe fermé : " + groupeCourant.size() + " réservations, départ = " + heureDepart);

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
            LocalDateTime heureDepart = groupeCourant.get(groupeCourant.size() - 1).getDateHeureArrivee();
            groupes.put(heureDepart, groupeCourant);
            logger.info("Sprint 5 - Dernier groupe fermé : " + groupeCourant.size() + " réservations, départ = " + heureDepart);
        }

        return groupes;
    }

    /**
     * Trouve le véhicule optimal selon les règles métier :
     * 1. Capacité >= nbrPersonnes
     * 2. Véhicule jamais utilisé OU déjà revenu à l'aéroport (heureRetour <= heureVolActuel)
     * 3. Capacité la plus proche du besoin
     * 4. Si égalité de capacité → Diesel ('D') prioritaire
     * 5. Sinon → Random
     * 
     * @param nbrPersonnes Nombre de personnes à transporter
     * @param vehiculesDisponibles Liste de tous les véhicules
     * @param vehiculesHeureRetour Map des heures de retour des véhicules
     * @param heureVolActuel Heure du vol actuel
     * @return Véhicule optimal ou null si aucun disponible
     */
    private Vehicule trouverVehiculeOptimal(int nbrPersonnes, 
                                            List<Vehicule> vehiculesDisponibles, 
                                            Map<Integer, LocalDateTime> vehiculesHeureRetour,
                                            LocalDateTime heureVolActuel) {
        
        List<Vehicule> candidats = vehiculesDisponibles.stream()
                .filter(v -> {
                    // Vérifier la capacité
                    if (v.getNbrPlaces() < nbrPersonnes) {
                        return false;
                    }
                    
                    // Vérifier la disponibilité (Sprint 3 - Réutilisation)
                    Integer vehiculeId = v.getIdVehicule();
                    if (!vehiculesHeureRetour.containsKey(vehiculeId)) {
                        // Véhicule jamais utilisé → disponible
                        return true;
                    }
                    
                    // Véhicule déjà utilisé → vérifier s'il est déjà revenu
                    LocalDateTime heureRetour = vehiculesHeureRetour.get(vehiculeId);
                    // boolean estRevenu = heureRetour.isBefore(heureVolActuel) || heureRetour.isEqual(heureVolActuel);
                    boolean estRevenu = false; // mbola atao hoe mbola tsy miverina loa izy fa mbola sprint 6 zany 
                    
                    if (estRevenu) {
                        logger.info("Véhicule " + v.getReference() + " réutilisable (retour: " + 
                                   heureRetour + ", vol actuel: " + heureVolActuel + ")");
                    }
                    
                    return estRevenu;
                })
                .collect(Collectors.toList());
        
        if (candidats.isEmpty()) {
            return null;
        }
        
        // Trier par capacité croissante, puis par type de carburant (Diesel prioritaire)
        candidats.sort((v1, v2) -> {
            int compareCapacite = Integer.compare(v1.getNbrPlaces(), v2.getNbrPlaces());
            if (compareCapacite != 0) {
                return compareCapacite;
            }
            
            boolean v1Diesel = "D".equals(v1.getTypeCarburant());
            boolean v2Diesel = "D".equals(v2.getTypeCarburant());
            
            if (v1Diesel && !v2Diesel) return -1;
            if (!v1Diesel && v2Diesel) return 1;
            
            return 0;
        });
        
        return candidats.get(0);
    }

    /**
     * Enregistre une assignation pour une réservation vers un véhicule.
     */
    private void enregistrerAssignation(Reservation reservation, Vehicule vehicule, LocalDate date) {
        LocalDateTime datePlanification = date.atStartOfDay();
        
        if (assignationRepository.existsByReservationId(reservation.getIdReservation())) {
            logger.warning("Réservation " + reservation.getIdReservation() + " déjà assignée, ignorée.");
            return;
        }
        
        Assignation assignation = new Assignation();
        assignation.setReservationId(reservation.getIdReservation());
        assignation.setVehiculeId(vehicule.getIdVehicule());
        assignation.setDateHeurePlanification(datePlanification);
        
        assignationRepository.save(assignation);
        logger.info("Assignation créée : Réservation " + reservation.getIdReservation() + 
                   " → Véhicule " + vehicule.getReference());
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
            PlanificationResult result) {
        
        List<Reservation> reservationsNonAssignees = new ArrayList<>(reservations);
        
        while (!reservationsNonAssignees.isEmpty()) {
            // Prendre la première réservation (la plus grande)
            Reservation premiereReservation = reservationsNonAssignees.get(0);
            int nbrPersonnes = premiereReservation.getNbrPers();
            
            // Trouver un véhicule pour cette réservation
            Vehicule vehicule = trouverVehiculeOptimal(nbrPersonnes, tousVehicules, vehiculesHeureRetour, heureVol);
            
            if (vehicule == null) {
                // Aucun véhicule disponible pour cette réservation
                logger.warning("Aucun véhicule disponible pour la réservation " + 
                              premiereReservation.getIdReservation() + " (" + nbrPersonnes + " personnes)");
                result.addReservationNonAssignee(premiereReservation);
                reservationsNonAssignees.remove(0);
                continue;
            }
            
            // RG8: Remplir le véhicule avec d'autres réservations du même vol
            List<Reservation> reservationsDuVehicule = new ArrayList<>();
            reservationsDuVehicule.add(premiereReservation);
            int capaciteRestante = vehicule.getNbrPlaces() - nbrPersonnes;
            
            logger.info("Véhicule " + vehicule.getReference() + " sélectionné (capacité: " + 
                       vehicule.getNbrPlaces() + "), capacité restante: " + capaciteRestante);
            
            // Essayer d'ajouter d'autres réservations tant que la capacité le permet
            Iterator<Reservation> iterator = reservationsNonAssignees.listIterator(1);
            while (iterator.hasNext() && capaciteRestante > 0) {
                Reservation autreReservation = iterator.next();
                if (autreReservation.getNbrPers() <= capaciteRestante) {
                    reservationsDuVehicule.add(autreReservation);
                    capaciteRestante -= autreReservation.getNbrPers();
                    iterator.remove();
                    logger.info("  + Ajout réservation " + autreReservation.getIdReservation() + 
                               " (" + autreReservation.getNbrPers() + " pers), capacité restante: " + capaciteRestante);
                }
            }
            
            // Retirer la première réservation de la liste
            reservationsNonAssignees.remove(premiereReservation);

            // Enregistrer les assignations
            for (Reservation reservation : reservationsDuVehicule) {
                enregistrerAssignation(reservation, vehicule, date);
            }

            // Sprint 5 : Calculer l'heure de départ spécifique à ce véhicule
            // = heure d'arrivée du DERNIER vol des réservations assignées à ce véhicule
            LocalDateTime heureDepartVehicule = reservationsDuVehicule.stream()
                    .map(Reservation::getDateHeureArrivee)
                    .max(Comparator.naturalOrder())
                    .orElse(heureVol); // fallback sur heureVol si liste vide (ne devrait pas arriver)

            logger.info("Sprint 5 - Véhicule " + vehicule.getReference() +
                       " : heure départ = " + heureDepartVehicule +
                       " (dernier vol des " + reservationsDuVehicule.size() + " réservations)");

            // Calculer le trajet complet avec l'heure de départ spécifique au véhicule
            TrajetComplet trajetComplet = trajetCalculator.calculerTrajetComplet(heureDepartVehicule, reservationsDuVehicule);

            // Stocker l'heure de retour du véhicule
            vehiculesHeureRetour.put(vehicule.getIdVehicule(), trajetComplet.getHeureRetour());

            // Ajouter au résultat
            int vehiculeId = vehicule.getIdVehicule();
            VehiculePlanDTO vehiculePlan;
            if (!vehiculePlans.containsKey(vehiculeId)) {
                vehiculePlan = new VehiculePlanDTO(vehicule, new ArrayList<>());
                vehiculePlans.put(vehiculeId, vehiculePlan);
            } else {
                vehiculePlan = vehiculePlans.get(vehiculeId);
            }

            // Créer un nouveau voyage avec l'heure de départ spécifique au véhicule
            int numeroVoyage = vehiculePlan.getNombreVoyages() + 1;
            VoyageDTO voyage = new VoyageDTO(
                numeroVoyage,
                heureDepartVehicule,
                trajetComplet.getHeureRetour(),
                trajetComplet.getDistanceTotale(),
                new ArrayList<>(reservationsDuVehicule),
                trajetComplet.getDetailsTrajet()
            );
            
            vehiculePlan.addVoyage(voyage);
            vehiculePlan.getReservations().addAll(reservationsDuVehicule);
            
            logger.info("Voyage " + numeroVoyage + " créé avec " + reservationsDuVehicule.size() + 
                       " réservations (" + (vehicule.getNbrPlaces() - capaciteRestante) + " personnes)");
        }
    }
}