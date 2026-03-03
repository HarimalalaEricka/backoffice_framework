package com.app.controllers;

import com.framework.annotation.*;
import com.framework.model.ModelView;
import com.app.models.Vehicule;
import com.app.repository.VehiculeRepository;
import com.app.repository.TokenRepository;
import com.app.service.VehiculeService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Contrôleur des véhicules utilisant le Framework avec ModelView.
 * Gère l'affichage du formulaire et l'insertion des véhicules.
 */
@Controller
public class VehiculeController {

    private static final String URL = "jdbc:postgresql://localhost:5432/gestion_ticket";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "postgres";

    /**
     * Affiche le formulaire d'ajout de véhicule et la liste des véhicules
     */
    @HandleGet("/vehicules/insert")
    public ModelView insertForm(HttpServletRequest request) {
        ModelView mv = new ModelView();
        
        // Vérifier le token stocké en session
        HttpSession session = request.getSession(false);
        String token = null;
        if (session != null) {
            token = (String) session.getAttribute("token");
        }
        
        TokenRepository tokenRepo = new TokenRepository(URL, USERNAME, PASSWORD);
        String tokenError = tokenRepo.getTokenErrorMessage(token);
        
        if (tokenError != null) {
            mv.addAttribute("error", tokenError);
            mv.addAttribute("vehicules", new java.util.ArrayList<>());
        } else {
            VehiculeRepository vehiculeRepo = new VehiculeRepository(URL, USERNAME, PASSWORD);
            List<Vehicule> vehicules = vehiculeRepo.findAll();
            mv.addAttribute("vehicules", vehicules);
        }
        
        mv.setView("/vehicules/vehicules.jsp");
        return mv;
    }

    /**
     * Traite l'insertion d'un nouveau véhicule
     */
    @HandlePost("/vehicules/insert")
    public ModelView handleInsert(@RequestParam("reference") String reference,
                                   @RequestParam("nbr_places") int nbrPlaces,
                                   @RequestParam("type_carburant") String typeCarburant) {
        ModelView mv = new ModelView();
        
        VehiculeRepository vehiculeRepo = new VehiculeRepository(URL, USERNAME, PASSWORD);
        VehiculeService service = new VehiculeService(vehiculeRepo);
        
        try {
            // Validation et insertion
            Vehicule vehicule = new Vehicule(0, reference, nbrPlaces, typeCarburant);
            service.insertVehicule(vehicule);
            
            mv.addAttribute("message", "Véhicule ajouté avec succès.");
            // Rafraîchir la liste
            mv.addAttribute("vehicules", vehiculeRepo.findAll());
        } catch (IllegalArgumentException e) {
            mv.addAttribute("error", "Erreur : " + e.getMessage());
            mv.addAttribute("vehicules", vehiculeRepo.findAll());
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de l'ajout : " + e.getMessage());
            mv.addAttribute("vehicules", vehiculeRepo.findAll());
        }
        
        mv.setView("/vehicules/vehicules.jsp");
        return mv;
    }

    /**
     * Supprime un véhicule par son ID
     */
    @HandlePost("/vehicules/delete")
    public ModelView handleDelete(@RequestParam("id_vehicule") int idVehicule) {
        ModelView mv = new ModelView();
        
        VehiculeRepository vehiculeRepo = new VehiculeRepository(URL, USERNAME, PASSWORD);
        
        try {
            vehiculeRepo.deleteVehicule(idVehicule);
            mv.addAttribute("message", "Véhicule supprimé avec succès.");
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de la suppression : " + e.getMessage());
        }
        
        mv.addAttribute("vehicules", vehiculeRepo.findAll());
        mv.setView("/vehicules/vehicules.jsp");
        return mv;
    }

    /**
     * Affiche le formulaire d'édition d'un véhicule
     */
    @HandleGet("/vehicules/edit")
    public ModelView editForm(@RequestParam("id") int idVehicule) {
        ModelView mv = new ModelView();
        
        VehiculeRepository vehiculeRepo = new VehiculeRepository(URL, USERNAME, PASSWORD);
        Vehicule vehicule = vehiculeRepo.findById(idVehicule);
        
        if (vehicule != null) {
            mv.addAttribute("vehicule", vehicule);
            mv.setView("/vehicules/vehicules_edit.jsp");
        } else {
            mv.addAttribute("error", "Véhicule non trouvé.");
            mv.addAttribute("vehicules", vehiculeRepo.findAll());
            mv.setView("/vehicules/vehicules.jsp");
        }
        
        return mv;
    }

    /**
     * Traite la modification d'un véhicule
     */
    @HandlePost("/vehicules/update")
    public ModelView handleUpdate(@RequestParam("id_vehicule") int idVehicule,
                                  @RequestParam("reference") String reference,
                                  @RequestParam("nbr_places") int nbrPlaces,
                                  @RequestParam("type_carburant") String typeCarburant) {
        ModelView mv = new ModelView();
        
        VehiculeRepository vehiculeRepo = new VehiculeRepository(URL, USERNAME, PASSWORD);
        
        try {
            Vehicule vehicule = new Vehicule(idVehicule, reference, nbrPlaces, typeCarburant);
            vehiculeRepo.updateVehicule(vehicule);
            
            mv.addAttribute("message", "Véhicule modifié avec succès.");
            mv.addAttribute("vehicules", vehiculeRepo.findAll());
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de la modification : " + e.getMessage());
            mv.addAttribute("vehicules", vehiculeRepo.findAll());
        }
        
        mv.setView("/vehicules/vehicules.jsp");
        return mv;
    }
}