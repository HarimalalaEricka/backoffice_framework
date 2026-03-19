package com.app.controllers;

import com.app.models.Vehicule;
import com.app.service.VehiculeService;
import com.app.repository.VehiculeRepository;
import com.framework.annotation.HandleGet;
import com.framework.annotation.JsonResponse;
import java.util.List;
import com.framework.annotation.Controller;
import com.framework.annotation.HandlePost;
import com.framework.annotation.JsonResponse;
import com.framework.annotation.RequestParam;

@Controller
public class VehiculeRestController {
    
    private final VehiculeService service;

    public VehiculeRestController() {
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String user = "postgres";
        String password = "kanto";
        VehiculeRepository vehiculeRepo = new VehiculeRepository(url, user, password);
        this.service = new VehiculeService(vehiculeRepo);
    }

    @HandlePost("/vehicules/insert")
    @JsonResponse
    public String createVehicule(@RequestParam("reference") String reference,
                                 @RequestParam("nbr_places") int nbrPlaces,
                                 @RequestParam("type_carburant") String typeCarburant) {
        try {
            Vehicule vehicule = new Vehicule(0, reference, nbrPlaces, typeCarburant);
            service.insertVehicule(vehicule);
            return "{\"status\": \"success\", \"message\": \"Véhicule créé\"}";
        } catch (IllegalArgumentException e) {
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    @HandleGet("/api/vehicules")
    @JsonResponse
    public List<Vehicule> listVehicules() {
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String user = "postgres";
        String password = "postgres";
        VehiculeRepository vehiculeRepo = new VehiculeRepository(url, user, password);
        return vehiculeRepo.findAll();
    }
}