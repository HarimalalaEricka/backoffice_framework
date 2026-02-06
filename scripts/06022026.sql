CREATE DATABASE IF NOT EXISTS gestion_ticket;

\c gestion_ticket

CREATE TABLE Hotel (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL
);

-- Table des réservations principales
CREATE TABLE Reservation (
    id SERIAL PRIMARY KEY,
    client_id VARCHAR(50) NOT NULL,
    date_heure TIMESTAMP NOT NULL,
    nbr_pers INTEGER NOT NULL CHECK (nbr_pers > 0),
    hotel_id INTEGER NOT NULL REFERENCES Hotel(id) ON DELETE CASCADE
);

