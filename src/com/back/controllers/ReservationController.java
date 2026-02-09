package com.back.controllers;

import com.framework.annotation.*;
import com.back.util.Connexion;
import com.back.models.Reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Controller
public class ReservationController {
    private Connexion connexion;

    public ReservationController() {
        // Initialiser la connexion à la base de données
        String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
        String username = "postgres";
        String password = "postgres"; // À adapter si un mot de passe est défini
        this.connexion = new Connexion(url, username, password);
    }
      
    // API : récupérer les réservations depuis la base et renvoyer en JSON
    @HandleGet("/api/reservations")
    @JsonResponse
    public List<Map<String, Object>> apiReservations() {
        List<Map<String, Object>> reservations = new ArrayList<>();

        connexion.connect();
        Connection conn = connexion.getConnection();
        if (conn == null) {
            return reservations;
        }

        String sql = "SELECT id, client_id, date_heure, nbr_pers, hotel_id FROM Reservation";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> res = new HashMap<>();
                res.put("id", rs.getInt("id"));
                res.put("client_id", rs.getString("client_id"));

                Timestamp ts = rs.getTimestamp("date_heure");
                LocalDateTime dateHeure = ts != null ? ts.toLocalDateTime() : null;
                res.put("date_heure", dateHeure);

                res.put("nbr_pers", rs.getInt("nbr_pers"));
                res.put("hotel_id", rs.getInt("hotel_id"));

                reservations.add(res);
            }

        } catch (SQLException e) {
            System.err.println("Erreur récupération réservations : " + e.getMessage());
        } finally {
            connexion.disconnect();
        }

        return reservations;
    }
}