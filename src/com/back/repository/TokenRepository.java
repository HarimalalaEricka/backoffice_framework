package com.app.repository;

import com.app.models.Token;
import com.app.util.Connexion;
import java.sql.*;

public class TokenRepository {
    private final Connexion connexion;

    public TokenRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    public void insertToken(Token t) {
        String sql = "INSERT INTO Token(token, date_heure_expiration) VALUES (?, ?)";
        try (PreparedStatement ps = connexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getToken());
            ps.setTimestamp(2, Timestamp.valueOf(t.getDateHeureExpiration()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur insertion token : " + e.getMessage());
        }
    }

    public Token findByToken(String token) {
        String sql = "SELECT idtoken, token, date_heure_expiration FROM Token WHERE token = ?";
        try (PreparedStatement ps = connexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Token(rs.getInt("idtoken"),
                                     rs.getString("token"),
                                     rs.getTimestamp("date_heure_expiration").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche token : " + e.getMessage());
        }
        return null;
    }

    /**
     * Indique si la connexion JDBC est établie
     */
    public boolean isConnected() {
        return this.connexion != null && this.connexion.getConnection() != null;
    }
}