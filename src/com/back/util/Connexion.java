package com.back.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import com.back.models.Hotel;

public class Connexion {

    private String url;
    private String username;
    private String password;
    private Connection connection;

    public Connexion(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public void connect() {
        try {
            // Driver PostgreSQL
            Class.forName("org.postgresql.Driver");

            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connexion PostgreSQL réussie !");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver PostgreSQL non trouvé : " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erreur de connexion PostgreSQL : " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Déconnexion réussie !");
            } catch (SQLException e) {
                System.err.println("Erreur lors de la déconnexion : " + e.getMessage());
            }
        }
    }

    // Nouvelle méthode : récupérer tous les hotels
    public List<Hotel> getHotels() {
        List<Hotel> hotels = new ArrayList<>();
        if (connection == null) {
            connect();
        }
        if (connection == null) {
            return hotels;
        }

        String sql = "SELECT id, nom FROM Hotel";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                hotels.add(new Hotel(id, nom));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des hotels : " + e.getMessage());
        }
        return hotels;
    }
}
