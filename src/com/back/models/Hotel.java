package com.app.models;

import java.io.Serializable;

/**
 * Modèle Hotel pour le framework.
 * Sérializable pour JSON et base de données.
 */
public class Hotel implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int idHotel;
    private String nom;

    public Hotel(int idHotel, String nom) {
        this.idHotel = idHotel;
        this.nom = nom;
    }

    public int getIdHotel() { return idHotel; }
    public void setIdHotel(int idHotel) { this.idHotel = idHotel; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom ; }
}