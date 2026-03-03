package com.app.service;

import com.app.models.Vehicule;
import com.app.repository.VehiculeRepository;
import java.util.logging.Logger;
import java.util.List;
/**
 * Service métier pour la gestion des véhicules.
 * Effectue les validations métier avant insertion en base.
 */
public class VehiculeService {

    private static final Logger logger = Logger.getLogger(VehiculeService.class.getName());
    private final VehiculeRepository vehiculeRepo;

    public VehiculeService(VehiculeRepository vehiculeRepo) {
        this.vehiculeRepo = vehiculeRepo;
    }

    /**
     * Insère un véhicule après validation complète
     */
    public void insertVehicule(Vehicule v) {
        // Validation de la référence
        if (v.getReference() == null || v.getReference().trim().isEmpty()) {
            throw new IllegalArgumentException("La référence ne peut pas être vide");
        }

        // Validation du nombre de places
        if (v.getNbrPlaces() <= 0) {
            throw new IllegalArgumentException("Le nombre de places doit être supérieur à 0");
        }

        // Validation du type de carburant
        if (!isValidCarburant(v.getTypeCarburant())) {
            throw new IllegalArgumentException("Type de carburant invalide. Valeurs acceptées : D, ES, EL, H");
        }

        logger.info("Insertion d'un véhicule : " + v.getReference());
        vehiculeRepo.insertVehicule(v);
    }

    /**
     * Valide si le type de carburant est autorisé
     */
    private boolean isValidCarburant(String type) {
        return type != null && (type.equals("D") || type.equals("ES") || 
               type.equals("EL") || type.equals("H"));
    }

    public List<Vehicule> getAllVehicules() {
        logger.info("Récupération de tous les véhicules");
        return vehiculeRepo.findAll();
    }
}
