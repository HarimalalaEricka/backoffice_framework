package com.app.controllers;

import com.app.models.Vehicule;
import com.app.service.VehiculeService;
import com.app.repository.VehiculeRepository;
import com.framework.annotation.Controller;
import com.framework.annotation.HandleGet;
import com.framework.annotation.JsonResponse;
import com.framework.model.ModelView;
import java.util.List;

@Controller
public class VehiculeController {

    private final VehiculeService service;

    public VehiculeController() {
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String user = "postgres";
        String password = "postgres";
        VehiculeRepository vehiculeRepo = new VehiculeRepository(url, user, password);
        this.service = new VehiculeService(vehiculeRepo);
    }

    @HandleGet("/api/vehicules")
    @JsonResponse
    public List<Vehicule> getAllVehicules() {
        return service.getAllVehicules();
    }

    @HandleGet("/vehicules/list")
    public ModelView listVehicules() {
        ModelView mv = new ModelView();
        try {
            List<Vehicule> vehicules = service.getAllVehicules();
            mv.addAttribute("vehicules", vehicules);
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de la récupération des véhicules : " + e.getMessage());
        }
        mv.setView("/vehicules_list.jsp");
        return mv;
    }
}
