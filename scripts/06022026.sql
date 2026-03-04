DROP DATABASE IF EXISTS gestion_ticket;
CREATE DATABASE gestion_ticket;

\c gestion_ticket

CREATE TABLE Hotel (
    idHotel SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL
);
ALTER TABLE Hotel ADD COLUMN code VARCHAR(100);
ALTER TABLE Hotel ADD COLUMN libelle VARCHAR(100);

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

-- Données de test
INSERT INTO Hotel (nom, code, libelle) VALUES ('Hôtel Luxe', 'LUX', 'Hotel');
INSERT INTO Hotel (nom, code, libelle) VALUES ('ATR', 'MOD', 'aeroport');

INSERT INTO reservation VALUES 
(1, 'CLI001', 2, TIMESTAMP '2026-03-10 09:30:00', 1);

INSERT INTO reservation VALUES 
(2, 'CLI002', 4, TIMESTAMP '2026-03-10 14:00:00', 2);

INSERT INTO reservation VALUES 
(3, 'CLI003', 1, TIMESTAMP '2026-03-11 08:15:00', 3);

INSERT INTO reservation VALUES 
(4, 'CLI004', 3, TIMESTAMP '2026-03-11 18:45:00', 4);

INSERT INTO reservation VALUES 
(5, 'CLI005', 5, TIMESTAMP '2026-03-12 10:00:00', 5);

INSERT INTO reservation VALUES 
(6, 'CLI006', 2, TIMESTAMP '2026-03-12 16:30:00', 1);

INSERT INTO reservation VALUES 
(7, 'CLI007', 6, TIMESTAMP '2026-03-13 12:00:00', 2);

INSERT INTO reservation VALUES 
(8, 'CLI008', 3, TIMESTAMP '2026-03-13 20:15:00', 3);


--gestion_ticket=# select * from hotel;
 id |   nom
----+---------
  1 | Colbert
  2 | Novotel
  3 | Ibis
  4 | Lokanga

  --<option value="<%= h.getIdHotel() %>"><%= h.getCode() != null ? h.getCode()+" - " : "" %><%= h.getNom() %><%= h.getLibelle() != null ? " ("+h.getLibelle()+")" : "" %></option>

