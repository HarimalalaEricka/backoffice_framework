package com.back.repository;

import com.back.models.Reservation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class ReservationRepository {

    private final JdbcTemplate jdbc;

    public ReservationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insertReservation(Reservation r) {
        String sql = "INSERT INTO reservation (client_id, nbr_pers, date_heure_arrivee, hotel_id) VALUES (?, ?, ?, ?)";
        jdbc.update(sql, r.getClientId(), r.getNbrPers(), r.getDateHeureArrivee(), r.getHotelId());
    }

    public List<Reservation> findAll() {
        String sql = "SELECT * FROM reservation";
        return jdbc.query(sql, (rs, rowNum) -> 
            new Reservation(rs.getInt("idReservation"), rs.getString("client_id"),
            rs.getInt("nbr_pers"), rs.getTimestamp("date_heure_arrivee").toLocalDateTime(),
            rs.getInt("hotel_id")));
    }

    public List<Reservation> findByDate(LocalDate date) {
        String sql = "SELECT * FROM reservation WHERE DATE(date_heure_arrivee) = ?";
        return jdbc.query(sql, new Object[]{Date.valueOf(date)}, (rs, rowNum) ->
            new Reservation(rs.getInt("idReservation"), rs.getString("client_id"),
            rs.getInt("nbr_pers"), rs.getTimestamp("date_heure_arrivee").toLocalDateTime(),
            rs.getInt("hotel_id")));
    }
}
