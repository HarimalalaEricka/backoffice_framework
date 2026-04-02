-- =========================================================
-- Test amélioré: split passager + sélection "plus proche"
-- Date de test: 2026-05-02
-- Objectif:
--   1) Tester le SPLIT : réservation de 12 passagers sur véhicule 10 places
--      => 10 assignés immédiatement, 2 restants assignés au véhicule suivant
--   2) Tester le "PLUS PROCHE" : plusieurs véhicules, choisir celui dont
--      la capacité est la plus proche du besoin restant
--   3) Tester les créneau de 30 min et la réutilisation de véhicules
-- =========================================================

\c gestion_ticket

-- Nettoyage complet pour test isolé
TRUNCATE TABLE Assignation CASCADE;
TRUNCATE TABLE Reservation CASCADE;
TRUNCATE TABLE Vehicule CASCADE;
TRUNCATE TABLE Distance CASCADE;
TRUNCATE TABLE Parametre CASCADE;
TRUNCATE TABLE Hotel CASCADE;

-- Paramètre de planification
-- vitesse_moyenne = 60 km/h => 1 km ~= 1 minute
-- temps_attente = 30 min => fenêtre [t ; t+30]
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente) VALUES
(1, 60, 30);

-- Hôtels
INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aéroport Ivato', 'AIV', 'aeroport'),
(2, 'Hotel D22', 'H22', 'hotel');

-- Distance Aéroport <-> Hotel D22
-- 22 km aller => 22 min, aller/retour = 44 min
INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 22);

-- =========================================================
-- VÉHICULES : Varier les capacités pour tester "plus proche"
-- =========================================================
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(201, 'V-10P-001', 10, 'D'),   -- 10 places
(202, 'V-8P-002', 8, 'D'),     -- 8 places  
(203, 'V-6P-003', 6, 'D'),     -- 6 places
(204, 'V-12P-004', 12, 'D');   -- 12 places

-- =========================================================
-- GROUPE A (autour de 10h) - Test du SPLIT
-- Objectif : réservation de 12 passagers => split sur V-10P (10) + V-8P (2)
-- =========================================================
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(9001, 'A-R1-12', '2026-05-02 10:00:00', 12, 2),  -- 12 passagers => SPLIT !
(9002, 'A-R2-5', '2026-05-02 10:05:00', 5, 2),    -- 5 passagers
(9003, 'A-R3-3', '2026-05-02 10:10:00', 3, 2),    -- 3 passagers
(9004, 'A-R4-2', '2026-05-02 10:15:00', 2, 2),    -- 2 passagers
(9005, 'A-R5-1', '2026-05-02 10:20:00', 1, 2);    -- 1 passager

-- Réservations pour après retour du véhicule (groupe A)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(9006, 'A-POST-RET-4', '2026-05-02 11:05:00', 4, 2),
(9007, 'A-POST-RET-2', '2026-05-02 11:10:00', 2, 2);

-- =========================================================
-- GROUPE B (autour de 13h) - Test du "plus proche"
-- Objectif : avec réservations de capacités variées,
--   vérifier que le véhicule "plus proche" est sélectionné
-- =========================================================
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(9008,  'B-R1-7', '2026-05-02 13:00:00', 7, 2),   -- 7 pax => V-8P serait "plus proche" que V-12P
(9009,  'B-R2-4', '2026-05-02 13:05:00', 4, 2),   -- 4 pax => V-6P serait "plus proche"
(9010, 'B-R3-3', '2026-05-02 13:10:00', 3, 2),    -- 3 pax => V-6P serait "plus proche"
(9011, 'B-R4-6', '2026-05-02 13:15:00', 6, 2),    -- 6 pax => V-6P complet, puis suivant
(9012, 'B-R5-2', '2026-05-02 13:20:00', 2, 2);    -- 2 pax => complète V-8P ou V-10P

-- Réservations pour après retour du véhicule (groupe B)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(9013, 'B-POST-RET-8', '2026-05-02 14:05:00', 8, 2),   -- 8 pax => V-8P ou V-10P
(9014, 'B-POST-RET-3', '2026-05-02 14:10:00', 3, 2);   -- 3 pax => V-6P

-- =========================================================
-- REQUÊTES DE CONTRÔLE (avant planification)
-- =========================================================
SELECT * FROM Parametre;
SELECT * FROM Distance ORDER BY from_hotel_id, to_hotel_id;

SELECT v.idVehicule, v.reference, v.nbr_places
FROM Vehicule v
ORDER BY v.nbr_places DESC;

SELECT DATE(date_heure_arrivee) AS d, COUNT(*) AS nb_resa, SUM(nbr_pers) AS pax
FROM Reservation
GROUP BY DATE(date_heure_arrivee)
ORDER BY d;

-- Avant planification, aucune réservation ne doit être assignée
SELECT r.idReservation, r.client_id, r.nbr_pers,
       COALESCE(SUM(a.nb_pers_assigne),0) AS deja_assigne,
       (r.nbr_pers - COALESCE(SUM(a.nb_pers_assigne),0)) AS restants
FROM Reservation r
LEFT JOIN Assignation a ON a.reservation_id = r.idReservation
GROUP BY r.idReservation, r.client_id, r.nbr_pers
ORDER BY r.idReservation;

-- =========================================================
-- APRÈS planifierJour('2026-05-02') via API/UI
-- Décommente ces requêtes pour vérifier les restants et les splits
-- =========================================================
-- SELECT r.idReservation, r.client_id, r.date_heure_arrivee, r.nbr_pers,
--        COALESCE(SUM(a.nb_pers_assigne),0) AS assigned,
--        (r.nbr_pers - COALESCE(SUM(a.nb_pers_assigne),0)) AS restants
-- FROM Reservation r
-- LEFT JOIN Assignation a ON a.reservation_id = r.idReservation
-- WHERE DATE(r.date_heure_arrivee) = DATE '2026-05-02'
-- GROUP BY r.idReservation, r.client_id, r.date_heure_arrivee, r.nbr_pers
-- ORDER BY r.date_heure_arrivee, r.idReservation;

-- SELECT a.vehicule_id, v.reference, v.nbr_places, SUM(a.nb_pers_assigne) AS pax_assigned
-- FROM Assignation a
-- JOIN Vehicule v ON v.idVehicule = a.vehicule_id
-- JOIN Reservation r ON r.idReservation = a.reservation_id
-- WHERE DATE(r.date_heure_arrivee) = DATE '2026-05-02'
-- GROUP BY a.vehicule_id, v.reference, v.nbr_places
-- ORDER BY a.vehicule_id;

-- Vérifier les splits : véhicule assigné une même réservation
-- SELECT a.vehicule_id, v.reference, a.reservation_id, r.client_id, r.nbr_pers, a.nb_pers_assigne
-- FROM Assignation a
-- JOIN Vehicule v ON v.idVehicule = a.vehicule_id
-- JOIN Reservation r ON r.idReservation = a.reservation_id
-- WHERE DATE(r.date_heure_arrivee) = DATE '2026-05-02'
-- ORDER BY r.idReservation, a.vehicule_id;
