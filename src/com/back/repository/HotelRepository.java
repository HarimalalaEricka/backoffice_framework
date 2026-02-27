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

        String sql = "SELECT id, nom FROM Hotel";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int idHotel = rs.getInt("id");
                String nom = rs.getString("nom");
                hotels.add(new Hotel(idHotel, nom));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des hôtels : " + e.getMessage());
        }
        return hotels;
    }
}
