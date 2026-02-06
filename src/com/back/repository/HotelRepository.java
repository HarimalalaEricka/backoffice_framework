package com.back.repository;

import com.back.models.Hotel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class HotelRepository {

    private final JdbcTemplate jdbc;

    public HotelRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Hotel> findAllHotels() {
        String sql = "SELECT * FROM Hotel";
        return jdbc.query(sql, (rs, rowNum) -> new Hotel(rs.getInt("id"), rs.getString("nom")));
    }
}
