-- =========================================================
-- Test ciblé: double groupe + réservations non assignées au départ
-- Date de test: 2026-05-02
-- Objectif:
--   1) Groupe A: 5 réservations, capacité dépassée -> 2 restent non assignées
--      puis sont reprises après retour véhicule avec d'autres réservations proches.
--   2) Groupe B vers 13:00: même principe, 2 non assignées puis reprise après retour.
-- Important: aucune simulation d'arrivée ni assignation manuelle initiale.
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

-- Véhicule de 12 places
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(201, 'V-TEST-DOUBLE-GROUPE-12P', 12, 'D');

-- =========================================================
-- GROUPE A (5 réservations, autour de 10h)
-- Capacité totale = 16 (>12) => 2 réservations resteront non assignées au 1er départ.
-- =========================================================
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(9001, 'A-R1-6', '2026-05-02 10:00:00', 6, 2),
(9002, 'A-R2-4', '2026-05-02 10:05:00', 4, 2),
(9003, 'A-R3-3', '2026-05-02 10:10:00', 3, 2),
(9004, 'A-R4-2', '2026-05-02 10:15:00', 2, 2),
(9005, 'A-R5-1', '2026-05-02 10:20:00', 1, 2);

-- Nouvelles réservations proches du retour du véhicule pour le groupe A
-- (dans la même logique de créneau de reprise)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(9006, 'A-POST-RET-2', '2026-05-02 11:05:00', 2, 2),
(9007, 'A-POST-RET-1', '2026-05-02 11:10:00', 1, 2);

-- =========================================================
-- GROUPE B (autour de 13h):
-- 5 réservations de base avec capacité dépassée -> 2 non assignées,
-- puis d'autres réservations dans la même plage de reprise.
-- =========================================================
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(9008,  'B-R1-7', '2026-05-02 13:00:00', 7, 2),
(9009,  'B-R2-4', '2026-05-02 13:05:00', 4, 2),
(9010, 'B-R3-3', '2026-05-02 13:10:00', 3, 2),
(9011, 'B-R4-2', '2026-05-02 13:15:00', 2, 2),
(9012, 'B-R5-1', '2026-05-02 13:20:00', 1, 2);

INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(9013, 'B-POST-RET-2', '2026-05-02 14:05:00', 2, 2),
(9014, 'B-POST-RET-1', '2026-05-02 14:10:00', 1, 2);

-- =========================================================
-- REQUÊTES DE CONTRÔLE (avant planification)
-- =========================================================
SELECT * FROM Parametre;
SELECT * FROM Distance ORDER BY from_hotel_id, to_hotel_id;

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
-- Décommente ces requêtes pour vérifier les restants
-- =========================================================
-- SELECT r.idReservation, r.client_id, r.date_heure_arrivee, r.nbr_pers,
--        COALESCE(SUM(a.nb_pers_assigne),0) AS assigned,
--        (r.nbr_pers - COALESCE(SUM(a.nb_pers_assigne),0)) AS restants
-- FROM Reservation r
-- LEFT JOIN Assignation a ON a.reservation_id = r.idReservation
-- WHERE DATE(r.date_heure_arrivee) = DATE '2026-05-02'
-- GROUP BY r.idReservation, r.client_id, r.date_heure_arrivee, r.nbr_pers
-- ORDER BY r.date_heure_arrivee, r.idReservation;

-- SELECT a.vehicule_id, v.reference, SUM(a.nb_pers_assigne) AS pax_assigned
-- FROM Assignation a
-- JOIN Vehicule v ON v.idVehicule = a.vehicule_id
-- JOIN Reservation r ON r.idReservation = a.reservation_id
-- WHERE DATE(r.date_heure_arrivee) = DATE '2026-05-02'
-- GROUP BY a.vehicule_id, v.reference
-- ORDER BY a.vehicule_id;
