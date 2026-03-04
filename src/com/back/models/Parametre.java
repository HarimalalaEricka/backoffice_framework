package com.app.models;

public class Parametre {

    private int idParametre;
    private int vitesseMoyenne;
    private int tempsAttente;

    // Constructors
    public Parametre() {}

    public Parametre(int idParametre, int vitesseMoyenne, int tempsAttente) {
        this.idParametre = idParametre;
        this.vitesseMoyenne = vitesseMoyenne;
        this.tempsAttente = tempsAttente;
    }

    // Getters and Setters
    public int getIdParametre() {
        return idParametre;
    }

    public void setIdParametre(int idParametre) {
        this.idParametre = idParametre;
    }

    public int getVitesseMoyenne() {
        return vitesseMoyenne;
    }

    public void setVitesseMoyenne(int vitesseMoyenne) {
        this.vitesseMoyenne = vitesseMoyenne;
    }

    public int getTempsAttente() {
        return tempsAttente;
    }

    public void setTempsAttente(int tempsAttente) {
        this.tempsAttente = tempsAttente;
    }
}
