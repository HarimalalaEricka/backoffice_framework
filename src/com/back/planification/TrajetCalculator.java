package com.app.planification;

import com.app.models.Hotel;
import com.app.models.Parametre;
import com.app.models.Reservation;
import com.app.repository.DistanceRepository;
import com.app.repository.HotelRepository;
import com.app.repository.ParametreRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service de calcul de trajet pour les véhicules.
 * Calcule l'heure de retour, la distance totale et les détails de chaque arrêt.
 */
public class TrajetCalculator {

    private static final Logger logger = Logger.getLogger(TrajetCalculator.class.getName());

    private final DistanceRepository distanceRepository;
    private final ParametreRepository parametreRepository;
    private final HotelRepository hotelRepository;

    /**
     * Constructeur avec injection des repositories.
     */
    public TrajetCalculator(DistanceRepository distanceRepository,
                             ParametreRepository parametreRepository,
                             HotelRepository hotelRepository) {
        this.distanceRepository = distanceRepository;
        this.parametreRepository = parametreRepository;
        this.hotelRepository = hotelRepository;
    }

    /**
     * Calcule l'heure de retour du véhicule à l'aéroport.
     * Version simplifiée qui retourne uniquement l'heure de retour.
     * 
     * @param heureDepart Heure de départ de l'aéroport
     * @param reservations Liste des réservations à desservir
     * @return Heure de retour à l'aéroport
     */
    public LocalDateTime calculerHeureRetour(LocalDateTime heureDepart, List<Reservation> reservations) {
        TrajetComplet trajetComplet = calculerTrajetComplet(heureDepart, reservations);
        return trajetComplet != null ? trajetComplet.getHeureRetour() : heureDepart;
    }

