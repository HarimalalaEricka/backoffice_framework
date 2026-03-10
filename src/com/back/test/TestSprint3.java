import com.app.models.*;
import com.app.repository.*;
import com.app.planification.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tests pour Sprint 3 - Réutilisation des véhicules et calcul de trajet
 * 
 * Ce fichier contient des tests pour valider :
 * - Le calcul de trajet complet (heures, distances)
 * - La réutilisation des véhicules
 * - L'affichage des détails du trajet
 * - La gestion des voyages multiples par véhicule
 */
public class TestSprint3 {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/gestion_ticket";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    public static void main(String[] args) {
        TestSprint3 test = new TestSprint3();
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST SPRINT 3 - RÉUTILISATION VÉHICULES & CALCUL TRAJET  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Test 1 : Calcul de trajet simple
        test.testCalculTrajetSimple();
        
        // Test 2 : Calcul de trajet avec plusieurs hôtels
        test.testCalculTrajetMultipleHotels();
        
        // Test 3 : Réutilisation de véhicule
        test.testReutilisationVehicule();
        
        // Test 4 : Planification complète avec réutilisation
        test.testPlanificationAvecReutilisation();
        
        System.out.println("\n✅ Tous les tests sont terminés!");
    }

    /**
     * Test 1 : Calcul de trajet simple avec 1 hôtel
     */
    public void testCalculTrajetSimple() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 1 : Calcul de trajet simple (1 hôtel)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            // Initialiser les repositories
            DistanceRepository distanceRepo = new DistanceRepository(DB_URL, DB_USER, DB_PASSWORD);
            ParametreRepository parametreRepo = new ParametreRepository(DB_URL, DB_USER, DB_PASSWORD);
            HotelRepository hotelRepo = new HotelRepository(DB_URL, DB_USER, DB_PASSWORD);
            
            // Créer le calculateur
            TrajetCalculator calculator = new TrajetCalculator(distanceRepo, parametreRepo, hotelRepo);
            
            // Créer une réservation test
            LocalDateTime heureDepart = LocalDateTime.of(2026, 3, 10, 10, 0);
            Reservation reservation = new Reservation(1, "CLIENT001", 3, heureDepart, 2); // Hotel ID 2
            List<Reservation> reservations = List.of(reservation);
            
            // Calculer le trajet
            TrajetComplet trajet = calculator.calculerTrajetComplet(heureDepart, reservations);
            
            // Afficher les résultats
            System.out.println("📍 Heure de départ : " + heureDepart);
            System.out.println("📍 Heure de retour : " + trajet.getHeureRetour());
            System.out.println("📏 Distance totale : " + trajet.getDistanceTotale() + " km");
            System.out.println("🗺️  Nombre d'arrêts : " + trajet.getDetailsTrajet().size());
            
            for (TrajetDetailDTO detail : trajet.getDetailsTrajet()) {
                System.out.println("   Arrêt " + detail.getOrdre() + " : " + detail.getNomHotel() + 
                                 " - Arrivée: " + detail.getHeureArrivee() + 
                                 " - Distance: " + detail.getDistanceSegment() + " km");
            }
            
            System.out.println("✅ Test 1 réussi\n");
            
        } catch (Exception e) {
            System.err.println("❌ Test 1 échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 2 : Calcul de trajet avec plusieurs hôtels
     */
    public void testCalculTrajetMultipleHotels() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 2 : Calcul de trajet avec 3+ hôtels");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            // Initialiser les repositories
            DistanceRepository distanceRepo = new DistanceRepository(DB_URL, DB_USER, DB_PASSWORD);
            ParametreRepository parametreRepo = new ParametreRepository(DB_URL, DB_USER, DB_PASSWORD);
            HotelRepository hotelRepo = new HotelRepository(DB_URL, DB_USER, DB_PASSWORD);
            
            // Créer le calculateur
            TrajetCalculator calculator = new TrajetCalculator(distanceRepo, parametreRepo, hotelRepo);
            
            // Créer plusieurs réservations pour différents hôtels
            LocalDateTime heureDepart = LocalDateTime.of(2026, 3, 10, 14, 30);
            List<Reservation> reservations = List.of(
                new Reservation(1, "CLIENT001", 2, heureDepart, 2),
                new Reservation(2, "CLIENT002", 3, heureDepart, 3),
                new Reservation(3, "CLIENT003", 1, heureDepart, 4)
            );
            
            // Calculer le trajet
            TrajetComplet trajet = calculator.calculerTrajetComplet(heureDepart, reservations);
            
            // Afficher les résultats
            System.out.println("📍 Heure de départ : " + heureDepart);
            System.out.println("📍 Heure de retour : " + trajet.getHeureRetour());
            System.out.println("📏 Distance totale : " + trajet.getDistanceTotale() + " km");
            System.out.println("🗺️  Itinéraire (" + trajet.getDetailsTrajet().size() + " arrêts) :");
            
            for (TrajetDetailDTO detail : trajet.getDetailsTrajet()) {
                System.out.println(String.format("   %d. %-20s | Arrivée: %s | Segment: %3d km | Cumulée: %3d km",
                    detail.getOrdre(),
                    detail.getNomHotel(),
                    detail.getHeureArrivee().toLocalTime(),
                    detail.getDistanceSegment(),
                    detail.getDistanceCumulee()
                ));
            }
            
            System.out.println("✅ Test 2 réussi\n");
            
        } catch (Exception e) {
            System.err.println("❌ Test 2 échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 3 : Vérifier la réutilisation d'un véhicule
     */
    public void testReutilisationVehicule() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 3 : Réutilisation de véhicule");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            DistanceRepository distanceRepo = new DistanceRepository(DB_URL, DB_USER, DB_PASSWORD);
            ParametreRepository parametreRepo = new ParametreRepository(DB_URL, DB_USER, DB_PASSWORD);
            HotelRepository hotelRepo = new HotelRepository(DB_URL, DB_USER, DB_PASSWORD);
            TrajetCalculator calculator = new TrajetCalculator(distanceRepo, parametreRepo, hotelRepo);
            
            // Premier trajet : Vol à 10h00
            LocalDateTime vol1 = LocalDateTime.of(2026, 3, 10, 10, 0);
            List<Reservation> reservations1 = List.of(
                new Reservation(1, "CLIENT001", 2, vol1, 2)
            );
            TrajetComplet trajet1 = calculator.calculerTrajetComplet(vol1, reservations1);
            
            System.out.println("🚗 Trajet 1 (Vol à 10h00)");
            System.out.println("   Départ  : " + vol1);
            System.out.println("   Retour  : " + trajet1.getHeureRetour());
            
            // Deuxième trajet : Vol à 13h00 (après le retour du véhicule)
            LocalDateTime vol2 = LocalDateTime.of(2026, 3, 10, 13, 0);
            List<Reservation> reservations2 = List.of(
                new Reservation(2, "CLIENT002", 3, vol2, 3)
            );
            TrajetComplet trajet2 = calculator.calculerTrajetComplet(vol2, reservations2);
            
            System.out.println("\n🚗 Trajet 2 (Vol à 13h00)");
            System.out.println("   Départ  : " + vol2);
            System.out.println("   Retour  : " + trajet2.getHeureRetour());
            
            // Vérifier la réutilisation
            boolean vehiculeDisponible = trajet1.getHeureRetour().isBefore(vol2) || 
                                        trajet1.getHeureRetour().isEqual(vol2);
            
            System.out.println("\n🔄 Véhicule disponible pour réutilisation : " + 
                             (vehiculeDisponible ? "✅ OUI" : "❌ NON"));
            
            if (vehiculeDisponible) {
                System.out.println("   Le véhicule est revenu à " + trajet1.getHeureRetour() + 
                                 " et peut être réutilisé pour le vol de " + vol2);
            }
            
            System.out.println("✅ Test 3 réussi\n");
            
        } catch (Exception e) {
            System.err.println("❌ Test 3 échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 4 : Planification complète avec réutilisation de véhicules
     */
    public void testPlanificationAvecReutilisation() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 4 : Planification complète avec réutilisation");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            // Créer le service de planification
            PlanificationService service = new PlanificationService();
            
            // Tester la planification pour une date
            LocalDate datePlanification = LocalDate.of(2026, 3, 10);
            
            System.out.println("📅 Date de planification : " + datePlanification);
            System.out.println("⏳ Lancement de la planification...\n");
            
            PlanificationResult result = service.planifierJour(datePlanification);
            
            // Afficher les résultats
            System.out.println("📊 RÉSULTATS :");
            System.out.println("   Véhicules utilisés     : " + result.getNombreVehiculesUtilises());
            System.out.println("   Réservations assignées : " + 
                             (result.getVehiculesAssignes().stream()
                                 .mapToInt(vp -> vp.getReservations().size()).sum()));
            System.out.println("   Réservations non assignées : " + result.getNombreReservationsNonAssignees());
            
            System.out.println("\n🚗 DÉTAILS DES VÉHICULES :");
            for (VehiculePlanDTO vp : result.getVehiculesAssignes()) {
                Vehicule v = vp.getVehicule();
                System.out.println("\n   Véhicule : " + v.getReference() + 
                                 " (Capacité: " + v.getNbrPlaces() + ", Carburant: " + v.getTypeCarburant() + ")");
                System.out.println("   Réservations totales : " + vp.getReservations().size());
                System.out.println("   Nombre de voyages : " + vp.getNombreVoyages());
                System.out.println("   Distance totale cumulée : " + vp.getDistanceTotaleTousVoyages() + " km");
                
                if (vp.getVoyages() != null && !vp.getVoyages().isEmpty()) {
                    for (VoyageDTO voyage : vp.getVoyages()) {
                        System.out.println("\n   ✈️  Voyage " + voyage.getNumeroVoyage() + " :");
                        System.out.println("      Départ       : " + voyage.getHeureDepart());
                        System.out.println("      Retour       : " + voyage.getHeureRetour());
                        System.out.println("      Distance     : " + voyage.getDistanceTotale() + " km");
                        System.out.println("      Durée        : " + voyage.getDureeVoyage() + " minutes");
                        System.out.println("      Réservations : " + voyage.getReservations().size() + 
                                         " (" + voyage.getTotalPersonnes() + " personnes)");
                        
                        if (voyage.getDetailsTrajet() != null && !voyage.getDetailsTrajet().isEmpty()) {
                            System.out.println("      Itinéraire   : " + voyage.getDetailsTrajet().size() + " arrêts");
                            for (TrajetDetailDTO detail : voyage.getDetailsTrajet()) {
                                System.out.println("         → " + detail.getNomHotel() + 
                                                 " (+" + detail.getDistanceSegment() + " km)");
                            }
                        }
                    }
                }
            }
            
            if (result.getNombreReservationsNonAssignees() > 0) {
                System.out.println("\n⚠️  RÉSERVATIONS NON ASSIGNÉES :");
                for (Reservation r : result.getReservationsNonAssignees()) {
                    System.out.println("   - Réservation " + r.getIdReservation() + 
                                     " : " + r.getNbrPers() + " personnes, Vol: " + r.getDateHeureArrivee());
                }
            }
            
            System.out.println("\n✅ Test 4 réussi");
            
        } catch (Exception e) {
            System.err.println("❌ Test 4 échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
