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
    private String code; 
    private String libelle;

    public Hotel(int idHotel, String nom, String code, String libelle) {
        this.idHotel = idHotel;
        this.nom = nom;
        this.code = code;
        this.libelle = libelle;
    }

    // constructeur simplifié pour compatibilité ascendante
    public Hotel(int idHotel, String nom) {
        this(idHotel, nom, null, null);
    }

    public int getIdHotel() { return idHotel; }
    public void setIdHotel(int idHotel) { this.idHotel = idHotel; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom ; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}