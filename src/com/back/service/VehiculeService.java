package com.app.service;

import com.app.models.Vehicule;
import com.app.repository.VehiculeRepository;
import java.util.List;
import java.util.logging.Logger;

public class VehiculeService {

    private static final Logger logger = Logger.getLogger(VehiculeService.class.getName());
    private final VehiculeRepository vehiculeRepo;

    public VehiculeService(VehiculeRepository vehiculeRepo) {
        this.vehiculeRepo = vehiculeRepo;
    }

    public List<Vehicule> getAllVehicules() {
        logger.info("Récupération de tous les véhicules");
        return vehiculeRepo.findAll();
    }
}
