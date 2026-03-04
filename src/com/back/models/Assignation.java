package com.app.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Assignation {

    private int idAssignation;
    private int reservationId;
    private int vehiculeId;
    private LocalDateTime date_heure_planification;

    // Constructors
    public Assignation() {}

    public Assignation(int idAssignation, int reservationId, int vehiculeId, LocalDateTime dateHeurePlanification) {
        this.idAssignation = idAssignation;
        this.reservationId = reservationId;
        this.vehiculeId = vehiculeId;
        this.date_heure_planification = dateHeurePlanification;
    }

    // Getters and Setters
    public int getIdAssignation() {
        return idAssignation;
    }

    public void setIdAssignation(int idAssignation) {
        this.idAssignation = idAssignation;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public int getVehiculeId() {
        return vehiculeId;
    }

    public void setVehiculeId(int vehiculeId) {
        this.vehiculeId = vehiculeId;
    }

    public LocalDateTime getDateHeurePlanification() {
        return date_heure_planification;
    }

    public void setDateHeurePlanification(LocalDateTime dateHeurePlanification) {
        this.date_heure_planification = dateHeurePlanification;
    }
}