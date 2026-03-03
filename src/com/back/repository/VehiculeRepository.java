package com.app.repository;

import com.app.models.Vehicule;
import com.app.util.Connexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class VehiculeRepository {

    private Connexion connexion;

    public VehiculeRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    public List<Vehicule> findAll() {
        List<Vehicule> vehicules = new ArrayList<>();
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return vehicules;
        }

        String sql = "SELECT idVehicule, reference, nbr_places, type_carburant FROM Vehicule";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int idVehicule = rs.getInt("idVehicule");
                String reference = rs.getString("reference");
                int nbrPlaces = rs.getInt("nbr_places");
                String typeCarburant = rs.getString("type_carburant");
                vehicules.add(new Vehicule(idVehicule, reference, nbrPlaces, typeCarburant));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des véhicules : " + e.getMessage());
        }
        return vehicules;
    }
}
