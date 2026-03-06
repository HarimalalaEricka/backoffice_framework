package com.app.repository;

import com.app.models.Assignation;
import com.app.util.Connexion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class AssignationRepository {

    private Connexion connexion;

    public AssignationRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    /**
     * Vérifie si une réservation est déjà assignée
     */
    public boolean existsByReservationId(int reservationId) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return false;
        }

        String sql = "SELECT COUNT(*) FROM Assignation WHERE reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de l'assignation : " + e.getMessage());
        }
        return false;
    }

    /**
     * Enregistre une nouvelle assignation
     */
    public void save(Assignation assignation) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return;
        }

        String sql = "INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assignation.getReservationId());
            ps.setInt(2, assignation.getVehiculeId());
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(assignation.getDateHeurePlanification()));
            ps.executeUpdate();
            System.out.println("Assignation enregistrée avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion de l'assignation : " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les assignations pour une date donnée
     */
    public List<Assignation> findByDate(LocalDate date) {
        List<Assignation> assignations = new ArrayList<>();
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return assignations;
        }

        String sql = "SELECT idAssignation, reservation_id, vehicule_id, date_heure_planification " +
                     "FROM Assignation WHERE DATE(date_heure_planification) = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idAssignation = rs.getInt("idAssignation");
                    int reservationId = rs.getInt("reservation_id");
                    int vehiculeId = rs.getInt("vehicule_id");
                    LocalDateTime dateHeurePlanification = rs.getTimestamp("date_heure_planification").toLocalDateTime();
                    assignations.add(new Assignation(idAssignation, reservationId, vehiculeId, dateHeurePlanification));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par date : " + e.getMessage());
        }
        return assignations;
    }

    /**
     * Récupère les IDs des véhicules déjà utilisés pour une date donnée
     */
    public List<Integer> findVehiculeIdsByDate(LocalDate date) {
        List<Integer> vehiculeIds = new ArrayList<>();
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return vehiculeIds;
        }

        String sql = "SELECT DISTINCT vehicule_id FROM Assignation WHERE DATE(date_heure_planification) = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vehiculeIds.add(rs.getInt("vehicule_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des véhicules utilisés : " + e.getMessage());
        }
        return vehiculeIds;
    }
}
