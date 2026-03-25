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
        
        // Étape 6 : Pour chaque groupe (tranche), traiter Sprint 8 d'abord, puis traiter le groupe
        for (Map.Entry<LocalDateTime, List<Reservation>> entry : groupesTries) {
            LocalDateTime heureVol = entry.getKey();          // = fin de tranche (Sprint 5)
            List<Reservation> groupe = entry.getValue();

            // Début réel du groupe = première arrivée dans la tranche
            LocalDateTime debutIntervalle = groupe.stream()
                    .map(Reservation::getDateHeureArrivee)
                    .min(Comparator.naturalOrder())
                    .orElse(heureVol);

            // Sprint 8 - Tâche 2 prise en compte en premier :
            // traiter tous les véhicules redevenus dispo AVANT le début de ce groupe
            traiterVehiculesRedisponiblesAvantProchainGroupe(
                    date,
                    debutIntervalle,
                    tempsAttente,
                    tousVehicules,
                    vehiculesHeureRetour,
                    vehiculePlans,
                    result
            );

            // Puis traiter normalement le groupe courant (Sprint 5 + Sprint 7 + Sprint 3)
            assignerAvecRemplissageProgressif(
                    groupe,
                    heureVol,
                    date,
                    tousVehicules,
                    vehiculesHeureRetour,
                    vehiculePlans,
                    result,
                    tempsAttente
            );
        }
        
        // Sprint 8 - fin de journée : re-tenter sur les véhicules redisponibles
        // (jusqu'à minuit) pour éviter de laisser des non-assignées alors qu'un véhicule revient plus tard.
        traiterVehiculesRedisponiblesAvantProchainGroupe(
                date,
                date.plusDays(1).atStartOfDay(),
                tempsAttente,
                tousVehicules,
                vehiculesHeureRetour,
                vehiculePlans,
                result
        );

        result.setVehiculesAssignes(new ArrayList<>(vehiculePlans.values()));
        
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
     * Sprint 8 - Tâche 2
     * Tant qu’il existe un véhicule dont l’heureRetour <= prochainDepart :
     * - déclenche un mini-cycle d’assignation à t = heureRetour
     * - priorise les non-assignées (<= t), puis complète dans [t ; t+tempsAttente]
     */
    private void traiterVehiculesRedisponiblesAvantProchainGroupe(
            LocalDate date,
            LocalDateTime prochainDepart,
            int tempsAttente,
            List<Vehicule> tousVehicules,
            Map<Integer, LocalDateTime> vehiculesHeureRetour,
            Map<Integer, VehiculePlanDTO> vehiculePlans,
            PlanificationResult result) {

        if (prochainDepart == null || tousVehicules == null || tousVehicules.isEmpty()
                || vehiculesHeureRetour == null || vehiculesHeureRetour.isEmpty()) {
            return;
        }

        Map<Integer, Vehicule> vehiculeById = tousVehicules.stream()
                .collect(Collectors.toMap(Vehicule::getIdVehicule, v -> v, (a, b) -> a));

        // Anti-boucle infinie : si à une heureDispo donnée on ne charge rien, on ne retente pas.
        Set<String> tentativesSansChargement = new HashSet<>();

        int gardeFou = 0;
        while (true) {
            if (gardeFou++ > 10_000) {
                logger.warning("Sprint 8 - garde-fou atteint (boucle véhicules redisponibles).");
                return;
            }

            Map.Entry<Integer, LocalDateTime> prochainVehicule = vehiculesHeureRetour.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .filter(e -> !e.getValue().isAfter(prochainDepart)) // <= prochainDepart
                    .filter(e -> !tentativesSansChargement.contains(e.getKey() + "|" + e.getValue()))
                    .min(Comparator.comparing(Map.Entry::getValue))
                    .orElse(null);

            if (prochainVehicule == null) {
                return;
            }

            int vehiculeId = prochainVehicule.getKey();
            LocalDateTime heureDispo = prochainVehicule.getValue();
            String cleTentative = vehiculeId + "|" + heureDispo;

            Vehicule vehicule = vehiculeById.get(vehiculeId);
            if (vehicule == null) {
                tentativesSansChargement.add(cleTentative);
                continue;
            }

            boolean charge = assignerQuandVehiculeDisponible(
                    date,
                    heureDispo,
                    vehicule,
                    tempsAttente,
                    vehiculesHeureRetour,
                    vehiculePlans,
                    result
            );

            if (!charge) {
                tentativesSansChargement.add(cleTentative);
            }
            // Si charge=true, vehiculesHeureRetour a été mis à jour (nouvelle heureRetour).
            // On laisse la boucle continuer : si le véhicule revient encore avant prochainDepart,
            // il pourra être retraité.
        }
    }

    /**
     * Sprint 8 - Tâche 2
     * Mini-cycle sur UN véhicule à l’instant heureDispo.
     * Priorité :
     * 1) non assignées (passagers restants) avec arrivée <= heureDispo
     * 2) puis complétion avec arrivées dans [heureDispo ; heureDispo+tempsAttente] si capacité restante
     *
     * @return true si au moins 1 passager a été chargé
     */
    private boolean assignerQuandVehiculeDisponible(
            LocalDate date,
            LocalDateTime heureDispo,
            Vehicule vehicule,
            int tempsAttente,
            Map<Integer, LocalDateTime> vehiculesHeureRetour,
            Map<Integer, VehiculePlanDTO> vehiculePlans,
            PlanificationResult result) {

        if (date == null || heureDispo == null || vehicule == null) {
            return false;
        }

        int capacite = vehicule.getNbrPlaces();
        if (capacite <= 0) {
            return false;
        }

        LocalDateTime finFenetre = heureDispo.plusMinutes(Math.max(0, tempsAttente));

        // RG-S8-2 (1) : non assignées <= heureDispo
        List<Reservation> priorite = reservationRepository.findUnassignedByDateAndArrivalBefore(date, heureDispo);
        List<Reservation> prioriteRestantes = new ArrayList<>(priorite);

        // RG-S8-2 (2) : complétion dans [heureDispo ; heureDispo+tempsAttente]
        List<Reservation> fenetre = reservationRepository.findUnassignedByDateAndArrivalBetween(date, heureDispo, finFenetre);

        // Dédup (car "between" inclut heureDispo, et une réservation à heureDispo peut être dans les 2 listes)
        Set<Integer> idsPriorite = priorite.stream()
                .map(Reservation::getIdReservation)
                .collect(Collectors.toSet());

        List<Reservation> fenetreRestantes = fenetre.stream()
                .filter(r -> !idsPriorite.contains(r.getIdReservation()))
                .collect(Collectors.toCollection(ArrayList::new));

        EtatVehiculeGroupe etat = new EtatVehiculeGroupe(vehicule);

        boolean hasFutureLoaded = false;
        LocalDateTime maxArriveeChargee = null;

        // Tant qu'on a de la place, on consomme d’abord la priorité, puis la fenêtre.
        // Note: on évite de considérer le véhicule "entamé" AVANT la 1ère assignation,
        // pour respecter le tri décroissant (Cas 3 Sprint8.txt).
        boolean vehiculeEntame = false;

        // Phase 1 : priorité (<= heureDispo)
        while (etat.capaciteRestante > 0 && !prioriteRestantes.isEmpty()) {
            Reservation prochaine = choisirProchaineReservationPourSplit(
                    prioriteRestantes,
                    vehiculeEntame ? Collections.singletonList(etat) : Collections.emptyList()
            );
            if (prochaine == null) {
                break;
            }

            prioriteRestantes.removeIf(r -> r.getIdReservation() == prochaine.getIdReservation());

            int restantsAvant = assignationRepository.getPassagersRestantsByReservationId(prochaine.getIdReservation());
            if (restantsAvant <= 0) {
                mettreAJourReservationNonAssignee(result, prochaine, 0);
                continue;
            }

            int nbAssignes = Math.min(restantsAvant, etat.capaciteRestante);
            if (nbAssignes <= 0) {
                break;
            }

            etat.ajouterReservation(prochaine, nbAssignes);
            enregistrerAssignation(prochaine, vehicule, date, nbAssignes);
            vehiculeEntame = true;

            int restantsApres = assignationRepository.getPassagersRestantsByReservationId(prochaine.getIdReservation());
            mettreAJourReservationNonAssignee(result, prochaine, restantsApres);

            LocalDateTime arrivee = prochaine.getDateHeureArrivee();
            if (arrivee != null) {
                if (maxArriveeChargee == null || arrivee.isAfter(maxArriveeChargee)) {
                    maxArriveeChargee = arrivee;
                }
                if (arrivee.isAfter(heureDispo)) {
                    hasFutureLoaded = true;
                }
            }
        }

        // Phase 2 : fenêtre [heureDispo ; heureDispo+tempsAttente] (si reste de la place)
        while (etat.capaciteRestante > 0 && !fenetreRestantes.isEmpty()) {
            Reservation prochaine = choisirProchaineReservationPourSplit(
                    fenetreRestantes,
                    vehiculeEntame ? Collections.singletonList(etat) : Collections.emptyList()
            );
            if (prochaine == null) {
                break;
            }

            fenetreRestantes.removeIf(r -> r.getIdReservation() == prochaine.getIdReservation());

            int restantsAvant = assignationRepository.getPassagersRestantsByReservationId(prochaine.getIdReservation());
            if (restantsAvant <= 0) {
                mettreAJourReservationNonAssignee(result, prochaine, 0);
                continue;
            }

            int nbAssignes = Math.min(restantsAvant, etat.capaciteRestante);
            if (nbAssignes <= 0) {
                break;
            }

            etat.ajouterReservation(prochaine, nbAssignes);
            enregistrerAssignation(prochaine, vehicule, date, nbAssignes);
            vehiculeEntame = true;

            int restantsApres = assignationRepository.getPassagersRestantsByReservationId(prochaine.getIdReservation());
            mettreAJourReservationNonAssignee(result, prochaine, restantsApres);

            LocalDateTime arrivee = prochaine.getDateHeureArrivee();
            if (arrivee != null) {
                if (maxArriveeChargee == null || arrivee.isAfter(maxArriveeChargee)) {
                    maxArriveeChargee = arrivee;
                }
                if (arrivee.isAfter(heureDispo)) {
                    hasFutureLoaded = true;
                }
            }
        }

        int totalCharges = vehicule.getNbrPlaces() - etat.capaciteRestante;
        if (totalCharges <= 0 || etat.reservationsVoyage.isEmpty()) {
            return false;
        }

        // RG-S8-4 : heure de départ
        LocalDateTime heureDepartVehicule;
        if (hasFutureLoaded && maxArriveeChargee != null && maxArriveeChargee.isAfter(heureDispo)) {
            heureDepartVehicule = maxArriveeChargee;
            if (heureDepartVehicule.isAfter(finFenetre)) {
                heureDepartVehicule = finFenetre; // garde-fou (normalement inutile)
            }
        } else {
            heureDepartVehicule = heureDispo;
        }

        // Trajet + MAJ heure retour (Sprint 3 réutilisation)
        List<Reservation> reservationsVoyage = new ArrayList<>(etat.reservationsVoyage);
        TrajetComplet trajetComplet = trajetCalculator.calculerTrajetComplet(heureDepartVehicule, reservationsVoyage);
        if (trajetComplet != null) {
            vehiculesHeureRetour.put(vehicule.getIdVehicule(), trajetComplet.getHeureRetour());
        } else {
            vehiculesHeureRetour.put(vehicule.getIdVehicule(), heureDepartVehicule);
        }

        // Alimenter le résultat (VehiculePlanDTO + VoyageDTO) comme dans assignerAvecRemplissageProgressif()
        VehiculePlanDTO vehiculePlan;
        if (!vehiculePlans.containsKey(vehicule.getIdVehicule())) {
            vehiculePlan = new VehiculePlanDTO(vehicule, new ArrayList<>());
            vehiculePlans.put(vehicule.getIdVehicule(), vehiculePlan);
        } else {
            vehiculePlan = vehiculePlans.get(vehicule.getIdVehicule());
        }

        int numeroVoyage = vehiculePlan.getNombreVoyages() + 1;
        VoyageDTO voyage = new VoyageDTO(
                numeroVoyage,
                heureDepartVehicule,
                trajetComplet != null ? trajetComplet.getHeureRetour() : heureDepartVehicule,
                trajetComplet != null ? trajetComplet.getDistanceTotale() : 0,
                reservationsVoyage,
                trajetComplet != null ? trajetComplet.getDetailsTrajet() : new ArrayList<>()
        );

        voyage.setPassagersAssignesParReservation(new LinkedHashMap<>(etat.passagersAssignesParReservation));
        vehiculePlan.addVoyage(voyage);
        vehiculePlan.getReservations().addAll(reservationsVoyage);

        logger.info("Sprint 8 - Véhicule " + vehicule.getIdVehicule()
                + " redispo à " + heureDispo
                + " -> départ " + heureDepartVehicule
                + " (" + totalCharges + " passagers chargés)");

        return true;
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
        
        List<Reservation> reservationsTriees = reservations.stream()
                .sorted((r1, r2) -> Integer.compare(r2.getNbrPers(), r1.getNbrPers()))
                .collect(Collectors.toList());

        Map<Integer, EtatVehiculeGroupe> etatsGroupe = new LinkedHashMap<>();

        List<Reservation> reservationsRestantes = new ArrayList<>(reservationsTriees);

        while (!reservationsRestantes.isEmpty()) {
            Reservation reservation = choisirProchaineReservationPourSplit(reservationsRestantes, etatsGroupe.values());
            if (reservation == null) {
                break;
            }
            reservationsRestantes.removeIf(r -> r.getIdReservation() == reservation.getIdReservation());

            int passagersRestants = assignationRepository.getPassagersRestantsByReservationId(reservation.getIdReservation());

            while (passagersRestants > 0) {
                // Sprint 7 : priorité aux véhicules entamés du groupe courant
                EtatVehiculeGroupe vehiculeEntame = choisirVehiculeEntame(etatsGroupe.values(), passagersRestants, vehiculePlans);
                if (vehiculeEntame != null) {
                    int nbAssignes = Math.min(passagersRestants, vehiculeEntame.capaciteRestante);
                    vehiculeEntame.ajouterReservation(reservation, nbAssignes);
                    enregistrerAssignation(reservation, vehiculeEntame.vehicule, date, nbAssignes);
                    passagersRestants -= nbAssignes;
                    continue;
                }

                List<Vehicule> vehiculesDisponibles = tousVehicules.stream()
                        .filter(v -> !etatsGroupe.containsKey(v.getIdVehicule()))
                        .filter(v -> estVehiculeDisponible(v, vehiculesHeureRetour, heureVol))
                        .collect(Collectors.toList());

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

        // Finaliser les voyages du groupe
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

    private Reservation choisirProchaineReservationPourSplit(List<Reservation> reservationsRestantes,
                                                             Collection<EtatVehiculeGroupe> etatsGroupe) {
        if (reservationsRestantes == null || reservationsRestantes.isEmpty()) {
            return null;
        }

        List<EtatVehiculeGroupe> vehiculesEntames = etatsGroupe.stream()
                .filter(e -> e.capaciteRestante > 0)
                .collect(Collectors.toList());

        if (vehiculesEntames.isEmpty()) {
            return reservationsRestantes.stream()
                    .max(Comparator.comparingInt(Reservation::getNbrPers))
                    .orElse(reservationsRestantes.get(0));
        }

        return reservationsRestantes.stream()
                .min((r1, r2) -> {
                    int restants1 = assignationRepository.getPassagersRestantsByReservationId(r1.getIdReservation());
                    int restants2 = assignationRepository.getPassagersRestantsByReservationId(r2.getIdReservation());

                    int delta1 = deltaCapaciteLePlusProche(restants1, vehiculesEntames);
                    int delta2 = deltaCapaciteLePlusProche(restants2, vehiculesEntames);

                    if (delta1 != delta2) {
                        return Integer.compare(delta1, delta2);
                    }

                    if (restants1 != restants2) {
                        return Integer.compare(restants2, restants1);
                    }

                    return r1.getDateHeureArrivee().compareTo(r2.getDateHeureArrivee());
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