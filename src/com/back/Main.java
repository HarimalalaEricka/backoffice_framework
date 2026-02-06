package com.back;
import com.back.util.Connexion;
import com.back.models.Hotel;
import java.util.List;
import java.util.Set;
import java.lang.reflect.*;

public class Main {
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
    }
}