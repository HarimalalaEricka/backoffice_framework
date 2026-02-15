package com.app.controllers;

import com.framework.annotation.*;
import com.app.util.Connexion;
import com.app.models.Reservation;

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
      
    // API : récupérer les réservations depuis la base et renvoyer en JSON, avec filtrage optionnel par date
    @HandleGet("/api/reservations")
    @JsonResponse
    public List<Map<String, Object>> apiReservations(@RequestParam(value = "date", required = false) String date) {
        List<Map<String, Object>> reservations = new ArrayList<>();

        connexion.connect();
        Connection conn = connexion.getConnection();
        if (conn == null) {
            return reservations;
        }

        // Updated SQL: Join with hotel table to include hotel name
        String sql = "SELECT r.id, r.client_id, r.date_heure, r.nbr_pers, r.hotel_id, h.nom AS hotel_nom FROM Reservation r JOIN hotel h ON r.hotel_id = h.id";
        if (date != null && !date.isEmpty()) {
            sql += " WHERE DATE(r.date_heure) = ?";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (date != null && !date.isEmpty()) {
                ps.setDate(1, java.sql.Date.valueOf(date));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("id", rs.getInt("id"));
                    res.put("client_id", rs.getString("client_id"));

                    Timestamp ts = rs.getTimestamp("date_heure");
                    LocalDateTime dateHeure = ts != null ? ts.toLocalDateTime() : null;
                    res.put("date_heure", dateHeure);

                    res.put("nbr_pers", rs.getInt("nbr_pers"));
                    res.put("hotel_id", rs.getInt("hotel_id"));
                    // Add hotel name to the response
                    res.put("hotel_nom", rs.getString("hotel_nom"));

                    reservations.add(res);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération réservations : " + e.getMessage());
        } finally {
            connexion.disconnect();
        }

        return reservations;
    }
}