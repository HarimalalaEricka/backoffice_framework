package com.app.repository;

import com.app.models.Reservation;
import com.app.util.Connexion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class ReservationRepository {

    private Connexion connexion;

    public ReservationRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    public void insertReservation(Reservation r) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return;
        }

        String sql = "INSERT INTO Reservation (client_id, nbr_pers, date_heure, hotel_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getClientId());
            ps.setInt(2, r.getNbrPers());
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(r.getDateHeureArrivee()));
            ps.setInt(4, r.getHotelId());
            ps.executeUpdate();
            System.out.println("Réservation insérée avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion de la réservation : " + e.getMessage());
        }
    }

    public List<Reservation> findAll() {
        List<Reservation> reservations = new ArrayList<>();
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return reservations;
        }

        String sql = "SELECT id, client_id, nbr_pers, date_heure, hotel_id FROM Reservation";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int idReservation = rs.getInt("id");
                String clientId = rs.getString("client_id");
                int nbrPers = rs.getInt("nbr_pers");
                java.time.LocalDateTime dateHeureArrivee = rs.getTimestamp("date_heure").toLocalDateTime();
                int hotelId = rs.getInt("hotel_id");
                reservations.add(new Reservation(idReservation, clientId, nbrPers, dateHeureArrivee, hotelId));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des réservations : " + e.getMessage());
        }
        return reservations;
    }

    public List<Reservation> findByDate(LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return reservations;
        }

        // Modifié : ajout du tri par date_heure_arrivee ASC, puis nbr_pers DESC
        String sql = "SELECT idReservation, client_id, nbr_pers, date_heure_arrivee, hotel_id " +
                     "FROM Reservation WHERE DATE(date_heure_arrivee) = ? " +
                     "ORDER BY date_heure_arrivee ASC, nbr_pers DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idReservation = rs.getInt("idReservation");
                    String clientId = rs.getString("client_id");
                    int nbrPers = rs.getInt("nbr_pers");
                    java.time.LocalDateTime dateHeureArrivee = rs.getTimestamp("date_heure_arrivee").toLocalDateTime();
                    int hotelId = rs.getInt("hotel_id");
                    reservations.add(new Reservation(idReservation, clientId, nbrPers, dateHeureArrivee, hotelId));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par date : " + e.getMessage());
        }
        return reservations;
    }
}
