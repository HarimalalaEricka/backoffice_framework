package com.app.repository;

import com.app.models.Hotel;
import com.app.util.Connexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class HotelRepository {

    private Connexion connexion;

    public HotelRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    public List<Hotel> findAllHotels() {
        List<Hotel> hotels = new ArrayList<>();
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return hotels;
        }

        String sql = "SELECT idHotel, nom, code, libelle FROM Hotel";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int idHotel = rs.getInt("idHotel");
                String nom = rs.getString("nom");
                String code = rs.getString("code");
                String libelle = rs.getString("libelle");
                hotels.add(new Hotel(idHotel, nom, code, libelle));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des hôtels : " + e.getMessage());
        }
        return hotels;
    }

    /**
     * Récupère l'aéroport (hôtel avec libelle = 'aeroport').
     * 
     * @return Hotel représentant l'aéroport, ou null si non trouvé
     */
    public Hotel findAeroport() {
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return null;
        }

        String sql = "SELECT idHotel, nom, code, libelle FROM Hotel WHERE LOWER(libelle) = 'aeroport' LIMIT 1";
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return new Hotel(
                    rs.getInt("idHotel"),
                    rs.getString("nom"),
                    rs.getString("code"),
                    rs.getString("libelle")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'aéroport : " + e.getMessage());
        }
        
        System.err.println("Aucun aéroport trouvé (libelle='aeroport')");
        return null;
    }
}
