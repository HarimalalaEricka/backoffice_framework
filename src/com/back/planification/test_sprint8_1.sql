-- Connexion à la base
\c gestion_ticket

-- =========================
-- 1. INSERTION DES HÔTELS (incluant l'aeroport)
-- =========================
INSERT INTO Hotel (nom, code, libelle) VALUES
('Aeroport International', 'AIR', 'aeroport'),
('Hotel Paradis', 'PAR', 'hotel'),
('Hotel Soleil', 'SOL', 'hotel'),
('Hotel Ocean', 'OCE', 'hotel'),
('Hotel Montagne', 'MON', 'hotel'),
('Hotel Riviera', 'RIV', 'hotel');

-- =========================
-- 2. INSERTION DES VEHICULES
-- =========================
INSERT INTO Vehicule (reference, nbr_places, type_carburant) VALUES
('AA-001-AA', 8, 'D'),
('BB-002-BB', 6, 'ES'),
('CC-003-CC', 10, 'D'),
('DD-004-DD', 4, 'EL'),
('EE-005-EE', 7, 'D'),
('FF-006-FF', 5, 'ES');

-- =========================
-- 3. INSERTION DES PARAMETRES
-- =========================
INSERT INTO Parametre (vitesse_moyenne, temps_attente) VALUES
(50, 30);  -- 50 km/h, 30 minutes d'attente

-- =========================
-- 4. INSERTION DES DISTANCES
-- =========================
-- Aeroport (id=1) vers les hôtels
INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 15),  -- Aeroport -> Paradis
(1, 3, 25),  -- Aeroport -> Soleil
(1, 4, 10),  -- Aeroport -> Ocean
(1, 5, 35),  -- Aeroport -> Montagne
(1, 6, 20);  -- Aeroport -> Riviera

-- Distances entre hôtels (retour)
INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(2, 3, 12), (3, 2, 12),
(2, 4, 8),  (4, 2, 8),
(2, 5, 18), (5, 2, 18),
(2, 6, 10), (6, 2, 10),
(3, 4, 15), (4, 3, 15),
(3, 5, 22), (5, 3, 22),
(3, 6, 14), (6, 3, 14),
(4, 5, 20), (5, 4, 20),
(4, 6, 12), (6, 4, 12),
(5, 6, 25), (6, 5, 25);

-- =========================
-- 5. INSERTION DES RESERVATIONS POUR TEST TÂCHE 1 SPRINT 8
-- =========================

-- SCeNARIO DE TEST :
-- Objectif : Verifier que les reservations non assignees (arrivees à 06:00, 06:45, 06:50)
-- sont traitees en PRIORITe avant les nouvelles reservations du groupe (08:00, 08:10, 08:15)

-- 5.1 Reservations du matin (qui seront NON ASSIGNeES lors de la planification)
-- Ces reservations arrivent avant le groupe principal mais n'ont pas pu être assignees
-- faute de vehicules disponibles

-- Premier groupe (06:00) - sera non assigne
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT_A1', '2026-04-15 06:00:00', 12, 2),  -- 12 personnes pour Paradis (trop pour un seul vehicule)
('CLIENT_A2', '2026-04-15 06:45:00', 3, 3),   -- 3 personnes pour Soleil
('CLIENT_A3', '2026-04-15 06:50:00', 2, 4);   -- 2 personnes pour Ocean

-- 5.2 Nouvelles reservations du groupe (08:00-08:30)
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT_B1', '2026-04-15 08:00:00', 4, 2),   -- 4 personnes pour Paradis
('CLIENT_B2', '2026-04-15 08:10:00', 2, 5),   -- 2 personnes pour Montagne
('CLIENT_B3', '2026-04-15 08:15:00', 3, 6);   -- 3 personnes pour Riviera

-- 5.3 Reservations supplementaires pour completer le test
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT_C1', '2026-04-15 09:30:00', 5, 2),   -- Groupe suivant
('CLIENT_C2', '2026-04-15 09:45:00', 2, 3),
('CLIENT_C3', '2026-04-15 10:00:00', 4, 4);

-- =========================
-- 6. INSeRER QUELQUES ASSIGNATIONS EXISTANTES POUR SIMULER DES VeHICULES OCCUPeS
-- =========================

-- Vehicule 1 (8 places) part à 07:30 et revient à 08:20
INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES
(1, 1, '2026-04-15 00:00:00', 8);  -- Partiellement assigne (8/12 personnes)

-- Vehicule 2 (6 places) part à 07:00 et revient à 07:45
INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES
(2, 2, '2026-04-15 00:00:00', 3);  -- Complet (3/3 personnes)

-- Vehicule 3 (10 places) part à 06:30 et revient à 07:30
INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES
(3, 3, '2026-04-15 00:00:00', 2);  -- Complet (2/2 personnes)

-- =========================
-- 7. VeRIFICATION DES DONNeES
-- =========================

-- Afficher toutes les reservations du 2026-04-15
SELECT 
    r.idReservation,
    r.client_id,
    r.date_heure_arrivee,
    r.nbr_pers,
    (r.nbr_pers - COALESCE(SUM(a.nb_pers_assigne), 0)) AS passagers_restants,
    h.nom AS hotel
FROM Reservation r
LEFT JOIN Assignation a ON a.reservation_id = r.idReservation
JOIN Hotel h ON h.idHotel = r.hotel_id
WHERE DATE(r.date_heure_arrivee) = '2026-04-15'
GROUP BY r.idReservation, r.client_id, r.date_heure_arrivee, r.nbr_pers, h.nom
ORDER BY r.date_heure_arrivee;

-- Resultat attendu :
-- id | client_id | date_heure_arrivee | nbr_pers | passagers_restants | hotel
-- ----------------------------------------------------------------------------
-- 1  | CLIENT_A1 | 06:00:00           | 12       | 4                  | Paradis
-- 2  | CLIENT_A2 | 06:45:00           | 3        | 0                  | Soleil
-- 3  | CLIENT_A3 | 06:50:00           | 2        | 0                  | Ocean
-- 4  | CLIENT_B1 | 08:00:00           | 4        | 4                  | Paradis
-- 5  | CLIENT_B2 | 08:10:00           | 2        | 2                  | Montagne
-- 6  | CLIENT_B3 | 08:15:00           | 3        | 3                  | Riviera
-- 7  | CLIENT_C1 | 09:30:00           | 5        | 5                  | Paradis
-- 8  | CLIENT_C2 | 09:45:00           | 2        | 2                  | Soleil
-- 9  | CLIENT_C3 | 10:00:00           | 4        | 4                  | Ocean