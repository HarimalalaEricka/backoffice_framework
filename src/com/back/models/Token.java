package com.app.models;

import java.time.LocalDateTime;

public class Token {
    private int idToken;
    private String token;
    private LocalDateTime dateHeureExpiration;

    // Constructors
    public Token() {}

    public Token(int idToken, String token, LocalDateTime dateHeureExpiration) {
        this.idToken = idToken;
        this.token = token;
        this.dateHeureExpiration = dateHeureExpiration;
    }

    // Getters and Setters
    public int getIdToken() {
        return idToken;
    }

    public void setIdToken(int idToken) {
        this.idToken = idToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getDateHeureExpiration() {
        return dateHeureExpiration;
    }

    public void setDateHeureExpiration(LocalDateTime dateHeureExpiration) {
        this.dateHeureExpiration = dateHeureExpiration;
    }
}