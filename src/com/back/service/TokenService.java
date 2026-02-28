package com.app.service;

import java.util.UUID;

public class TokenService {

    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public java.time.LocalDateTime calculateExpiration(int hours) {
        return java.time.LocalDateTime.now().plusHours(hours);
    }
}