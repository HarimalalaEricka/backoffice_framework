package com.app.planification;

import com.app.models.Reservation;
import java.util.List;
import java.util.ArrayList;

/**
 * DTO contenant le résultat de la planification.
 * - Liste des véhicules avec leurs réservations assignées
 * - Liste des réservations non assignées (faute de véhicule disponible)
 */
public class PlanificationResult {
    
    private List<VehiculePlanDTO> vehiculesAssignes;
    private List<Reservation> reservationsNonAssignees;

    // Constructors
    public PlanificationResult() {
        this.vehiculesAssignes = new ArrayList<>();
        this.reservationsNonAssignees = new ArrayList<>();
    }

    public PlanificationResult(List<VehiculePlanDTO> vehiculesAssignes, 
                               List<Reservation> reservationsNonAssignees) {
        this.vehiculesAssignes = vehiculesAssignes != null ? vehiculesAssignes : new ArrayList<>();
        this.reservationsNonAssignees = reservationsNonAssignees != null ? reservationsNonAssignees : new ArrayList<>();
    }

    // Getters and Setters
    public List<VehiculePlanDTO> getVehiculesAssignes() {
        return vehiculesAssignes;
    }

    public void setVehiculesAssignes(List<VehiculePlanDTO> vehiculesAssignes) {
        this.vehiculesAssignes = vehiculesAssignes;
    }

    public List<Reservation> getReservationsNonAssignees() {
        return reservationsNonAssignees;
    }

    public void setReservationsNonAssignees(List<Reservation> reservationsNonAssignees) {
        this.reservationsNonAssignees = reservationsNonAssignees;
    }

    // Méthodes utilitaires
    public void addVehiculePlan(VehiculePlanDTO vehiculePlan) {
        if (this.vehiculesAssignes == null) {
            this.vehiculesAssignes = new ArrayList<>();
        }
        this.vehiculesAssignes.add(vehiculePlan);
    }

    public void addReservationNonAssignee(Reservation reservation) {
        if (this.reservationsNonAssignees == null) {
            this.reservationsNonAssignees = new ArrayList<>();
        }
        this.reservationsNonAssignees.add(reservation);
    }

    public void addAllReservationsNonAssignees(List<Reservation> reservations) {
        if (this.reservationsNonAssignees == null) {
            this.reservationsNonAssignees = new ArrayList<>();
        }
        this.reservationsNonAssignees.addAll(reservations);
    }

    // Statistiques
    public int getNombreVehiculesUtilises() {
        return vehiculesAssignes != null ? vehiculesAssignes.size() : 0;
    }

    public int getNombreReservationsAssignees() {
        if (vehiculesAssignes == null) return 0;
        return vehiculesAssignes.stream()
                .mapToInt(v -> v.getReservations().size())
                .sum();
    }

    public int getNombreReservationsNonAssignees() {
        return reservationsNonAssignees != null ? reservationsNonAssignees.size() : 0;
    }

    public int getTotalPersonnesAssignees() {
        if (vehiculesAssignes == null) return 0;
        return vehiculesAssignes.stream()
                .mapToInt(VehiculePlanDTO::getTotalPersonnes)
                .sum();
    }
}