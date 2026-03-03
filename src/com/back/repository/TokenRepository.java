package com.app.repository;

import com.app.models.Token;
import com.app.util.Connexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class TokenRepository {

    private Connexion connexion;

    public TokenRepository(String url, String username, String password) {
        this.connexion = new Connexion(url, username, password);
        this.connexion.connect();
    }

    /**
     * Vérifier si un token existe et est valide (non expiré)
     */
    public Token findByTokenValue(String tokenValue) {
        Connection conn = connexion.getConnection();
        
        if (conn == null) {
            System.err.println("Connexion non établie");
            return null;
        }

        String sql = "SELECT idToken, token, date_heure_expiration FROM Token WHERE token = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenValue);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idToken = rs.getInt("idToken");
                    String token = rs.getString("token");
                    LocalDateTime dateHeureExpiration = rs.getTimestamp("date_heure_expiration").toLocalDateTime();
                    return new Token(idToken, token, dateHeureExpiration);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche du token : " + e.getMessage());
        }
        return null;
    }

    /**
     * Vérifier si le token est expiré
     */
    public boolean isTokenExpired(Token token) {
        if (token == null) {
            return true;
        }
        return token.getDateHeureExpiration().isBefore(LocalDateTime.now());
    }

    /**
     * Insérer un nouveau token
     */
    public void insertToken(Token token) {
        Connection conn = connexion.getConnection();
        if (conn == null) {
            System.err.println("Connexion non établie");
            return;
        }

        String sql = "INSERT INTO Token (token, date_heure_expiration) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token.getToken());
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(token.getDateHeureExpiration()));
            ps.executeUpdate();
            System.out.println("Token inséré avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion du token : " + e.getMessage());
        }
    }
}
