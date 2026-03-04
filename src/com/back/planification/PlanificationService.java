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
     * @param date La date de planification
     * @return PlanificationResult contenant les véhicules assignés et les réservations non assignées
     */
    public PlanificationResult planifierJour(LocalDate date) {
        logger.info("Début de la planification pour la date : " + date);
        
        PlanificationResult result = new PlanificationResult();
        
        // Étape 1 : Récupérer les réservations du jour (triées par date_heure_arrivee ASC, nbr_pers DESC)
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
        
        // Étape 5 : Trier les groupes par totalPersonnes DESC (priorité aux grands groupes)
        List<Map.Entry<LocalDateTime, List<Reservation>>> groupesTries = groupesParVol.entrySet()
                .stream()
                .sorted((e1, e2) -> {
                    int total1 = e1.getValue().stream().mapToInt(Reservation::getNbrPers).sum();
                    int total2 = e2.getValue().stream().mapToInt(Reservation::getNbrPers).sum();
                    return Integer.compare(total2, total1); // DESC
                })
                .collect(Collectors.toList());
        
        // Étape 6 : Pour chaque groupe, trouver et assigner un véhicule
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
            int totalPersonnes = reservationsAAssigner.stream()
                    .mapToInt(Reservation::getNbrPers)
                    .sum();
            
            logger.info("Groupe vol " + heureVol + " : " + reservationsAAssigner.size() + 
                       " réservations, " + totalPersonnes + " personnes");
            
            // Trouver le véhicule optimal
            Vehicule vehiculeOptimal = trouverVehiculeOptimal(totalPersonnes, tousVehicules, vehiculesUtilises);
            
            if (vehiculeOptimal != null) {
                logger.info("Véhicule assigné : " + vehiculeOptimal.getReference() + 
                           " (capacité: " + vehiculeOptimal.getNbrPlaces() + ")");
                
                // Enregistrer les assignations
                enregistrerAssignation(reservationsAAssigner, vehiculeOptimal, date);
                
                // Marquer le véhicule comme utilisé
                vehiculesUtilises.add(vehiculeOptimal.getIdVehicule());
                
                // Ajouter au résultat
                VehiculePlanDTO vehiculePlan = new VehiculePlanDTO(vehiculeOptimal, reservationsAAssigner);
                result.addVehiculePlan(vehiculePlan);
                
            } else {
                logger.warning("Aucun véhicule disponible pour le groupe " + heureVol + 
                              " (" + totalPersonnes + " personnes)");
                // Ajouter aux réservations non assignées
                result.addAllReservationsNonAssignees(reservationsAAssigner);
            }
        }
        
        logger.info("Planification terminée. Véhicules assignés: " + result.getNombreVehiculesUtilises() + 
                   ", Réservations non assignées: " + result.getNombreReservationsNonAssignees());
        
        return result;
    }

    /**
     * Groupe les réservations par vol (même date_heure_arrivee).
     * 
     * @param reservations Liste des réservations à grouper
     * @return Map avec clé = date_heure_arrivee, valeur = liste des réservations
     */
    private Map<LocalDateTime, List<Reservation>> grouperParVol(List<Reservation> reservations) {
        return reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getDateHeureArrivee));
    }

    /**
     * Trouve le véhicule optimal selon les règles métier :
     * 1. Capacité >= totalPersonnes
     * 2. Capacité la plus proche du besoin
     * 3. Si égalité de capacité → Diesel ('D') prioritaire
     * 4. Sinon → Random
     * 
     * @param totalPersonnes Nombre total de personnes à transporter
     * @param vehiculesDisponibles Liste de tous les véhicules
     * @param vehiculesUtilises Set des IDs de véhicules déjà utilisés ce jour
     * @return Le véhicule optimal ou null si aucun disponible
     */
    private Vehicule trouverVehiculeOptimal(int totalPersonnes, 
                                            List<Vehicule> vehiculesDisponibles, 
                                            Set<Integer> vehiculesUtilises) {
        
        // Filtrer les véhicules : non utilisés ET capacité suffisante
        List<Vehicule> candidats = vehiculesDisponibles.stream()
                .filter(v -> !vehiculesUtilises.contains(v.getIdVehicule()))
                .filter(v -> v.getNbrPlaces() >= totalPersonnes)
                .collect(Collectors.toList());
        
        if (candidats.isEmpty()) {
            return null;
        }
        
        // Trier par capacité ASC (plus proche du besoin), puis Diesel prioritaire
        candidats.sort((v1, v2) -> {
            // D'abord par capacité (ASC - plus proche du besoin)
            int compareCapacite = Integer.compare(v1.getNbrPlaces(), v2.getNbrPlaces());
            if (compareCapacite != 0) {
                return compareCapacite;
            }
            
            // Si même capacité, Diesel ('D') prioritaire
            boolean v1Diesel = "D".equals(v1.getTypeCarburant());
            boolean v2Diesel = "D".equals(v2.getTypeCarburant());
            
            if (v1Diesel && !v2Diesel) {
                return -1; // v1 prioritaire
            } else if (!v1Diesel && v2Diesel) {
                return 1;  // v2 prioritaire
            }
            
            // Si toujours égalité, random (on prend le premier)
            return 0;
        });
        
        // Retourner le meilleur candidat
        return candidats.get(0);
    }

    /**
     * Enregistre les assignations pour un groupe de réservations vers un véhicule.
     * 
     * @param groupe Liste des réservations à assigner
     * @param vehicule Véhicule assigné
     * @param date Date de planification
     */
    private void enregistrerAssignation(List<Reservation> groupe, Vehicule vehicule, LocalDate date) {
        LocalDateTime datePlanification = date.atStartOfDay();
        
        for (Reservation reservation : groupe) {
            // Vérification supplémentaire (double-check)
            if (assignationRepository.existsByReservationId(reservation.getIdReservation())) {
                logger.warning("Réservation " + reservation.getIdReservation() + " déjà assignée, ignorée.");
                continue;
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
}