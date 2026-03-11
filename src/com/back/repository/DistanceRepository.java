package com.app.repository;

import com.app.models.Distance;
import com.app.util.Connexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Repository pour la gestion des distances entre hôtels.
 */
public class DistanceRepository {

    private static final Logger logger = Logger.getLogger(DistanceRepository.class.getName());
    private Connexion connexion;

    public DistanceRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    /**
     * Récupère la distance en km entre deux hôtels.
     * Cherche dans les deux sens (from->to et to->from).
     * 
     * @param fromHotelId ID de l'hôtel de départ
     * @param toHotelId ID de l'hôtel d'arrivée
     * @return Distance en km, ou 0 si non trouvée
     */
    public int getDistance(int fromHotelId, int toHotelId) {
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return 0;
        }

        // Chercher dans les deux sens car la distance est bidirectionnelle
        String sql = "SELECT distance_km FROM Distance " +
                     "WHERE (from_hotel_id = ? AND to_hotel_id = ?) " +
                     "   OR (from_hotel_id = ? AND to_hotel_id = ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fromHotelId);
            ps.setInt(2, toHotelId);
            ps.setInt(3, toHotelId);
            ps.setInt(4, fromHotelId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("distance_km");
                }
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors de la récupération de la distance entre " + 
                         fromHotelId + " et " + toHotelId + " : " + e.getMessage());
        }
        
        logger.warning("Distance non trouvée entre hôtels " + fromHotelId + " et " + toHotelId);
        return 0;
    }

    /**
     * Récupère toutes les distances.
     * 
     * @return Liste de toutes les distances
     */
    public List<Distance> findAll() {
        List<Distance> distances = new ArrayList<>();
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return distances;
        }

        String sql = "SELECT idDistance, fromHotelId, toHotelId, distance_km FROM Distance";
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Distance distance = new Distance(
                    rs.getInt("idDistance"),
                    rs.getInt("fromHotelId"),
                    rs.getInt("toHotelId"),
                    rs.getInt("distance_km")
                );
                distances.add(distance);
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors de la récupération de toutes les distances : " + e.getMessage());
        }
        
        return distances;
    }
}
