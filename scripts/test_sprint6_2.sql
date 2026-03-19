/*
================================================================================
TEST SPRINT 6 - TÂCHE 2 : Nouveau Critère de Sélection Véhicule
================================================================================

CRITÈRES D'ASSIGNATION (dans l'ordre) :
  1. Capacité véhicule >= nbr personnes à transporter
  2. Capacité la plus proche du besoin (minimiser la capacité)
  3. [NOUVEAU] Véhicule ayant le MOINS de trajets déjà effectués sur la date
  4. Si égalité => Priorité Diesel
  5. Si encore égalité => Random

RÈGLE POUR heure_depart :
  - Si dernierVol < heure_retour => heure_depart = heure_retour
  - Si dernierVol >= heure_retour => heure_depart = dernierVol

SCÉNARIOS DE TEST :
  - G1 (09:00): Tous véhicules vierges => priorité Diesel (V1 ou V5)
  - G2 (10:30): V3 a 0 trajets vs V1,V2 qui en ont 1 => V3 choisi
  - G3 (11:30): Égalité des trajets => capacité, puis Diesel
  - G4 (13:00): V5 (nouveau Diesel) a 0 trajets => priorité
  - G5 (14:30): Test heure_depart avec dernier vol
  - G6 (16:00): Petit groupe => capacité minimale
  - G7 (17:00): Grand groupe => véhicules plus grands

================================================================================
*/

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

-- 4) Véhicules (capacités différentes et carburants différents)
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(1, 'VH-001', 10, 'D'),      -- Diesel, 10 places
(2, 'VH-002', 10, 'ES'),     -- Essence, 10 places
(3, 'VH-003', 8, 'D'),       -- Diesel, 8 places
(4, 'VH-004', 6, 'ES'),      -- Essence, 6 places
(5, 'VH-005', 10, 'D');      -- Diesel, 10 places

-- 5) Réservations - Scénarios de test
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
-- ========== GROUPE 1 : 09:00 (2 clients) ==========
-- Scénario: V1 et V2 vierges (0 trajets). V1 est Diesel => priorité Diesel
(1, 'C1', '2026-03-20 09:00:00', 5, 2),
(2, 'C2', '2026-03-20 09:05:00', 3, 2),

-- ========== GROUPE 2 : 10:30 (2 clients) ==========
-- Scénario: V1 a 1 trajet, V2 a 1 trajet. V3 a 0 trajets (preference)
-- Test: vérifier que V3 (8 places) est choisi car moins de trajets
(3, 'C3', '2026-03-20 10:30:00', 4, 2),
(4, 'C4', '2026-03-20 10:35:00', 3, 2),

-- ========== GROUPE 3 : 11:30 (2 clients) ==========
-- Scénario: V1 a 1 trajet, V2 a 1 trajet, V3 a 1 trajet
-- V4 a 0 trajets mais capacité 6 < 7 personnes => ne peut pas être assigné
-- Test: Résultat en egalité entre V1, V2, V3 => priorité Diesel (V1 ou V3)
(5, 'C5', '2026-03-20 11:30:00', 3, 2),
(6, 'C6', '2026-03-20 11:35:00', 4, 2),

-- ========== GROUPE 4 : 13:00 (2 clients) ==========
-- Scénario: Tous les véhicules ont 1 trajet
-- V5 (nouveau Diesel) a 0 trajets => priorité Diesel => V5 choisi
(7, 'C7', '2026-03-20 13:00:00', 5, 2),
(8, 'C8', '2026-03-20 13:05:00', 2, 2),

-- ========== GROUPE 5 : 14:30 (2 clients) ==========
-- Scénario: Test heure_depart avec dernier vol
-- Supposons V1 retour à 13:45, dernier vol du groupe à 13:50
-- Si heure_retour (13:45) < dernier_vol (13:50) => heure_depart = 13:50
-- V2 retour à 13:40 < heure_retour V1 (13:45) mais V2 a 1 trajet, V1 a 2
-- => devrait chercher autre véhicule
(9, 'C9', '2026-03-20 14:30:00', 6, 2),
(10, 'C10', '2026-03-20 14:35:00', 1, 2),

-- ========== GROUPE 6 : 16:00 (1 client) ==========
-- Scénario: Test avec petit nombre de passagers
-- Tous véhicules même nombre de trajets => capacité minimale (V4 avec 6 places)
(11, 'C11', '2026-03-20 16:00:00', 2, 2),

-- ========== GROUPE 7 : 17:00 (2 clients) ==========
-- Scénario: Surcharger V4 (6 places) impossible
-- Forcer l'utilisation d'autres véhicules
(12, 'C12', '2026-03-20 17:00:00', 5, 2),
(13, 'C13', '2026-03-20 17:05:00', 3, 2);