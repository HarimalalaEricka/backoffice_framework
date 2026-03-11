-- ============================================================================
-- DONNÉES DE TEST SPRINT 3 - RÉUTILISATION VÉHICULES & CALCUL TRAJET
-- Date: 10 Mars 2026
-- ============================================================================

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
('Lokanga Boutique Hotel', 'H003', 'hotel'),
('Novotel Convention & Spa', 'H004', 'hotel'),
('Ibis Hotel Ankorondrano', 'H005', 'hotel'),
-- Hôtels pour test RG11 (tri alphabétique)
('Zebra Palace Hotel', 'H006', 'hotel'),
('Alpha Star Hotel', 'H007', 'hotel'),
('Mikado Resort Hotel', 'H008', 'hotel');

-- ============================================================================
-- 2. PARAMÈTRES DE CALCUL
-- ============================================================================
-- Vitesse moyenne: 60 km/h
-- Temps d'attente par arrêt: 10 minutes
INSERT INTO Parametre (vitesse_moyenne, temps_attente) VALUES (60, 10);

-- ============================================================================
-- 3. DISTANCES (en km)
-- ============================================================================
-- Distances depuis l'aéroport vers chaque hôtel
INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES

(7, 1, 22),  -- Zebra Palace → Aéroport (22 km)
(8, 1, 22),  -- Alpha Star → Aéroport (22 km)
(9, 1, 22),  -- Mikado Resort → Aéroport (22 km)

-- Distances entre hôtels (pour trajets multi-arrêts)
(2, 3, 5),   -- Carton → Radisson (5 km)
(2, 4, 8),   -- Carton → Lokanga (8 km)
(3, 4, 6),   -- Radisson → Lokanga (6 km)
(4, 5, 10),  -- Lokanga → Novotel (10 km)
(5, 6, 8),   -- Novotel → Ibis (8 km)

-- Distances entre les hôtels de test RG11 (toutes égales pour tester le tri alphabétique)
(7, 8, 5),   -- Zebra → Alpha (5 km)
(8, 7, 5),   -- Alpha → Zebra (5 km)
(7, 9, 5),   -- Zebra → Mikado (5 km)
(9, 7, 5),   -- Mikado → Zebra (5 km)
(8, 9, 5),   -- Alpha → Mikado (5 km)
(9, 8, 5);   -- Mikado → Alpha (5 km)

-- ============================================================================
-- 4. VÉHICULES
-- ============================================================================
-- Types: D=Diesel, ES=Essence, EL=Électrique, H=Hydrogène
INSERT INTO Vehicule (reference, nbr_places, type_carburant) VALUES
('VH-001-D', 4, 'D'),   -- Petit véhicule Diesel
('VH-002-D', 4, 'D'),   -- Petit véhicule Diesel
('VH-003-ES', 7, 'ES'), -- Moyen véhicule Essence
('VH-004-D', 7, 'D'),   -- Moyen véhicule Diesel (prioritaire)
('VH-005-EL', 12, 'EL'),-- Grand véhicule Électrique
('VH-006-D', 12, 'D'),  -- Grand véhicule Diesel (prioritaire)
('VH-007-ES', 20, 'ES'),-- Très grand véhicule Essence
('VH-008-D', 20, 'D');  -- Très grand véhicule Diesel (prioritaire)

-- ============================================================================
-- 5. RÉSERVATIONS POUR TEST - 10 Mars 2026
-- ============================================================================

-- -----------------------------------------------------
-- CAS 1 : VOL 10H00 - Test trajet simple (1 hôtel)
-- -----------------------------------------------------
-- Groupe de 3 personnes → Hotel Carton
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-001', '2026-03-10 10:00:00', 3, 2);

-- -----------------------------------------------------
-- CAS 2 : VOL 11H00 - Test trajet multi-hôtels
-- -----------------------------------------------------
-- 3 réservations vers 3 hôtels différents (total: 8 personnes)
-- Peuvent tenir dans un véhicule de 12 places
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-002', '2026-03-10 11:00:00', 2, 4), -- Lokanga (12km)
('CLIENT-003', '2026-03-10 11:00:00', 3, 2), -- Carton (15km)
('CLIENT-004', '2026-03-10 11:00:00', 3, 3); -- Radisson (18km)

-- -----------------------------------------------------
-- CAS 3 : VOL 13H00 - Test réutilisation véhicule
-- -----------------------------------------------------
-- Le véhicule du vol de 10h00 devrait être revenu
-- Devrait pouvoir être réutilisé
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-005', '2026-03-10 13:00:00', 2, 3), -- Radisson
('CLIENT-006', '2026-03-10 13:00:00', 2, 4); -- Lokanga

-- -----------------------------------------------------
-- CAS 4 : VOL 14H30 - Test plusieurs véhicules
-- -----------------------------------------------------
-- Groupe trop grand pour un seul véhicule
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-007', '2026-03-10 14:30:00', 15, 5), -- 15 personnes → Novotel
('CLIENT-008', '2026-03-10 14:30:00', 10, 6); -- 10 personnes → Ibis

