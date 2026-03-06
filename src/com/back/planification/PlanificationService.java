package com.app.planification;

import com.app.models.Assignation;
import com.app.models.Reservation;
import com.app.models.Vehicule;
import com.app.repository.AssignationRepository;
import com.app.repository.ReservationRepository;
import com.app.repository.VehiculeRepository;

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
    }

    /**
     * Constructeur avec injection des repositories (pour tests)
     */
    public PlanificationService(ReservationRepository reservationRepo, 
                                 VehiculeRepository vehiculeRepo,
                                 AssignationRepository assignationRepo) {
        this.reservationRepository = reservationRepo;
        this.vehiculeRepository = vehiculeRepo;
        this.assignationRepository = assignationRepo;
    }

    /**
     * Méthode principale : Planifie les réservations pour une date donnée.
     * 
     * Règle métier :
     * - Les réservations sont groupées par vol (même date_heure_arrivee)
     * - On essaie d'abord d'assigner tout le groupe à UN SEUL véhicule
     * - Si impossible (capacité insuffisante), on traite individuellement (nbr_pers DESC)
     * 
     * @param date La date de planification
     * @return PlanificationResult contenant les véhicules assignés et les réservations non assignées
     */
    public PlanificationResult planifierJour(LocalDate date) {
        logger.info("Début de la planification pour la date : " + date);
        
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
        
        // Étape 3 : Récupérer les véhicules déjà utilisés ce jour
        Set<Integer> vehiculesUtilises = new HashSet<>(assignationRepository.findVehiculeIdsByDate(date));
        logger.info("Véhicules déjà utilisés ce jour : " + vehiculesUtilises.size());
        
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
            
            // ESSAI 1 : Trouver UN véhicule pour TOUT le groupe
            Vehicule vehiculeGroupe = trouverVehiculeOptimal(totalPersonnesGroupe, tousVehicules, vehiculesUtilises);
            
            if (vehiculeGroupe != null) {
                // Succès : Assigner tout le groupe au même véhicule
                logger.info("Véhicule assigné pour tout le groupe : " + vehiculeGroupe.getReference() + 
                           " (capacité: " + vehiculeGroupe.getNbrPlaces() + ")");
                
                // Enregistrer les assignations pour tout le groupe
                for (Reservation reservation : reservationsAAssigner) {
                    enregistrerAssignation(reservation, vehiculeGroupe, date);
                }
                
                // Marquer le véhicule comme utilisé
                vehiculesUtilises.add(vehiculeGroupe.getIdVehicule());
                
                // Ajouter au résultat
                int vehiculeId = vehiculeGroupe.getIdVehicule();
                if (!vehiculePlans.containsKey(vehiculeId)) {
                    vehiculePlans.put(vehiculeId, new VehiculePlanDTO(vehiculeGroupe, new ArrayList<>()));
                }
                vehiculePlans.get(vehiculeId).getReservations().addAll(reservationsAAssigner);
                
            } else {
                // ESSAI 2 : Pas de véhicule assez grand → Traiter individuellement
                logger.info("Aucun véhicule pour le groupe entier (" + totalPersonnesGroupe + 
                           " pers). Traitement individuel...");
                
                // Trier par nbr_pers DESC pour prioriser les grands groupes
                List<Reservation> reservationsTriees = reservationsAAssigner.stream()
                        .sorted(Comparator.comparingInt(Reservation::getNbrPers).reversed())
                        .collect(Collectors.toList());
                
                for (Reservation reservation : reservationsTriees) {
                    int nbrPersonnes = reservation.getNbrPers();
                    
                    Vehicule vehiculeIndiv = trouverVehiculeOptimal(nbrPersonnes, tousVehicules, vehiculesUtilises);
                    
                    if (vehiculeIndiv != null) {
                        logger.info("Véhicule assigné individuellement : " + vehiculeIndiv.getReference() + 
                                   " pour réservation " + reservation.getIdReservation());
                        
                        enregistrerAssignation(reservation, vehiculeIndiv, date);
                        vehiculesUtilises.add(vehiculeIndiv.getIdVehicule());
                        
                        int vehiculeId = vehiculeIndiv.getIdVehicule();
                        if (!vehiculePlans.containsKey(vehiculeId)) {
                            vehiculePlans.put(vehiculeId, new VehiculePlanDTO(vehiculeIndiv, new ArrayList<>()));
                        }
                        vehiculePlans.get(vehiculeId).getReservations().add(reservation);
                        
                    } else {
                        logger.warning("Aucun véhicule disponible pour la réservation " + 
                                      reservation.getIdReservation() + " (" + nbrPersonnes + " personnes)");
                        result.addReservationNonAssignee(reservation);
                    }
                }
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
     * Groupe les réservations par vol (même date_heure_arrivee).
     */
    private Map<LocalDateTime, List<Reservation>> grouperParVol(List<Reservation> reservations) {
        return reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getDateHeureArrivee));
    }

    /**
     * Trouve le véhicule optimal selon les règles métier :
     * 1. Capacité >= nbrPersonnes
     * 2. Capacité la plus proche du besoin
     * 3. Si égalité de capacité → Diesel ('D') prioritaire
     * 4. Sinon → Random
     */
    private Vehicule trouverVehiculeOptimal(int nbrPersonnes, 
                                            List<Vehicule> vehiculesDisponibles, 
                                            Set<Integer> vehiculesUtilises) {
        
        List<Vehicule> candidats = vehiculesDisponibles.stream()
                .filter(v -> !vehiculesUtilises.contains(v.getIdVehicule()))
                .filter(v -> v.getNbrPlaces() >= nbrPersonnes)
                .collect(Collectors.toList());
        
        if (candidats.isEmpty()) {
            return null;
        }
        
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
}