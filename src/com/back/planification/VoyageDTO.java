package com.app.planification;

import com.app.models.Reservation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private Map<Integer, Integer> passagersAssignesParReservation; // reservationId -> nb pers assignées

    // Constructors
    public VoyageDTO() {
        this.reservations = new ArrayList<>();
        this.detailsTrajet = new ArrayList<>();
        this.passagersAssignesParReservation = new LinkedHashMap<>();
    }

    public VoyageDTO(int numeroVoyage, LocalDateTime heureDepart, LocalDateTime heureRetour, 
                     int distanceTotale, List<Reservation> reservations, List<TrajetDetailDTO> detailsTrajet) {
        this.numeroVoyage = numeroVoyage;
        this.heureDepart = heureDepart;
        this.heureRetour = heureRetour;
        this.distanceTotale = distanceTotale;
        this.reservations = reservations != null ? reservations : new ArrayList<>();
        this.detailsTrajet = detailsTrajet != null ? detailsTrajet : new ArrayList<>();
        this.passagersAssignesParReservation = new LinkedHashMap<>();
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

    public Map<Integer, Integer> getPassagersAssignesParReservation() {
        return passagersAssignesParReservation;
    }

    public void setPassagersAssignesParReservation(Map<Integer, Integer> passagersAssignesParReservation) {
        this.passagersAssignesParReservation = passagersAssignesParReservation != null
                ? passagersAssignesParReservation
                : new LinkedHashMap<>();
    }

    public void setPassagersAssignesPourReservation(int reservationId, int nbPassagers) {
        if (this.passagersAssignesParReservation == null) {
            this.passagersAssignesParReservation = new LinkedHashMap<>();
        }
        this.passagersAssignesParReservation.put(reservationId, nbPassagers);
    }

    public int getPassagersAssignesPourReservation(Reservation reservation) {
        if (reservation == null) {
            return 0;
        }

        if (passagersAssignesParReservation == null || !passagersAssignesParReservation.containsKey(reservation.getIdReservation())) {
            return reservation.getNbrPers();
        }

        return passagersAssignesParReservation.get(reservation.getIdReservation());
    }

    // Méthodes utilitaires
    public int getTotalPersonnes() {
        if (reservations == null) return 0;
        return reservations.stream()
            .mapToInt(this::getPassagersAssignesPourReservation)
            .sum();
    }

    public long getDureeVoyage() {
        if (heureDepart == null || heureRetour == null) return 0;
        return java.time.Duration.between(heureDepart, heureRetour).toMinutes();
    }
}