-- -----------------------------------------------------
-- CAS 5 : VOL 16H00 - Test capacité optimale
-- -----------------------------------------------------
-- Exactement 12 personnes → devrait choisir véhicule 12 places Diesel
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-009', '2026-03-10 16:00:00', 5, 2),
('CLIENT-010', '2026-03-10 16:00:00', 4, 3),
('CLIENT-011', '2026-03-10 16:00:00', 3, 4);

-- -----------------------------------------------------
-- CAS 6 : VOL 17H00 - Test distance longue
-- -----------------------------------------------------
-- Vers l'hôtel le plus éloigné (25 km)
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-012', '2026-03-10 17:00:00', 6, 6); -- Ibis (25km)

-- -----------------------------------------------------
-- CAS 7 : VOL 18H00 - SPRINT 4 - Test remplissage progressif RG8
-- -----------------------------------------------------
-- Plusieurs petites réservations pour le même vol
-- Un véhicule de 12 places devrait faire PLUSIEURS VOYAGES (remplissage progressif)
-- Voyage 1 : 5 personnes → Carton
-- Voyage 2 : 4 personnes + 3 personnes (total 7) → Radisson
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-013', '2026-03-11 18:00:00', 5, 2),  -- Carton (voyage 1)
('CLIENT-014', '2026-03-11 18:00:00', 4, 3),  -- Radisson (voyage 2)
('CLIENT-015', '2026-03-11 18:00:00', 3, 3);  -- Radisson (voyage 2)

-- -----------------------------------------------------
-- CAS 8 : VOL 19H00 - SPRINT 4 - Test algorithme nearest-first RG9
-- -----------------------------------------------------
-- Plusieurs hôtels à visiter, ordre devrait être optimisé par proximité
-- Ordre attendu depuis Aéroport (ID=1):
--   1. Lokanga (12km)
--   2. Carton (distance Lokanga→Carton = 8km)
--   3. Radisson (distance Carton→Radisson = 5km)
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-016', '2026-03-10 19:00:00', 2, 2),  -- Carton
('CLIENT-017', '2026-03-10 19:00:00', 2, 3),  -- Radisson
('CLIENT-018', '2026-03-10 19:00:00', 2, 4);  -- Lokanga

-- -----------------------------------------------------
-- CAS 9 : VOL 20H00 - SPRINT 4 - Test tri alphabétique RG11
-- -----------------------------------------------------
-- 3 hôtels à EXACTEMENT la même distance (22 km)
-- L'ordre alphabétique devrait être appliqué : Alpha → Mikado → Zebra
-- Ordre attendu :
--   1. Alpha Star Hotel (22 km - 'A' alphabétiquement premier)
--   2. Mikado Resort Hotel (22 km - 'M' avant 'Z')
--   3. Zebra Palace Hotel (22 km - 'Z' dernier)
INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-019', '2026-03-10 20:00:00', 2, 7),  -- Zebra Palace (devrait être visité 3ème)
('CLIENT-020', '2026-03-10 20:00:00', 2, 8),  -- Alpha Star (devrait être visité 1er)
('CLIENT-021', '2026-03-10 20:00:00', 2, 9);  -- Mikado Resort (devrait être visité 2ème)

-- ============================================================================
-- 6. TOKEN DE TEST (valide 24h)
-- ============================================================================
INSERT INTO Token (token, date_heure_expiration) VALUES
('TEST-TOKEN-2026-03-10', '2026-03-11 23:59:59');

-- ============================================================================
-- RÉSUMÉ DES DONNÉES DE TEST
-- ============================================================================
SELECT '=== RÉSUMÉ DES DONNÉES ===' as info;

SELECT 
    (SELECT COUNT(*) FROM Hotel) as nb_hotels,
    (SELECT COUNT(*) FROM Hotel WHERE libelle = 'aeroport') as nb_aeroports,
    (SELECT COUNT(*) FROM Vehicule) as nb_vehicules,
    (SELECT COUNT(*) FROM Reservation) as nb_reservations,
    (SELECT COUNT(*) FROM Distance) as nb_distances,
    (SELECT COUNT(*) FROM Parametre) as nb_parametres;

-- Vérifier les réservations par vol
SELECT '=== RÉSERVATIONS PAR VOL ===' as info;
SELECT 
    date_heure_arrivee as vol,
    COUNT(*) as nb_reservations,
    SUM(nbr_pers) as total_personnes
FROM Reservation
GROUP BY date_heure_arrivee
ORDER BY date_heure_arrivee;

-- Vérifier les véhicules disponibles
SELECT '=== VÉHICULES DISPONIBLES ===' as info;
SELECT 
    reference,
    nbr_places as capacite,
    type_carburant as carburant
FROM Vehicule
ORDER BY nbr_places, type_carburant;

-- ============================================================================
-- FIN DU SCRIPT
-- ============================================================================
