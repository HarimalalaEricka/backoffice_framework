import com.app.models.Assignation;
import com.app.models.Reservation;
import com.app.planification.PlanificationResult;
import com.app.planification.PlanificationService;
import com.app.planification.VoyageDTO;
import com.app.repository.AssignationRepository;
import com.app.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Test Sprint 7 - ETU003350
 *
 * Objectifs:
 * - Vérifier le support DB/repository du split (multi-assignations)
 * - Vérifier le calcul des passagers restants (partially assigned)
 * - Vérifier le support DTO/JSP pour affichage passagers assignés par sous-partie
 *
 * Pré-requis:
 * 1) Exécuter scripts/19032026_ETU003350_sprint7_split.sql
 * 2) Exécuter scripts/test_sprint7_split.sql
 */
public class TestSprint7 {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/gestion_ticket";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    public static void main(String[] args) {
        TestSprint7 test = new TestSprint7();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║    TEST SPRINT 7 - SPLIT / PARTIAL ASSIGNMENT (DB+UI)     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        test.testRepositoryMultiAssignation();
        test.testReservationRepositoryPartiallyAssignedQuery();
        test.testVoyageDtoSplitDisplaySupport();
        test.testPlanificationScenarioSprint7();

        System.out.println("\n✅ Fin des tests Sprint 7");
    }

    /**
     * Vérifie qu'une réservation peut avoir plusieurs assignations,
     * et que la somme nb_pers_assigne est correcte.
     */
    public void testRepositoryMultiAssignation() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 1 : Repository multi-assignation + agrégats");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        AssignationRepository assignationRepository = new AssignationRepository(DB_URL, DB_USER, DB_PASSWORD);
        LocalDate date = LocalDate.of(2026, 3, 21);

        try {
            // Nettoyer les assignations de la date pour avoir un test reproductible
            assignationRepository.deleteByDate(date);

            // Réservation 701 (9 pers) -> split 6 + 3
            Assignation a1 = new Assignation();
            a1.setReservationId(701);
            a1.setVehiculeId(1);
            a1.setDateHeurePlanification(date.atStartOfDay());
            a1.setNbPersAssigne(6);
            assignationRepository.save(a1);

            Assignation a2 = new Assignation();
            a2.setReservationId(701);
            a2.setVehiculeId(2);
            a2.setDateHeurePlanification(date.atStartOfDay().plusMinutes(5));
            a2.setNbPersAssigne(3);
            assignationRepository.save(a2);

            // Réservation 704 (8 pers) -> partielle 5
            Assignation a3 = new Assignation();
            a3.setReservationId(704);
            a3.setVehiculeId(3);
            a3.setDateHeurePlanification(date.atStartOfDay().plusMinutes(10));
            a3.setNbPersAssigne(5);
            assignationRepository.save(a3);

            int count701 = assignationRepository.countByReservationId(701);
            int sum701 = assignationRepository.getTotalPassagersAssignesByReservationId(701);
            boolean complete701 = assignationRepository.isReservationCompletementAssignee(701, 9);

            int sum704 = assignationRepository.getTotalPassagersAssignesByReservationId(704);
            boolean hasRestants704 = assignationRepository.hasPassagersRestants(704, 8);

            assertTrue("701 a 2 assignations", count701 == 2);
            assertTrue("701 total assigné = 9", sum701 == 9);
            assertTrue("701 complètement assignée", complete701);
            assertTrue("704 total assigné = 5", sum704 == 5);
            assertTrue("704 partiellement assignée (reste > 0)", hasRestants704);

            System.out.println("✅ Test 1 réussi\n");
        } catch (Exception e) {
            System.err.println("❌ Test 1 échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Vérifie que la récupération "unassigned avant cutoff" inclut aussi
     * les réservations partiellement assignées (reste > 0).
     */
    public void testReservationRepositoryPartiallyAssignedQuery() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 2 : Query partiellement assignées avant cutoff");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        ReservationRepository reservationRepository = new ReservationRepository(DB_URL, DB_USER, DB_PASSWORD);
        LocalDate date = LocalDate.of(2026, 3, 21);
        LocalDateTime cutoff = LocalDateTime.of(2026, 3, 21, 9, 30);

        try {
            List<Reservation> restants = reservationRepository.findUnassignedByDateAndArrivalBefore(date, cutoff);

            Reservation r701 = restants.stream().filter(r -> r.getIdReservation() == 701).findFirst().orElse(null);
            Reservation r704 = restants.stream().filter(r -> r.getIdReservation() == 704).findFirst().orElse(null);

            // 701 est complète (9/9) -> ne doit plus apparaître
            assertTrue("701 n'apparaît plus dans les restants", r701 == null);

            // 704 est partielle (5/8) -> doit apparaître avec reste 3
            assertTrue("704 apparaît dans les restants", r704 != null);
            assertTrue("704 a 3 passagers restants", r704 != null && r704.getNbrPers() == 3);

            System.out.println("✅ Test 2 réussi\n");
        } catch (Exception e) {
            System.err.println("❌ Test 2 échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Vérifie le support DTO pour affichage split (passagers assignés par réservation).
     */
    public void testVoyageDtoSplitDisplaySupport() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 3 : DTO affichage split passagers");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            VoyageDTO voyage = new VoyageDTO();
            Reservation r1 = new Reservation(9001, "C-SPLIT", 10, LocalDateTime.of(2026, 3, 21, 9, 0), 2);
            Reservation r2 = new Reservation(9002, "C-FULL", 4, LocalDateTime.of(2026, 3, 21, 9, 10), 3);

            voyage.getReservations().add(r1);
            voyage.getReservations().add(r2);

            // r1 est split dans ce voyage: seulement 6 passagers ici
            voyage.setPassagersAssignesPourReservation(9001, 6);
            // r2 non renseignée -> fallback = nbr_pers

            int total = voyage.getTotalPersonnes();

            assertTrue("Passagers assignés r1 = 6", voyage.getPassagersAssignesPourReservation(r1) == 6);
            assertTrue("Passagers assignés r2 fallback = 4", voyage.getPassagersAssignesPourReservation(r2) == 4);
            assertTrue("Total voyage = 10", total == 10);

            System.out.println("✅ Test 3 réussi\n");
        } catch (Exception e) {
            System.err.println("❌ Test 3 échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test fonctionnel global Sprint 7 (selon l'état courant de l'algorithme de planification).
     */
    public void testPlanificationScenarioSprint7() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 4 : Planification fonctionnelle Sprint 7");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            PlanificationService service = new PlanificationService();
            LocalDate datePlanification = LocalDate.of(2026, 3, 21);

            PlanificationResult result = service.planifierJour(datePlanification);

            System.out.println("Véhicules utilisés : " + result.getNombreVehiculesUtilises());
            System.out.println("Réservations assignées : " + result.getNombreReservationsAssignees());
            System.out.println("Réservations non assignées : " + result.getNombreReservationsNonAssignees());
            System.out.println("Personnes transportées : " + result.getTotalPersonnesAssignees());

            // Assertion faible de non-régression: exécution sans exception + stats cohérentes
            assertTrue("Le résultat de planification est non nul", result != null);
            assertTrue("Nombre de véhicules >= 0", result.getNombreVehiculesUtilises() >= 0);
            assertTrue("Nombre de réservations non assignées >= 0", result.getNombreReservationsNonAssignees() >= 0);

            System.out.println("✅ Test 4 terminé (vérification fonctionnelle)\n");
        } catch (Exception e) {
            System.err.println("❌ Test 4 échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void assertTrue(String label, boolean condition) {
        if (condition) {
            System.out.println("  ✔ " + label);
        } else {
            throw new IllegalStateException("Assertion échouée: " + label);
        }
    }
}
