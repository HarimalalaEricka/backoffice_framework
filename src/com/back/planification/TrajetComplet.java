package com.app.planification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe représentant le résultat complet du calcul d'un trajet.
 * Contient l'heure de retour, la distance totale et les détails de chaque arrêt.
 */
public class TrajetComplet {
    
    private LocalDateTime heureRetour;       // Heure de retour à l'aéroport
    private int distanceTotale;               // Distance totale parcourue (aller + retour) en km
    private List<TrajetDetailDTO> detailsTrajet;  // Liste des arrêts avec détails

    // Constructors
    public TrajetComplet() {
        this.detailsTrajet = new ArrayList<>();
    }

    public TrajetComplet(LocalDateTime heureRetour, int distanceTotale, List<TrajetDetailDTO> detailsTrajet) {
        this.heureRetour = heureRetour;
        this.distanceTotale = distanceTotale;
        this.detailsTrajet = detailsTrajet != null ? detailsTrajet : new ArrayList<>();
    }

    // Getters and Setters
    public LocalDateTime getHeureRetour() {
        return heureRetour;
    }

    public void setHeureRetour(LocalDateTime heureRetour) {
        this.heureRetour = heureRetour;
    }

    public int getDistanceTotale() {
        return distanceTotale;
    }

    public void setDistanceTotale(int distanceTotale) {
        this.distanceTotale = distanceTotale;
    }

    public List<TrajetDetailDTO> getDetailsTrajet() {
        return detailsTrajet;
    }

    public void setDetailsTrajet(List<TrajetDetailDTO> detailsTrajet) {
        this.detailsTrajet = detailsTrajet;
    }

    public void addDetail(TrajetDetailDTO detail) {
        if (this.detailsTrajet == null) {
            this.detailsTrajet = new ArrayList<>();
        }
        this.detailsTrajet.add(detail);
    }
}