    /**
     * Calcule le trajet complet avec tous les détails.
     * 
     * @param heureDepart Heure de départ de l'aéroport
     * @param reservations Liste des réservations à desservir
     * @return TrajetComplet contenant heureRetour, distanceTotale et détails des arrêts
     */
    public TrajetComplet calculerTrajetComplet(LocalDateTime heureDepart, List<Reservation> reservations) {
        
        if (reservations == null || reservations.isEmpty()) {
            logger.warning("Aucune réservation à traiter");
            return new TrajetComplet(heureDepart, 0, new ArrayList<>());
        }

        // Étape 1 : Récupérer l'aéroport
        Hotel aeroport = hotelRepository.findAeroport();
        if (aeroport == null) {
            logger.severe("Aéroport non trouvé dans la base de données");
            return new TrajetComplet(heureDepart, 0, new ArrayList<>());
        }
        int aeroportId = aeroport.getIdHotel();
        logger.info("Aéroport trouvé : " + aeroport.getNom() + " (ID: " + aeroportId + ")");

        // Étape 2 : Récupérer les paramètres
        Parametre parametre = parametreRepository.getParametre();
        if (parametre == null) {
            logger.severe("Paramètres non trouvés dans la base de données");
            return new TrajetComplet(heureDepart, 0, new ArrayList<>());
        }
        int vitesseMoyenne = parametre.getVitesseMoyenne();
        int tempsAttente = parametre.getTempsAttente();
        logger.info("Paramètres : vitesse=" + vitesseMoyenne + " km/h, attente=" + tempsAttente + " min");

        // Étape 3 : Extraire les hôtels uniques des réservations
        Set<Integer> hotelIdsSet = reservations.stream()
                .map(Reservation::getHotelId)
                .collect(Collectors.toSet());
        
        List<Integer> hotelIds = new ArrayList<>(hotelIdsSet);
        logger.info("Nombre d'hôtels à desservir : " + hotelIds.size());

        // Récupérer tous les hôtels pour afficher leurs noms
        List<Hotel> tousHotels = hotelRepository.findAllHotels();
        Map<Integer, String> hotelNoms = tousHotels.stream()
                .collect(Collectors.toMap(Hotel::getIdHotel, Hotel::getNom));

        // Étape 4 : SPRINT 4 RG9 - Algorithme nearest-first (plus proche voisin)
        // Au lieu de trier globalement, on choisit à chaque étape l'hôtel non visité le plus proche
        List<Integer> hotelsSorted = new ArrayList<>();
        List<Integer> hotelsRestants = new ArrayList<>(hotelIds);
        int positionActuelle = aeroportId;
        
        while (!hotelsRestants.isEmpty()) {
            // Trouver l'hôtel le plus proche de la position actuelle
            final int fromId = positionActuelle;
            
            // SPRINT 4 RG11 : En cas d'égalité de distance, trier par nom alphabétique
            Integer plusProche = hotelsRestants.stream()
                    .min((h1, h2) -> {
                        int dist1 = distanceRepository.getDistance(fromId, h1);
                        int dist2 = distanceRepository.getDistance(fromId, h2);
                        
                        if (dist1 != dist2) {
                            return Integer.compare(dist1, dist2);
                        }
                        
                        // RG11 : En cas d'égalité, ordre lexicographique
                        String nom1 = hotelNoms.getOrDefault(h1, "Hôtel inconnu #" + h1);
                        String nom2 = hotelNoms.getOrDefault(h2, "Hôtel inconnu #" + h2);
                        return nom1.compareTo(nom2);
                    })
                    .orElse(null);
            
            if (plusProche != null) {
                hotelsSorted.add(plusProche);
                hotelsRestants.remove(plusProche);
                positionActuelle = plusProche;
            }
        }
        
        hotelIds = hotelsSorted;
        logger.info("Ordre de visite (nearest-first) : " + hotelIds.stream()
                .map(id -> hotelNoms.getOrDefault(id, "Hotel#" + id))
                .collect(Collectors.joining(" → ")));

        // Construire le trajet : Aéroport → Hôtel1 → Hôtel2 → ... → Aéroport
        List<TrajetDetailDTO> detailsTrajet = new ArrayList<>();
        LocalDateTime heureActuelle = heureDepart;
        int distanceCumuleeAller = 0;
        int fromId = aeroportId;

        // Étape 5 : Calculer chaque segment du trajet (aller)
        int ordre = 1;
        for (int toId : hotelIds) {
            int distanceSegment = distanceRepository.getDistance(fromId, toId);
            distanceCumuleeAller += distanceSegment;

            // Calculer le temps de trajet
            double tempsHeures = (double) distanceSegment / vitesseMoyenne;
            int tempsMinutes = (int) (tempsHeures * 60) + tempsAttente;
            heureActuelle = heureActuelle.plusMinutes(tempsMinutes);

            // Créer le détail de l'arrêt
            String nomHotel = hotelNoms.getOrDefault(toId, "Hôtel inconnu #" + toId);
            TrajetDetailDTO detail = new TrajetDetailDTO(
                    ordre,
                    nomHotel,
                    heureActuelle,
                    distanceSegment,
                    distanceCumuleeAller
            );
            detailsTrajet.add(detail);

            logger.info("Arrêt " + ordre + " : " + nomHotel + " à " + heureActuelle + 
                       " (segment=" + distanceSegment + " km, cumulé=" + distanceCumuleeAller + " km)");

            fromId = toId;
            ordre++;
        }

        // Étape 6 : Retour à l'aéroport
        int distanceRetour = distanceRepository.getDistance(fromId, aeroportId);
        double tempsRetourHeures = (double) distanceRetour / vitesseMoyenne;
        int tempsRetourMinutes = (int) (tempsRetourHeures * 60);
        LocalDateTime heureRetour = heureActuelle.plusMinutes(tempsRetourMinutes);

        int distanceTotale = distanceCumuleeAller + distanceRetour;

        logger.info("Retour à l'aéroport : " + heureRetour + " (distance retour=" + distanceRetour + 
                   " km, total=" + distanceTotale + " km)");

        // Créer le résultat
        TrajetComplet trajetComplet = new TrajetComplet(heureRetour, distanceTotale, detailsTrajet);
        
        return trajetComplet;
    }
}
