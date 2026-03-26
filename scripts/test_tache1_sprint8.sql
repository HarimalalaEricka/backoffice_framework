-- =================================================================
-- SCRIPT DE TEST POUR LA TÂCHE 1 - SPRINT 8
-- Priorisation des réservations non assignées dans le prochain groupe
--
-- Règle métier :
-- - Identifier toutes les réservations non assignées
-- - Les assigner au groupe d'intervalle suivante
-- - Les réservations non assignées seront assignées en premiers (priorité)
--
-- Date de test : 2026-03-26
-- =================================================================

-- Connexion à la base
\c gestion_ticket

-- =================================================================
-- NETTOYAGE DES DONNÉES EXISTANTES
-- =================================================================
TRUNCATE TABLE Assignation CASCADE;
TRUNCATE TABLE Reservation CASCADE;
TRUNCATE TABLE Vehicule CASCADE;
TRUNCATE TABLE Hotel CASCADE;
TRUNCATE TABLE Distance CASCADE;
TRUNCATE TABLE Parametre CASCADE;

-- =================================================================
-- DONNÉES DE BASE (Hôtels, Paramètres, Distances)
-- =================================================================

-- Hôtels
INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aéroport Ivato', 'AIV', 'aeroport'),
(2, 'Hotel Carlton', 'HCT', 'hotel'),
(3, 'Hotel Colbert', 'HCB', 'hotel'),
(4, 'Hotel Ibis', 'HIB', 'hotel'),
(5, 'Hotel Panorama', 'HPN', 'hotel');

-- Paramètres (Sprint 5 - temps d'attente = 30 minutes)
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente) VALUES
(1, 40, 30);

-- Distances
INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 25), (1, 3, 20), (1, 4, 18), (1, 5, 30),
(2, 3, 5), (2, 4, 8), (2, 5, 10),
(3, 4, 6), (3, 5, 12), (4, 5, 15);

-- Véhicules disponibles
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(1, 'BUS-001', 10, 'D'),
(2, 'BUS-002', 8, 'D'),
(3, 'BUS-003', 12, 'ES'),
(4, 'BUS-004', 6, 'D');

-- =================================================================
-- SCÉNARIO 1 : TÂCHE 1 - Priorisation des non assignées
-- Date de test : 2026-03-26
--
-- Scénario :
-- - 2 réservations non assignées du jour antérieur (25/03) : 06:00, 06:45
-- - 3 nouvelles réservations du jour actuel (26/03) : 08:00, 08:10, 08:15
-- - Temps d'attente = 30 min → Groupe 8:00-8:30
--
-- Attendu :
-- 1. Les non assignées (06:00, 06:45) sont assignées EN PREMIER
-- 2. Puis les nouvelles (08:00, 08:10, 08:15) selon RG7/RG11
-- 3. Respect des règles RG8 (remplissage progressif), RG9 (nearest-first)
-- =================================================================

-- Réservations non assignées du jour antérieur (25/03)
-- Ces réservations n'ont AUCUNE assignation → elles sont "non assignées"
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(1, 'CLIENT-001', '2026-03-25 06:00:00', 3, 2),  -- 3 pers, Hotel Carlton
(2, 'CLIENT-002', '2026-03-25 06:45:00', 2, 3);  -- 2 pers, Hotel Colbert

-- Nouvelles réservations du jour actuel (26/03)
-- Ces réservations arrivent dans le groupe 8:00-8:30
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(3, 'CLIENT-003', '2026-03-26 08:00:00', 4, 2),  -- 4 pers, Hotel Carlton
(4, 'CLIENT-004', '2026-03-26 08:10:00', 3, 4),  -- 3 pers, Hotel Ibis
(5, 'CLIENT-005', '2026-03-26 08:15:00', 2, 5);  -- 2 pers, Hotel Panorama

-- =================================================================
-- RÉSULTAT ATTENDU APRÈS PLANIFICATION DU 26/03
-- =================================================================

