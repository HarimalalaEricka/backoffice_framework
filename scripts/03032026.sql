DROP DATABASE IF EXISTS gestion_ticket;
CREATE DATABASE gestion_ticket;

\c gestion_ticket

-- =========================
-- TABLE HOTEL (Lieu + Aéroport)
-- =========================
CREATE TABLE Hotel (
    idHotel SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    libelle VARCHAR(100) NOT NULL --aeroport ou hotel
);

-- =========================
-- TABLE RESERVATION
-- =========================
CREATE TABLE Reservation (
    idReservation SERIAL PRIMARY KEY,
    client_id VARCHAR(50) NOT NULL,
    date_heure_arrivee TIMESTAMP NOT NULL,
    nbr_pers INTEGER NOT NULL CHECK (nbr_pers > 0),
    hotel_id INTEGER NOT NULL REFERENCES Hotel(idHotel) ON DELETE CASCADE
);

-- =========================
-- TABLE VEHICULE
-- =========================
CREATE TABLE Vehicule (
    idVehicule SERIAL PRIMARY KEY,
    reference VARCHAR(100) NOT NULL,
    nbr_places INTEGER NOT NULL CHECK (nbr_places > 0),
    type_carburant VARCHAR(2) NOT NULL CHECK (type_carburant IN ('D','ES','EL','H'))
);

-- =========================
-- TABLE TOKEN
-- =========================
CREATE TABLE Token (
    idToken SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    date_heure_expiration TIMESTAMP NOT NULL
);

-- =========================
-- TABLE PARAMETRE
-- =========================
CREATE TABLE Parametre (
    idParametre SERIAL PRIMARY KEY,
    vitesse_moyenne INTEGER NOT NULL CHECK (vitesse_moyenne > 0),
    temps_attente INTEGER NOT NULL CHECK (temps_attente >= 0)
);

-- =========================
-- TABLE DISTANCE
-- =========================
CREATE TABLE Distance (
    idDistance SERIAL PRIMARY KEY,
    from_hotel_id INTEGER NOT NULL REFERENCES Hotel(idHotel) ON DELETE CASCADE,
    to_hotel_id INTEGER NOT NULL REFERENCES Hotel(idHotel) ON DELETE CASCADE,
    distance_km INTEGER NOT NULL CHECK (distance_km > 0),
    UNIQUE (from_hotel_id, to_hotel_id)
);

CREATE TABLE Assignation (
    idAssignation SERIAL PRIMARY KEY,
    reservation_id INTEGER NOT NULL REFERENCES Reservation(idReservation) ON DELETE CASCADE,
    vehicule_id INTEGER NOT NULL REFERENCES Vehicule(idVehicule) ON DELETE CASCADE,
    date_heure_planification TIMESTAMP NOT NULL,
    nb_pers_assigne INTEGER NOT NULL CHECK (nb_pers_assigne > 0)
);