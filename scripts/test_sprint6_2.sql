-- 1) Nettoyage
TRUNCATE TABLE Assignation CASCADE;
TRUNCATE TABLE Reservation CASCADE;
TRUNCATE TABLE Vehicule CASCADE;
TRUNCATE TABLE Hotel CASCADE;
TRUNCATE TABLE Distance CASCADE;
TRUNCATE TABLE Parametre CASCADE;

-- 2) Paramètres
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente)
VALUES (1, 60, 30);

-- 3) Hôtels + distances nécessaires (minimal)
INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aéroport Ivato', 'AIV', 'aeroport'),
(2, 'Hotel Test', 'HT', 'hotel');

INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 20), (2, 1, 20);

-- 4) Véhicules (même capacité, différents trajets)
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(1, 'VH-001', 10, 'D'),
(2, 'VH-002', 10, 'ES');

-- 5) Réservations (2 groupes)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
-- Groupe 1 (09:00)
(1, 'C1', '2026-03-20 09:00:00', 5, 2),
(2, 'C2', '2026-03-20 09:05:00', 3, 2),

-- Groupe 2 (11:30)
(3, 'C3', '2026-03-20 11:30:00', 4, 2),
(4, 'C4', '2026-03-20 11:35:00', 2, 2);