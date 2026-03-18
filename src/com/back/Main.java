package com.app;
import com.app.util.Connexion;
import com.app.models.Hotel;
import com.app.planification.PlanificationService;
import com.app.planification.PlanificationResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.lang.reflect.*;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        // Test de connexion à la base de données
        System.out.println("=== Test de Connexion PostgreSQL ===");
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String username = "postgres";
        String password = "postgres"; // À adapter si un mot de passe est défini
        
        Connexion connexion = new Connexion(url, username, password);
        connexion.connect();
        
        if (connexion.getConnection() != null) {
            System.out.println("Connexion réussie à la base 'gestion_ticket'");

            // Test : récupérer et afficher les hotels
            List<Hotel> hotels = connexion.getHotels();
            System.out.println("Hotels trouvés : " + hotels.size());
            for (Hotel h : hotels) {
                System.out.println(" - id=" + h.getIdHotel() + " nom=" + h.getNom());
            }

            connexion.disconnect();
        } else {
            System.out.println("Échec de la connexion");
        }
        
        // Test de la planification automatique pour Sprint 5
        System.out.println("\n=== Test Planification Sprint 5 - Assignation automatique ===");
        testPlanificationSprint5();
        
        // Scan des contrôleurs annotés avec @Controller
        // logger.info("Scan des contrôleurs avec le framework...");
        // ClassScanner scanner = new ClassScanner("com.app");
        // Set<Class<?>> controllers = scanner.getClassesAnnotatedWith();
        // logger.info("Contrôleurs trouvés : " + controllers.size());
    }

    /**
     * Test de la planification pour Sprint 5 avec assignation automatique
     */
    private static void testPlanificationSprint5() {
        try {
            // Date de test : 2026-03-20 (avec les données de test)
            LocalDate testDate = LocalDate.of(2026, 3, 20);
            
            System.out.println("Test de planification pour la date : " + testDate);
            
            // Créer le service de planification
            PlanificationService planificationService = new PlanificationService();
            
            // Exécuter la planification
            PlanificationResult result = planificationService.planifierJour(testDate);
            
            // Afficher les résultats
            System.out.println("Résultats de la planification :");
            System.out.println(" - Véhicules utilisés : " + result.getNombreVehiculesUtilises());
            System.out.println(" - Réservations assignées : " + result.getNombreReservationsAssignees());
            System.out.println(" - Réservations non assignées : " + result.getNombreReservationsNonAssignees());
            System.out.println(" - Total personnes assignées : " + result.getTotalPersonnesAssignees());
            
            // Détails des véhicules assignés
            System.out.println("\nDétails par véhicule :");
            result.getVehiculesAssignes().forEach(plan -> {
                System.out.println("Véhicule " + plan.getVehicule().getIdVehicule() + " (" + plan.getVehicule().getReference() + ") :");
                System.out.println("  - Réservations : " + plan.getReservations().size());
                System.out.println("  - Total personnes : " + plan.getTotalPersonnes());
                System.out.println("  - Heure départ : " + plan.getHeureDepart());
                System.out.println("  - Distance totale : " + plan.getDistanceTotaleTousVoyages() + " km");
            });
            
            // Réservations non assignées
            if (!result.getReservationsNonAssignees().isEmpty()) {
                System.out.println("\nRéservations non assignées :");
                result.getReservationsNonAssignees().forEach(r -> {
                    System.out.println("  - " + r.getClientId() + " (" + r.getDateHeureArrivee() + ", " + r.getNbrPers() + " pers)");
                });
            }
            
            System.out.println("\nTest terminé avec succès !");
            
        } catch (Exception e) {
            System.err.println("Erreur lors du test de planification : " + e.getMessage());
            e.printStackTrace();
        }
    }
}