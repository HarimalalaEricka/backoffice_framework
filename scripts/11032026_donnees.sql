-- Suppression des données existantes (pour test propre)
TRUNCATE TABLE Assignation CASCADE;
TRUNCATE TABLE Reservation CASCADE;
TRUNCATE TABLE Distance CASCADE;
TRUNCATE TABLE Vehicule CASCADE;
TRUNCATE TABLE Hotel CASCADE;
TRUNCATE TABLE Parametre CASCADE;

-- ============================================================================
-- 1. HÔTELS (Aéroport + Hôtels de destination)
-- ============================================================================
INSERT INTO Hotel (nom, code, libelle) VALUES
('Aéroport International Ivato', 'AERO', 'aeroport'),
('Hotel Carton Madagascar', 'H001', 'hotel'),
('Radisson Blu Hotel Waterfront', 'H002', 'hotel'),
('Ibis Hotel Ankorondrano', 'H005', 'hotel');


INSERT INTO Parametre (vitesse_moyenne, temps_attente) VALUES (60, 10);

INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 10),
(1, 3, 25),
(1, 4, 25),
(2, 3, 15),
(2, 4, 15);


INSERT INTO Vehicule (reference, nbr_places, type_carburant) VALUES
('VH-001', 12, 'D'),   
('VH-002', 12, 'D'),   
('VH-003', 8, 'ES'), 
('VH-004', 4, 'H'),   
('VH-005', 5, 'ES'),
('VH-006', 5, 'D');

INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(1, 'C001', '2026-03-15 10:00:00', 4, 3),
(2, 'C002', '2026-03-15 10:00:00', 7, 2),
(3, 'C003', '2026-03-15 15:00:00', 4, 4),
(4, 'C004', '2026-03-15 16:00:00', 15, 2),
(5, 'C005', '2026-03-16 09:00:00', 6, 2),
(6, 'C006', '2026-03-16 09:00:00', 7, 3),
(7, 'C007', '2026-03-16 12:00:00', 2, 3),
(8, 'C008', '2026-03-16 12:00:00', 3, 4);

