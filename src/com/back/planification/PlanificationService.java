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
        
        // Initialiser TrajetCalculator
        DistanceRepository distanceRepo = new DistanceRepository(DB_URL, DB_USER, DB_PASSWORD);
        ParametreRepository parametreRepo = new ParametreRepository(DB_URL, DB_USER, DB_PASSWORD);
        this.hotelRepository = new HotelRepository(DB_URL, DB_USER, DB_PASSWORD);
        this.trajetCalculator = new TrajetCalculator(distanceRepo, parametreRepo, hotelRepository);
    }

    /**
     * Constructeur avec injection des repositories (pour tests)
     */
    public PlanificationService(ReservationRepository reservationRepo, 
                                 VehiculeRepository vehiculeRepo,
                                 AssignationRepository assignationRepo,
                                 TrajetCalculator trajetCalc,
                                 HotelRepository hotelRepo) {
        this.reservationRepository = reservationRepo;
        this.vehiculeRepository = vehiculeRepo;
        this.assignationRepository = assignationRepo;
        this.trajetCalculator = trajetCalc;
        this.hotelRepository = hotelRepo;
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
        logger.info("Réutilisation des véhicules activée (Sprint 3)");
        
        // Étape 4 : Grouper les réservations par vol (même date_heure_arrivee)
        Map<LocalDateTime, List<Reservation>> groupesParVol = grouperParVol(reservations);
        logger.info("Nombre de groupes (vols) : " + groupesParVol.size());
        
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

            
            // RG8 CORRIGÉ: Assigne les réservations d'un même vol.
            // STRATÉGIE :
            // 1. Trier par nombre de personnes décroissant
            // 2. Pour chaque réservation :
            //    - D'abord vérifier si un véhicule DÉJÀ ASSIGNÉ pour ce créneau a assez de places
            //    - Sinon chercher un nouveau véhicule optimal
            // 3. Seules les réservations de même heure d'arrivée peuvent partager un véhicule
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
     * Groupe les réservations par vol (même date_heure_arrivee).
     */
    private Map<LocalDateTime, List<Reservation>> grouperParVol(List<Reservation> reservations) {
        return reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getDateHeureArrivee));
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
                    if (vehiculesHeureRetour.containsKey(vehiculeId)) {
                        // Véhicule déjà utilisé pour un autre créneau → NON disponible
                        // Règle: seules les réservations de même heure d'arrivée peuvent partager un véhicule
                        return false;
                    }
                    
                    // Véhicule jamais utilisé → disponible
                    return true;
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
     * RG8 CORRIGÉ: Assigne les réservations d'un même vol.
     * 
     * STRATÉGIE :
     * 1. Trier par nombre de personnes décroissant
     * 2. Pour chaque réservation :
     *    - D'abord vérifier si un véhicule DÉJÀ ASSIGNÉ pour ce créneau a assez de places
     *    - Sinon chercher un nouveau véhicule optimal
     * 3. Seules les réservations de même heure d'arrivée peuvent partager un véhicule
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
        
        // Map pour suivre les places RESTANTES des véhicules utilisés pour CE créneau
        // Clé: idVehicule, Valeur: places restantes
        Map<Integer, Integer> vehiculesPlacesRestantes = new LinkedHashMap<>();
        
        // Map pour suivre les réservations assignées à chaque véhicule pour CE créneau
        Map<Integer, List<Reservation>> vehiculesReservations = new LinkedHashMap<>();
        
        // Map pour accéder aux objets Vehicule par ID
        Map<Integer, Vehicule> vehiculesById = tousVehicules.stream()
                .collect(Collectors.toMap(Vehicule::getIdVehicule, v -> v));
        
        logger.info("=== Assignation pour créneau " + heureVol + " ===");
        logger.info("Réservations à traiter (triées par nbr décroissant) : " + 
               reservations.stream()
                   .map(r -> r.getClientId() + "(" + r.getNbrPers() + ")")
                   .collect(Collectors.joining(", ")));
        
        // Parcourir les réservations (déjà triées par nbr décroissant)
        for (Reservation reservation : reservations) {
            int nbrPersonnes = reservation.getNbrPers();
            Vehicule vehiculeChoisi = null;
            
            // PRIORITÉ 1: Chercher parmi les véhicules DÉJÀ assignés pour ce créneau
            for (Map.Entry<Integer, Integer> entry : vehiculesPlacesRestantes.entrySet()) {
                int vehiculeId = entry.getKey();
                int placesRestantes = entry.getValue();
                
                if (placesRestantes >= nbrPersonnes) {
                    vehiculeChoisi = vehiculesById.get(vehiculeId);
                    logger.info("✅ Réservation " + reservation.getClientId() + " (" + nbrPersonnes + " pers) → " +
                               "Véhicule existant " + vehiculeChoisi.getReference() + 
                               " (places restantes: " + placesRestantes + " >= " + nbrPersonnes + ")");
                    break;
                }
            }
            
            // PRIORITÉ 2: Si aucun véhicule existant n'a assez de place, chercher un nouveau
            if (vehiculeChoisi == null) {
                vehiculeChoisi = trouverVehiculeOptimal(nbrPersonnes, tousVehicules, 
                                                       vehiculesHeureRetour, heureVol);
                
                if (vehiculeChoisi != null) {
                    // Initialiser le tracking pour ce nouveau véhicule
                    vehiculesPlacesRestantes.put(vehiculeChoisi.getIdVehicule(), 
                                                    vehiculeChoisi.getNbrPlaces());
                    vehiculesReservations.put(vehiculeChoisi.getIdVehicule(), new ArrayList<>());
                    
                    logger.info("✅ Réservation " + reservation.getClientId() + " (" + nbrPersonnes + " pers) → " +
                               "Nouveau véhicule " + vehiculeChoisi.getReference() + 
                               " (capacité: " + vehiculeChoisi.getNbrPlaces() + ")");
                }
            }
            
            if (vehiculeChoisi == null) {
                logger.warning("❌ Aucun véhicule disponible pour la réservation " + 
                          reservation.getIdReservation() + " (" + nbrPersonnes + " personnes)");
                result.addReservationNonAssignee(reservation);
                continue;
            }
            
            // Assigner la réservation au véhicule
            enregistrerAssignation(reservation, vehiculeChoisi, date);
            
            // Mettre à jour le tracking des places restantes
            int placesRestantes = vehiculesPlacesRestantes.get(vehiculeChoisi.getIdVehicule());
            int nouvellesPlacesRestantes = placesRestantes - nbrPersonnes;
            vehiculesPlacesRestantes.put(vehiculeChoisi.getIdVehicule(), nouvellesPlacesRestantes);
            vehiculesReservations.get(vehiculeChoisi.getIdVehicule()).add(reservation);
            
            logger.info("   → Places restantes après assignation: " + nouvellesPlacesRestantes);
        }
        
        // Créer les VoyageDTO pour chaque véhicule utilisé dans ce créneau
        for (Map.Entry<Integer, List<Reservation>> entry : vehiculesReservations.entrySet()) {
            int vehiculeId = entry.getKey();
            List<Reservation> reservationsDuVehicule = entry.getValue();
            
            if (!reservationsDuVehicule.isEmpty()) {
                Vehicule vehicule = vehiculesById.get(vehiculeId);
                
                // Calculer le trajet complet pour ce véhicule
                TrajetComplet trajetComplet = trajetCalculator.calculerTrajetComplet(heureVol, reservationsDuVehicule);
                
                // Stocker l'heure de retour pour la réutilisation du véhicule
                vehiculesHeureRetour.put(vehiculeId, trajetComplet.getHeureRetour());
                
                // Ajouter au résultat
                ajouterVoyageAuPlan(vehicule, heureVol, trajetComplet, reservationsDuVehicule, vehiculePlans);
                
                int totalPersonnes = reservationsDuVehicule.stream().mapToInt(Reservation::getNbrPers).sum();
                logger.info("📊 Véhicule " + vehicule.getReference() + " : " + 
                           reservationsDuVehicule.size() + " réservations, " + 
                           totalPersonnes + " personnes au total");
            }
        }
    }

    /**
     * Méthode utilitaire pour ajouter un voyage à un plan de véhicule
     */
    private void ajouterVoyageAuPlan(Vehicule vehicule, 
                                 LocalDateTime heureVol,
                                 TrajetComplet trajetComplet,
                                 List<Reservation> reservations,
                                 Map<Integer, VehiculePlanDTO> vehiculePlans) {
        int vehiculeId = vehicule.getIdVehicule();
        VehiculePlanDTO vehiculePlan;
        
        if (!vehiculePlans.containsKey(vehiculeId)) {
            vehiculePlan = new VehiculePlanDTO(vehicule, new ArrayList<>());
            vehiculePlans.put(vehiculeId, vehiculePlan);
        } else {
            vehiculePlan = vehiculePlans.get(vehiculeId);
        }
        
        int numeroVoyage = vehiculePlan.getNombreVoyages() + 1;
        VoyageDTO voyage = new VoyageDTO(
            numeroVoyage,
            heureVol,
            trajetComplet.getHeureRetour(),
            trajetComplet.getDistanceTotale(),
            new ArrayList<>(reservations),
            trajetComplet.getDetailsTrajet()
        );
        
        vehiculePlan.addVoyage(voyage);
    }
}