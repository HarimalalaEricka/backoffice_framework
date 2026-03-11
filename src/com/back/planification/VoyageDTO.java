package com.app.planification;

import com.app.models.Reservation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO représentant un voyage complet (aller-retour) d'un véhicule.
 * Un véhicule peut faire plusieurs voyages dans la journée.
 */
public class VoyageDTO {
    
    private int numeroVoyage;                      // Numéro du voyage (1, 2, 3...)
    private LocalDateTime heureDepart;             // Heure de départ de l'aéroport
    private LocalDateTime heureRetour;             // Heure de retour à l'aéroport
    private int distanceTotale;                    // Distance totale du voyage (km)
    private List<Reservation> reservations;        // Réservations de ce voyage
    private List<TrajetDetailDTO> detailsTrajet;   // Détails des arrêts

    // Constructors
    public VoyageDTO() {
        this.reservations = new ArrayList<>();
        this.detailsTrajet = new ArrayList<>();
    }

    public VoyageDTO(int numeroVoyage, LocalDateTime heureDepart, LocalDateTime heureRetour, 
                     int distanceTotale, List<Reservation> reservations, List<TrajetDetailDTO> detailsTrajet) {
        this.numeroVoyage = numeroVoyage;
        this.heureDepart = heureDepart;
        this.heureRetour = heureRetour;
        this.distanceTotale = distanceTotale;
        this.reservations = reservations != null ? reservations : new ArrayList<>();
        this.detailsTrajet = detailsTrajet != null ? detailsTrajet : new ArrayList<>();
    }

    // Getters and Setters
    public int getNumeroVoyage() {
        return numeroVoyage;
    }

    public void setNumeroVoyage(int numeroVoyage) {
        this.numeroVoyage = numeroVoyage;
    }

    public LocalDateTime getHeureDepart() {
        return heureDepart;
    }

    public void setHeureDepart(LocalDateTime heureDepart) {
        this.heureDepart = heureDepart;
    }

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

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public List<TrajetDetailDTO> getDetailsTrajet() {
        return detailsTrajet;
    }

    public void setDetailsTrajet(List<TrajetDetailDTO> detailsTrajet) {
        this.detailsTrajet = detailsTrajet;
    }

    // Méthodes utilitaires
    public int getTotalPersonnes() {
        if (reservations == null) return 0;
        return reservations.stream()
                .mapToInt(Reservation::getNbrPers)
                .sum();
    }

    public long getDureeVoyage() {
        if (heureDepart == null || heureRetour == null) return 0;
        return java.time.Duration.between(heureDepart, heureRetour).toMinutes();
    }
}
