-- =========================================
-- TEST SPRINT 7 : vehicule entamé + choix "reste le plus proche"
-- Base: gestion_ticket (PostgreSQL)
-- =========================================

\c gestion_ticket;

BEGIN;

-- Nettoyage (reproductible)
TRUNCATE TABLE Assignation RESTART IDENTITY CASCADE;
TRUNCATE TABLE Reservation RESTART IDENTITY CASCADE;
TRUNCATE TABLE Vehicule RESTART IDENTITY CASCADE;
TRUNCATE TABLE Distance RESTART IDENTITY CASCADE;
TRUNCATE TABLE Hotel RESTART IDENTITY CASCADE;
TRUNCATE TABLE Parametre RESTART IDENTITY CASCADE;

-- -----------------------------------------
-- PARAMETRE
-- vitesse_moyenne: pour TrajetCalculator
-- temps_attente: pour grouperParTrancheAttente (Sprint 5)
-- -----------------------------------------
INSERT INTO Parametre(idParametre, vitesse_moyenne, temps_attente)
VALUES (1, 60, 30);

-- -----------------------------------------
-- HOTELS
-- IMPORTANT: il faut 1 ligne libelle='aeroport'
-- sinon TrajetCalculator échoue
-- -----------------------------------------
INSERT INTO Hotel(idHotel, nom, code, libelle) VALUES
(1, 'AEROPORT', 'AIR', 'aeroport'),
(2, 'HOTEL_TEST', 'H01', 'hotel');

-- -----------------------------------------
-- DISTANCES (bidirectionnelles gérées en code)
-- On met quelques valeurs simples pour éviter "distance non trouvée"
-- -----------------------------------------
INSERT INTO Distance(from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 10);

-- -----------------------------------------
-- VEHICULES (v1, v2)
-- type_carburant: D prioritaire en tie-breaker
-- -----------------------------------------
INSERT INTO Vehicule(idVehicule, reference, nbr_places, type_carburant) VALUES
(1, 'V1-8P', 8, 'D'),
(2, 'V2-3P', 3, 'ES');

-- -----------------------------------------
-- RESERVATIONS (r1, r2, r3) même tranche / même heure
-- Même hotel_id (2) pour simplifier le trajet
-- -----------------------------------------
INSERT INTO Reservation(idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(101, 'R1', '2026-03-21 09:00:00', 6, 2),
(102, 'R2', '2026-03-21 09:00:00', 4, 2),
(103, 'R3', '2026-03-21 09:00:00', 3, 2);

COMMIT;

-- =========================================
-- EXECUTION
-- 1) Lance la planification sur la date 2026-03-21
--    (via ton UI / ou main de TestSprint7 / PlanificationService.planifierJour)
-- 2) Puis exécute les requêtes de vérification ci-dessous
-- =========================================

-- Vérifier l'ordre des insertions d'assignation (idAssignation croissant)
-- Attendu (minimum):
--  - V1-8P : R1 nb_pers_assigne = 6
--  - V1-8P : R3 nb_pers_assigne = 2
SELECT
  a.idAssignation,
  v.reference AS vehicule,
  r.client_id AS reservation,
  a.nb_pers_assigne,
  a.date_heure_planification
FROM Assignation a
JOIN Vehicule v ON v.idVehicule = a.vehicule_id
JOIN Reservation r ON r.idReservation = a.reservation_id
ORDER BY a.idAssignation ASC;

-- Vue agrégée par véhicule + réservation (utile si split)
SELECT
  v.reference AS vehicule,
  r.client_id AS reservation,
  SUM(a.nb_pers_assigne) AS total_assignes
FROM Assignation a
JOIN Vehicule v ON v.idVehicule = a.vehicule_id
JOIN Reservation r ON r.idReservation = a.reservation_id
GROUP BY v.reference, r.client_id
ORDER BY v.reference, r.client_id;

-- Assertion ciblée : V1 doit prendre 6 de R1 et 2 de R3
SELECT
  (SELECT COALESCE(SUM(nb_pers_assigne),0) FROM Assignation WHERE vehicule_id = 1 AND reservation_id = 101) AS v1_r1,
  (SELECT COALESCE(SUM(nb_pers_assigne),0) FROM Assignation WHERE vehicule_id = 1 AND reservation_id = 103) AS v1_r3;
-- Attendu: v1_r1 = 6 ; v1_r3 = 2