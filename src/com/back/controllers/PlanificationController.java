package com.app.controllers;
import com.framework.annotation.*;
import com.framework.model.ModelView;
import com.app.planification.PlanificationService;
import com.app.planification.PlanificationResult;
import com.app.planification.VehiculePlanDTO;
import com.app.models.Reservation;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
public class PlanificationController {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/gestion_ticket";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    @HandleGet("/planification/form")
    public ModelView planificationForm() {
        ModelView mv = new ModelView();
        mv.setView("/planifier.jsp");
        return mv;
    }

    @HandlePost("/planification")
    public ModelView executerPlanification(@RequestParam("datePlanification") String datePlanification) {
        ModelView mv = new ModelView();
        
        try {
            // Validation de la date
            if (datePlanification == null || datePlanification.trim().isEmpty()) {
                mv.addAttribute("error", "La date de planification est requise.");
                mv.setView("/planifier.jsp");
                return mv;
            }
            
            // Parser la date
            LocalDate date;
            try {
                date = LocalDate.parse(datePlanification);
            } catch (DateTimeParseException e) {
                mv.addAttribute("error", "Format de date invalide. Utilisez le format YYYY-MM-DD.");
                mv.setView("/planifier.jsp");
                return mv;
            }
            
            // Vérifier que la date n'est pas dans le passé
            if (date.isBefore(LocalDate.now())) {
                mv.addAttribute("error", "La date de planification ne peut pas être dans le passé.");
                mv.setView("/planifier.jsp");
                return mv;
            }
            
            // Exécuter la planification
            PlanificationService planificationService = new PlanificationService();
            PlanificationResult result = planificationService.planifierJour(date);
            
            // Ajouter les résultats au ModelView
            mv.addAttribute("datePlanification", date);
            mv.addAttribute("vehiculesAssignes", result.getVehiculesAssignes());
            mv.addAttribute("reservationsNonAssignees", result.getReservationsNonAssignees());
            mv.addAttribute("nombreVehiculesUtilises", result.getNombreVehiculesUtilises());
            mv.addAttribute("nombreReservationsAssignees", result.getNombreReservationsAssignees());
            mv.addAttribute("nombreReservationsNonAssignees", result.getNombreReservationsNonAssignees());
            mv.addAttribute("totalPersonnesAssignees", result.getTotalPersonnesAssignees());
            mv.addAttribute("success", "Planification effectuée avec succès pour le " + date);
            
            mv.setView("/planification_result.jsp");
            
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de la planification : " + e.getMessage());
            mv.setView("/planifier.jsp");
        }
        
        return mv;
    }

    /**
     * API JSON : Récupère le résultat de la planification pour une date.
     * Route: GET /api/planification?date=YYYY-MM-DD
     * Protégé par AuthFilter (token requis)
     * 
     * @param date La date au format YYYY-MM-DD
     * @return PlanificationResult en JSON
     */
    @HandleGet("/api/planification")
    @JsonResponse
    public Object getPlanification(@RequestParam("date") String date) {
        try {
            // Validation de la date
            if (date == null || date.trim().isEmpty()) {
                return new ApiError("error", "Le paramètre 'date' est requis.");
            }
            
            // Parser la date
            LocalDate localDate;
            try {
                localDate = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                return new ApiError("error", "Format de date invalide. Utilisez le format YYYY-MM-DD.");
            }
            
            // Exécuter la planification
            PlanificationService planificationService = new PlanificationService();
            PlanificationResult result = planificationService.planifierJour(localDate);
            
            return result;
            
        } catch (Exception e) {
            return new ApiError("error", "Erreur lors de la planification : " + e.getMessage());
        }
    }

    /**
     * API JSON : Lance la planification pour une date (POST).
     * Route: POST /api/planification?date=YYYY-MM-DD
     * Protégé par AuthFilter (token requis)
     * 
     * @param date La date au format YYYY-MM-DD
     * @return PlanificationResult en JSON
     */
    @HandlePost("/api/planification")
    @JsonResponse
    public Object postPlanification(@RequestParam("date") String date) {
        return getPlanification(date);
    }

    /**
     * Classe interne pour les erreurs API.
     */
    public static class ApiError {
        private String status;
        private String message;

        public ApiError(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}