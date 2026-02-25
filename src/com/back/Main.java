package com.app;
import com.app.util.Connexion;
import com.app.models.Hotel;
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
        
        // Scan des contrôleurs annotés avec @Controller
        // logger.info("Scan des contrôleurs avec le framework...");
        // ClassScanner scanner = new ClassScanner("com.back");
        // Set<Class<?>> controllers = scanner.getClassesAnnotatedWith();
        // logger.info("Contrôleurs trouvés : " + controllers.size());
    }
}