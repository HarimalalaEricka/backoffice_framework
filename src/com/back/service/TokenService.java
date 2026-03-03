package com.app.service;

import java.util.UUID;

public class TokenService {

    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public java.time.LocalDateTime calculateExpiration(int duree, String unite) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if ("heures".equals(unite)) {
            return now.plusHours(duree);
        } else if ("jours".equals(unite)) {
            return now.plusDays(duree);
        }
        throw new IllegalArgumentException("Unité non supportée : " + unite);
    }
}