-- Phase 1 : Assignation des NON ASSIGNÉES (priorité absolue)
-- Ordre d'assignation : NON-ASSIGNEE-001 (3 pers) → NON-ASSIGNEE-002 (2 pers)
-- Véhicule BUS-001 (10 places) prend les 2 non assignées = 5 pers

-- Phase 2 : Assignation des NOUVELLES réservations selon RG7/RG11
-- RG7 : Tri par nombre passagers décroissant
-- CLIENT-003 (4 pers), CLIENT-004 (3 pers), CLIENT-005 (2 pers)
-- RG11 : En cas d'égalité, ordre alphabétique par hôtel
-- Véhicule BUS-001 (5 places restantes) prend CLIENT-003 (4 pers) = 9 pers
-- Véhicule BUS-002 (8 places) prend CLIENT-004 (3 pers) + CLIENT-005 (2 pers) = 5 pers

-- =================================================================
-- VÉRIFICATION DES RÉSULTATS
-- =================================================================

-- Exécuter la planification pour le 26/03
-- SELECT * FROM Assignation WHERE DATE(date_heure_planification) = '2026-03-26' ORDER BY vehicule_id, reservation_id;

-- Vérifier l'ordre d'assignation :
-- BUS-001 devrait avoir : NON-ASSIGNEE-001, NON-ASSIGNEE-002, CLIENT-003
-- BUS-002 devrait avoir : CLIENT-004, CLIENT-005

-- =================================================================
-- SCÉNARIO 2 : COMPORTEMENT NORMAL (sans non assignées)
-- Date de test : 2026-03-27
-- =================================================================

-- Nettoyer les assignations précédentes
-- TRUNCATE TABLE Assignation CASCADE;

-- Nouvelles réservations (aucune non assignée)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(6, 'NORMAL-001', '2026-03-27 09:00:00', 5, 2),
(7, 'NORMAL-002', '2026-03-27 09:15:00', 3, 3),
(8, 'NORMAL-003', '2026-03-27 09:30:00', 2, 4);

-- Attendu : Comportement normal RG7/RG11 sans priorisation

-- =================================================================
-- SCÉNARIO 3 : CAPACITÉ INSUFFISANTE (RG8 - remplissage progressif)
-- Date de test : 2026-03-28
-- =================================================================

-- Réservations non assignées volumineuses
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(9, 'BIG-NON-ASSIGNEE', '2026-03-27 07:00:00', 8, 2);  -- 8 pers non assignée

-- Nouvelles réservations
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(10, 'CLIENT-010', '2026-03-28 10:00:00', 6, 3),
(11, 'CLIENT-011', '2026-03-28 10:15:00', 4, 4);

-- Attendu :
-- Phase 1 : BIG-NON-ASSIGNEE (8 pers) prend BUS-001 (10 places) = 8 pers
-- Phase 2 : CLIENT-010 (6 pers) prend BUS-003 (12 places) = 6 pers
-- Phase 3 : CLIENT-011 (4 pers) pourrait être assignée au BUS-001 (2 places restantes) OU nouveau véhicule

-- =================================================================
-- INSTRUCTIONS DE TEST
-- =================================================================

-- 1. Exécuter ce script pour charger les données
-- 2. Lancer l'application et planifier pour 2026-03-26
-- 3. Vérifier les assignations dans la base :
--    SELECT a.*, r.client_id, r.date_heure_arrivee, r.nbr_pers
--    FROM Assignation a
--    JOIN Reservation r ON a.reservation_id = r.idReservation
--    WHERE DATE(a.date_heure_planification) = '2026-03-26'
--    ORDER BY a.vehicule_id, a.date_heure_planification;

-- 4. Vérifier que les non assignées sont bien prioritaires
-- 5. Tester les autres dates pour valider les règles RG7/RG8/RG9/RG11

-- =================================================================
-- NETTOYAGE FINAL (optionnel)
-- =================================================================
-- Pour relancer les tests :
-- TRUNCATE TABLE Assignation CASCADE;
-- DELETE FROM Reservation WHERE idReservation >= 1;