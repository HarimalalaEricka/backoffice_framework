package com.app.repository;

import com.app.models.Vehicule;
import com.app.util.Connexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VehiculeRepository {
    private Connexion connexion;

    public VehiculeRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    /**
     * Insère un nouveau véhicule en base de données
     */
    public void insertVehicule(Vehicule v) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return;
        }

        String sql = "INSERT INTO Vehicule (reference, nbr_places, type_carburant) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getReference());
            ps.setInt(2, v.getNbrPlaces());
            ps.setString(3, v.getTypeCarburant());
            ps.executeUpdate();
            System.out.println("Véhicule inséré avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion du véhicule : " + e.getMessage());
        }
    }

    /**
     * Récupère tous les véhicules
     */
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
                int id = rs.getInt("idVehicule");
                String reference = rs.getString("reference");
                int nbrPlaces = rs.getInt("nbr_places");
                String typeCarburant = rs.getString("type_carburant");
                
                vehicules.add(new Vehicule(id, reference, nbrPlaces, typeCarburant));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des véhicules : " + e.getMessage());
        }
        return vehicules;
    }

    /**
     * Supprime un véhicule par son ID
     */
    public void deleteVehicule(int idVehicule) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return;
        }

        String sql = "DELETE FROM Vehicule WHERE idVehicule = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVehicule);
            ps.executeUpdate();
            System.out.println("Véhicule supprimé avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du véhicule : " + e.getMessage());
        }
    }

    /**
     * Récupère un véhicule par son ID
     */
    public Vehicule findById(int idVehicule) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return null;
        }

        String sql = "SELECT idVehicule, reference, nbr_places, type_carburant FROM Vehicule WHERE idVehicule = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVehicule);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Vehicule(rs.getInt("idVehicule"), rs.getString("reference"), 
                                   rs.getInt("nbr_places"), rs.getString("type_carburant"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du véhicule : " + e.getMessage());
        }
        return null;
    }

    /**
     * Modifie un véhicule par son ID
     */
    public void updateVehicule(Vehicule v) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return;
        }

        String sql = "UPDATE Vehicule SET reference = ?, nbr_places = ?, type_carburant = ? WHERE idVehicule = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getReference());
            ps.setInt(2, v.getNbrPlaces());
            ps.setString(3, v.getTypeCarburant());
            ps.setInt(4, v.getIdVehicule());
            ps.executeUpdate();
            System.out.println("Véhicule modifié avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du véhicule : " + e.getMessage());
        }
    }
}