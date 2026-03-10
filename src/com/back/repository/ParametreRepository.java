package com.app.repository;

import com.app.models.Parametre;
import com.app.util.Connexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Repository pour la gestion des paramètres de planification.
 */
public class ParametreRepository {

    private static final Logger logger = Logger.getLogger(ParametreRepository.class.getName());
    private Connexion connexion;

    public ParametreRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    /**
     * Récupère les paramètres de planification (vitesse moyenne et temps d'attente).
     * On suppose qu'il n'y a qu'un seul enregistrement dans la table Parametre.
     * 
     * @return Parametre contenant vitesse_moyenne et temps_attente, ou null si non trouvé
     */
    public Parametre getParametre() {
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return null;
        }

        String sql = "SELECT idParametre, vitesse_moyenne, temps_attente FROM Parametre LIMIT 1";
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return new Parametre(
                    rs.getInt("idParametre"),
                    rs.getInt("vitesse_moyenne"),
                    rs.getInt("temps_attente")
                );
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors de la récupération des paramètres : " + e.getMessage());
        }
        
        logger.warning("Aucun paramètre trouvé dans la table Parametre");
        return null;
    }
}
