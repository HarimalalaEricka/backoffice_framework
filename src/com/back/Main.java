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

        // Test SPRINT 8 - TACHE 1 : Priorisation des réservations non assignées
        System.out.println("\n=== Test SPRINT 8 - TACHE 1 : Priorisation des non assignées ===");
        testSprint8Tache1();
        
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

    /**
     * Test SPRINT 8 - TACHE 1 : Priorisation des réservations non assignées
     * dans le prochain groupe d'intervalle
     */
    private static void testSprint8Tache1() {
        try {
            // Date de test : 2026-04-01 (avec les données de test SPRINT 8)
            LocalDate testDate = LocalDate.of(2026, 4, 1);

            System.out.println("🚀 Test SPRINT 8 - TACHE 1 pour la date : " + testDate);
            System.out.println("Scénario : Priorisation des non assignées (06:00, 06:45, 06:50)");
            System.out.println("         + Groupe suivant (08:00, 08:10, 08:15)");
            System.out.println("Ordre attendu : Non assignées → Groupe 08:00-08:30\n");

            // Créer le service de planification
            PlanificationService planificationService = new PlanificationService();

            // VÉRIFICATION AVANT : État initial des réservations
            System.out.println("📋 ÉTAT AVANT PLANIFICATION :");
            afficherEtatReservations(testDate);

            // EXÉCUTER LA PLANIFICATION
            System.out.println("\n⚙️  EXÉCUTION DE LA PLANIFICATION...");
            PlanificationResult result = planificationService.planifierJour(testDate);

            // RÉSULTATS APRÈS PLANIFICATION
            System.out.println("\n📊 RÉSULTATS APRÈS PLANIFICATION :");
            System.out.println("✅ Véhicules utilisés : " + result.getNombreVehiculesUtilises());
            System.out.println("✅ Réservations assignées : " + result.getNombreReservationsAssignees());
            System.out.println("✅ Réservations non assignées : " + result.getNombreReservationsNonAssignees());
            System.out.println("✅ Total personnes assignées : " + result.getTotalPersonnesAssignees());

            // VÉRIFICATION DE LA PRIORISATION
            System.out.println("\n🎯 VÉRIFICATION DE LA PRIORISATION SPRINT 8 - TACHE 1 :");
            afficherOrdreAssignation(testDate);

            // ANALYSE DES RÉSULTATS
            analyserResultatsPriorisation(testDate);

            System.out.println("\n🎉 Test SPRINT 8 - TACHE 1 terminé !");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du test SPRINT 8 - TACHE 1 : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Affiche l'état des réservations avant/après planification
     */
    private static void afficherEtatReservations(LocalDate date) {
        try {
            Connexion connexion = new Connexion("jdbc:postgresql://localhost:5432/gestion_ticket", "postgres", "postgres");
            connexion.connect();

            String query = """
                SELECT r.idReservation, r.client_id, r.nbr_pers, r.date_heure_arrivee,
                       CASE WHEN a.reservation_id IS NULL THEN '🔴 NON ASSIGNÉE' ELSE '🟢 ASSIGNÉE' END as statut,
                       COALESCE(a.vehicule_id, 0) as vehicule_id
                FROM reservation r
                LEFT JOIN assignation a ON r.idReservation = a.reservation_id
                WHERE DATE(r.date_heure_arrivee) = ?
                ORDER BY r.date_heure_arrivee ASC
                """;

            var ps = connexion.getConnection().prepareStatement(query);
            ps.setDate(1, java.sql.Date.valueOf(date));
            var rs = ps.executeQuery();

            System.out.println("ID  | Client       | Pers | Heure arrivée    | Statut          | Véhicule");
            System.out.println("----|---------------|------|-----------------|-----------------|----------");

            while (rs.next()) {
                System.out.printf("%-3d | %-12s | %-4d | %-15s | %-15s | %-8d%n",
                    rs.getInt("idReservation"),
                    rs.getString("client_id"),
                    rs.getInt("nbr_pers"),
                    rs.getTimestamp("date_heure_arrivee").toLocalDateTime().toString().substring(11, 16),
                    rs.getString("statut"),
                    rs.getInt("vehicule_id")
                );
            }

            connexion.disconnect();
        } catch (Exception e) {
            System.err.println("Erreur affichage état : " + e.getMessage());
        }
    }

    /**
     * Affiche l'ordre d'assignation pour vérifier la priorisation
     */
    private static void afficherOrdreAssignation(LocalDate date) {
        try {
            Connexion connexion = new Connexion("jdbc:postgresql://localhost:5432/gestion_ticket", "postgres", "postgres");
            connexion.connect();

            String query = """
                SELECT r.date_heure_arrivee, r.client_id, r.nbr_pers,
                       a.vehicule_id, a.nb_pers_assigne, a.date_heure_planification,
                       CASE WHEN r.date_heure_arrivee < '08:00:00' THEN '⭐ PRIORITÉ (Non assignée)' ELSE '📅 Groupe 08:00-08:30' END as categorie
                FROM reservation r
                LEFT JOIN assignation a ON r.idReservation = a.reservation_id
                WHERE DATE(r.date_heure_arrivee) = ?
                ORDER BY a.date_heure_planification ASC, r.date_heure_arrivee ASC
                """;

            var ps = connexion.getConnection().prepareStatement(query);
            ps.setDate(1, java.sql.Date.valueOf(date));
            var rs = ps.executeQuery();

            System.out.println("Heure | Client       | Pers | Catégorie              | Véhicule | Assignés");
            System.out.println("------|---------------|------|-----------------------|----------|----------");

            while (rs.next()) {
                String heure = rs.getTimestamp("date_heure_arrivee") != null ?
                    rs.getTimestamp("date_heure_arrivee").toLocalDateTime().toString().substring(11, 16) : "--:--";
                String categorie = rs.getString("categorie");
                int vehiculeId = rs.getInt("vehicule_id");

                System.out.printf("%-5s | %-12s | %-4d | %-21s | %-8d | %-8d%n",
                    heure,
                    rs.getString("client_id"),
                    rs.getInt("nbr_pers"),
                    categorie,
                    vehiculeId > 0 ? vehiculeId : 0,
                    rs.getInt("nb_pers_assigne")
                );
            }

            connexion.disconnect();
        } catch (Exception e) {
            System.err.println("Erreur affichage ordre : " + e.getMessage());
        }
    }

    /**
     * Analyse les résultats pour vérifier que la priorisation fonctionne
     */
    private static void analyserResultatsPriorisation(LocalDate date) {
        try {
            Connexion connexion = new Connexion("jdbc:postgresql://localhost:5432/gestion_ticket", "postgres", "postgres");
            connexion.connect();

            // Vérifier que les non assignées ont été traitées avant le groupe 08:00
            String queryAnalyse = """
                SELECT
                    CASE WHEN r.date_heure_arrivee < ? THEN 'PRIORITÉ (Non assignées)' ELSE 'Groupe 08:00-08:30' END as categorie,
                    COUNT(*) as total_reservations,
                    SUM(CASE WHEN a.reservation_id IS NOT NULL THEN 1 ELSE 0 END) as assignees,
                    MIN(a.date_heure_planification) as premiere_assignation,
                    MAX(a.date_heure_planification) as derniere_assignation
                FROM reservation r
                LEFT JOIN assignation a ON r.idReservation = a.reservation_id
                WHERE DATE(r.date_heure_arrivee) = ?
                GROUP BY CASE WHEN r.date_heure_arrivee < ? THEN 'PRIORITÉ (Non assignées)' ELSE 'Groupe 08:00-08:30' END
                ORDER BY premiere_assignation ASC
                """;

            var ps = connexion.getConnection().prepareStatement(queryAnalyse);
            ps.setTime(1, java.sql.Time.valueOf("08:00:00"));
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setTime(3, java.sql.Time.valueOf("08:00:00"));
            var rs = ps.executeQuery();

            System.out.println("\n📈 ANALYSE DE LA PRIORISATION :");
            boolean prioriteRespectee = true;

            while (rs.next()) {
                String categorie = rs.getString("categorie");
                int total = rs.getInt("total_reservations");
                int assignees = rs.getInt("assignees");

                System.out.printf("📂 %s : %d/%d assignées%n", categorie, assignees, total);

                if (categorie.contains("PRIORITÉ") && assignees < total) {
                    System.out.println("⚠️  ATTENTION : Toutes les réservations prioritaires n'ont pas été assignées !");
                    prioriteRespectee = false;
                }
            }

            if (prioriteRespectee) {
                System.out.println("✅ PRIORISATION RESPECTÉE : Les non assignées ont été traitées en priorité !");
            } else {
                System.out.println("❌ PROBLÈME : La priorisation n'a pas été respectée !");
            }

            connexion.disconnect();
        } catch (Exception e) {
            System.err.println("Erreur analyse : " + e.getMessage());
        }
    }
}