package com.app.service;

import java.util.UUID;
import com.app.repository.TokenRepository;

public class TokenService {
    private TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public TokenService() {}

    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public java.time.LocalDateTime calculateExpiration(int duree, String unite) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if ("heures".equals(unite)) {
            return now.plusHours(duree);
        } else if ("jours".equals(unite)) {
            return now.plusDays(duree);
        } else if( "minutes".equals(unite)) {
            return now.plusMinutes(duree);
        }   
        throw new IllegalArgumentException("Unité non supportée : " + unite);
    }

    /**
     * Valide le token et retourne un message d'erreur s'il est invalide ou expiré
     */
    public String validateToken(String token, TokenRepository repo) {
        if (token == null || token.trim().isEmpty()) {
            return "Token manquant.";
        }

        // Vérifier si le token est valide et non expiré
        if (!repo.isTokenValid(token)) {
            return repo.getTokenErrorMessage(token);
        }

        return null; // Token valide
    }
}