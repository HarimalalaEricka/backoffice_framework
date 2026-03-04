package com.app.models;

import java.time.LocalDate;

public class Assignation {

    private int idAssignation;
    private int reservationId;
    private int vehiculeId;
    private LocalDate datePlanification;

    // Constructors
    public Assignation() {}

    public Assignation(int idAssignation, int reservationId, int vehiculeId, LocalDate datePlanification) {
        this.idAssignation = idAssignation;
        this.reservationId = reservationId;
        this.vehiculeId = vehiculeId;
        this.datePlanification = datePlanification;
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

    public LocalDate getDatePlanification() {
        return datePlanification;
    }

    public void setDatePlanification(LocalDate datePlanification) {
        this.datePlanification = datePlanification;
    }
}