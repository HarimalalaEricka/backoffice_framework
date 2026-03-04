package com.app.planification;

import com.app.models.Vehicule;
import com.app.models.Reservation;
import java.util.List;
import java.util.ArrayList;

/**
 * DTO représentant un véhicule avec ses réservations assignées.
 */
public class VehiculePlanDTO {
    
    private Vehicule vehicule;
    private List<Reservation> reservations;

    // Constructors
    public VehiculePlanDTO() {
        this.reservations = new ArrayList<>();
    }

    public VehiculePlanDTO(Vehicule vehicule, List<Reservation> reservations) {
        this.vehicule = vehicule;
        this.reservations = reservations != null ? reservations : new ArrayList<>();
    }

    // Getters and Setters
    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    // Méthode utilitaire pour ajouter une réservation
    public void addReservation(Reservation reservation) {
        if (this.reservations == null) {
            this.reservations = new ArrayList<>();
        }
        this.reservations.add(reservation);
    }

    // Calcul du nombre total de personnes assignées à ce véhicule
    public int getTotalPersonnes() {
        if (reservations == null) return 0;
        return reservations.stream()
                .mapToInt(Reservation::getNbrPers)
                .sum();
    }
}