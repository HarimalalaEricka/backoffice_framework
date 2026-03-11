package com.app.planification;

import java.time.LocalDateTime;

/**
 * DTO représentant les détails d'un arrêt (hôtel) dans le trajet d'un véhicule.
 */
public class TrajetDetailDTO {
    
    private int ordre;              // Ordre de passage (1, 2, 3...)
    private String nomHotel;        // Nom de l'hôtel
    private LocalDateTime heureArrivee;  // Heure d'arrivée estimée à l'hôtel
    private int distanceSegment;    // Distance depuis l'arrêt précédent (km)
    private int distanceCumulee;    // Distance totale depuis l'aéroport (km)

    // Constructors
    public TrajetDetailDTO() {}

    public TrajetDetailDTO(int ordre, String nomHotel, LocalDateTime heureArrivee, 
                           int distanceSegment, int distanceCumulee) {
        this.ordre = ordre;
        this.nomHotel = nomHotel;
        this.heureArrivee = heureArrivee;
        this.distanceSegment = distanceSegment;
        this.distanceCumulee = distanceCumulee;
    }

    // Getters and Setters
    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public String getNomHotel() {
        return nomHotel;
    }

    public void setNomHotel(String nomHotel) {
        this.nomHotel = nomHotel;
    }

    public LocalDateTime getHeureArrivee() {
        return heureArrivee;
    }

    public void setHeureArrivee(LocalDateTime heureArrivee) {
        this.heureArrivee = heureArrivee;
    }

    public int getDistanceSegment() {
        return distanceSegment;
    }

    public void setDistanceSegment(int distanceSegment) {
        this.distanceSegment = distanceSegment;
    }

    public int getDistanceCumulee() {
        return distanceCumulee;
    }

    public void setDistanceCumulee(int distanceCumulee) {
        this.distanceCumulee = distanceCumulee;
    }
}
