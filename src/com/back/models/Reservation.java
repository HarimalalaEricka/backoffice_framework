package com.back.models;

import java.time.LocalDateTime;

public class Reservation {
    private int idReservation;
    private String client_id;
    private int nbr_pers;
    private LocalDateTime date_heure_arrivee;
    private int hotel_id;

    public Reservation(int idReservation, String client_id, int nbr_pers, LocalDateTime date_heure_arrivee, int hotel_id) {
        this.idReservation = idReservation;
        this.client_id = client_id;
        this.nbr_pers = nbr_pers;
        this.date_heure_arrivee = date_heure_arrivee;
        this.hotel_id = hotel_id;
    }

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public String getClientId() { return client_id; }
    public void setClientId(String client_id) { this.client_id = client_id; }

    public int getNbrPers() { return nbr_pers; }
    public void setNbrPers(int nbr_pers) { this.nbr_pers = nbr_pers; }

    public LocalDateTime getDateHeureArrivee() { return date_heure_arrivee; }
    public void setDateHeureArrivee(LocalDateTime date_heure_arrivee) { this.date_heure_arrivee = date_heure_arrivee; }

    public int getHotelId() { return hotel_id; }
    public void setHotelId(int hotel_id) { this.hotel_id = hotel_id; }
}