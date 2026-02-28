package com.app.models;

import java.time.LocalDateTime;

public class Vehicule {
    private int idVehicule;
    private String reference;
    private int nbrPlaces;
    private String typeCarburant;

    // Constructors
    public Vehicule() {}

    public Vehicule(int idVehicule, String reference, int nbrPlaces, String typeCarburant) {
        this.idVehicule = idVehicule;
        this.reference = reference;
        this.nbrPlaces = nbrPlaces;
        this.typeCarburant = typeCarburant;
    }

    // Getters and Setters
    public int getIdVehicule() {
        return idVehicule;
    }

    public void setIdVehicule(int idVehicule) {
        this.idVehicule = idVehicule;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public int getNbrPlaces() {
        return nbrPlaces;
    }

    public void setNbrPlaces(int nbrPlaces) {
        this.nbrPlaces = nbrPlaces;
    }

    public String getTypeCarburant() {
        return typeCarburant;
    }

    public void setTypeCarburant(String typeCarburant) {
        this.typeCarburant = typeCarburant;
    }
}