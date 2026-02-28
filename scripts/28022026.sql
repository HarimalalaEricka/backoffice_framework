DROP DATABASE IF EXISTS gestion_ticket;
CREATE DATABASE gestion_ticket;

\c gestion_ticket

CREATE TABLE Hotel (
    idHotel SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL
);

-- Table des réservations principales
CREATE TABLE Reservation (
    idReservation SERIAL PRIMARY KEY,
    client_id VARCHAR(50) NOT NULL,
    date_heure_arrivee TIMESTAMP NOT NULL,
    nbr_pers INTEGER NOT NULL CHECK (nbr_pers > 0),
    hotel_id INTEGER NOT NULL REFERENCES Hotel(idHotel) ON DELETE CASCADE
);

CREATE TABLE Vehicule (
    idVehicule SERIAL PRIMARY KEY,
    reference VARCHAR(100) NOT NULL,
    nbr_places INTEGER NOT NULL CHECK (nbr_places > 0),
    type_carburant VARCHAR(2) NOT NULL CHECK (type_carburant IN ('D','ES','EL','H'))
);

CREATE TABLE Token (
    idToken SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    date_heure_expiration TIMESTAMP NOT NULL
);

