package com.app.planification;

import com.app.models.Vehicule;
import com.app.models.Reservation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * DTO représentant un véhicule avec ses réservations assignées.
 */
public class VehiculePlanDTO {
    
    private Vehicule vehicule;
    private List<Reservation> reservations;
    
    // Sprint 3 - Gestion des voyages multiples
    private List<VoyageDTO> voyages;  // Liste des voyages (aller-retour) du véhicule
    
    // Attributs dépréciés (conservés pour compatibilité)
    @Deprecated
    private LocalDateTime heureDepart;         
    @Deprecated
    private LocalDateTime heureRetour;         
    @Deprecated
    private int distanceTotale;                 
    @Deprecated
    private List<TrajetDetailDTO> detailsTrajet;

    // Constructors
    public VehiculePlanDTO() {
        this.reservations = new ArrayList<>();
        this.voyages = new ArrayList<>();
        this.detailsTrajet = new ArrayList<>();
    }

    public VehiculePlanDTO(Vehicule vehicule, List<Reservation> reservations) {
        this.vehicule = vehicule;
        this.reservations = reservations != null ? reservations : new ArrayList<>();
        this.voyages = new ArrayList<>();
        this.detailsTrajet = new ArrayList<>();
    }

    // Getters and Setters
    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }

    /**
     * CORRIGÉ : Retourne toutes les réservations (agrégées des voyages si présents)
     */
    public List<Reservation> getReservations() {
        // Si on a des voyages, agréger les réservations de tous les voyages
        if (voyages != null && !voyages.isEmpty()) {
            List<Reservation> toutesReservations = new ArrayList<>();
            for (VoyageDTO voyage : voyages) {
                if (voyage.getReservations() != null) {
                    toutesReservations.addAll(voyage.getReservations());
                }
            }
            return toutesReservations;
        }
        // Fallback sur la liste directe (compatibilité)
        return reservations != null ? reservations : new ArrayList<>();
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
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

    public List<TrajetDetailDTO> getDetailsTrajet() {
        return detailsTrajet;
    }

    public void setDetailsTrajet(List<TrajetDetailDTO> detailsTrajet) {
        this.detailsTrajet = detailsTrajet;
    }

    // Getters et Setters pour les voyages
    public List<VoyageDTO> getVoyages() {
        return voyages;
    }

    public void setVoyages(List<VoyageDTO> voyages) {
        this.voyages = voyages;
    }

    public void addVoyage(VoyageDTO voyage) {
        if (this.voyages == null) {
            this.voyages = new ArrayList<>();
        }
        this.voyages.add(voyage);
    }

    // Méthode utilitaire pour ajouter une réservation
    public void addReservation(Reservation reservation) {
        if (this.reservations == null) {
            this.reservations = new ArrayList<>();
        }
        this.reservations.add(reservation);
    }

    /**
     * CORRIGÉ : Calcul du nombre total de personnes assignées à ce véhicule
     * Agrège les réservations de tous les voyages
     */
    public int getTotalPersonnes() {
        // Si on a des voyages, calculer depuis les voyages
        if (voyages != null && !voyages.isEmpty()) {
            return voyages.stream()
                    .flatMap(v -> v.getReservations().stream())
                    .mapToInt(Reservation::getNbrPers)
                    .sum();
        }
        // Fallback sur la liste directe (compatibilité)
        if (reservations == null) return 0;
        return reservations.stream()
                .mapToInt(Reservation::getNbrPers)
                .sum();
    }

    // Calcul du nombre de voyages effectués
    public int getNombreVoyages() {
        return voyages != null ? voyages.size() : 0;
    }

    // Calcul de la distance totale de tous les voyages
    public int getDistanceTotaleTousVoyages() {
        if (voyages == null) return distanceTotale; // Fallback sur l'ancien attribut
        return voyages.stream()
                .mapToInt(VoyageDTO::getDistanceTotale)
                .sum();
    }
}