-- =================================================================
-- SCRIPT DE TEST POUR LA TÂCHE 1 - SPRINT 8
-- Priorisation des réservations non assignées dans le prochain groupe
--
-- Règle métier :
-- - Identifier toutes les réservations non assignées
-- - Les assigner au groupe d'intervalle suivante
-- - Les réservations non assignées seront assignées en premiers (priorité)
--
-- Date de test : 2026-04-01
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
(1, 'BUS-001', 20, 'D'),   -- Capacité augmentée pour le test
(2, 'BUS-002', 15, 'D'),   -- Capacité augmentée pour le test
(3, 'BUS-003', 12, 'ES'),
(4, 'BUS-004', 10, 'D');

-- =================================================================
-- SCÉNARIO PRINCIPAL : TÂCHE 1 - Priorisation des non assignées
-- Date de test : 2026-04-01
--
-- Scénario EXACT de la tâche :
-- - 3 réservations non assignées : 06:00, 06:45, 06:50
-- - Nouveau groupe 8:00-8:30 : 08:00, 08:10, 08:15
-- - Temps d'attente = 30 min → Groupe 8:00-8:30
--
-- Ordre d'assignation ATTENDU :
-- 1. 06:00 (priorité - non assignée)
-- 2. 06:45 (priorité - non assignée)
-- 3. 06:50 (priorité - non assignée)
-- 4. 08:00 (groupe suivant)
-- 5. 08:10 (groupe suivant)
-- 6. 08:15 (groupe suivant)
-- =================================================================

-- RÉSERVATIONS NON ASSIGNÉES (aucune assignation - priorité absolue)
-- Ces réservations arrivent tôt et n'ont pas encore été assignées
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(1, 'CLIENT_0600', '2026-04-10 06:00:00', 3, 2),  -- 3 pers, Hotel Carlton
(2, 'CLIENT_0645', '2026-04-10 06:45:00', 2, 3),  -- 2 pers, Hotel Colbert
(3, 'CLIENT_0650', '2026-04-10 06:50:00', 1, 4);  -- 1 pers, Hotel Ibis

-- NOUVEAU GROUPE D'INTERVALLE (8:00 - 8:30)
-- Ces réservations arrivent dans le prochain groupe temporel
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(4, 'CLIENT_0800', '2026-04-10 08:00:00', 5, 3),  -- 5 pers, Hotel Colbert
(5, 'CLIENT_0810', '2026-04-10 08:10:00', 4, 2),  -- 4 pers, Hotel Carlton
(6, 'CLIENT_0815', '2026-04-10 08:15:00', 3, 4);  -- 3 pers, Hotel Ibis

-- =================================================================
-- RÉSULTAT ATTENDU APRÈS PLANIFICATION DU 01/04
-- =================================================================

-- PHASE 1 : Assignation des RÉSERVATIONS NON ASSIGNÉES (priorité absolue)
-- Ordre : 06:00 (3p) → 06:45 (2p) → 06:50 (1p)
-- Véhicule BUS-001 (20 places) prend les 3 non assignées = 6 pers

-- PHASE 2 : Assignation du GROUPE 8:00-8:30 selon RG7/RG11
-- RG7 : Tri par nombre passagers décroissant
-- 08:00 (5p), 08:10 (4p), 08:15 (3p)
-- BUS-001 (14 places restantes) prend 08:00 (5p) = 11 pers
-- BUS-002 (15 places) prend 08:10 (4p) + 08:15 (3p) = 7 pers

-- =================================================================
-- VÉRIFICATION DES RÉSULTATS
-- =================================================================

-- =================================================================
-- RÉSULTAT ATTENDU APRÈS PLANIFICATION DU 01/04
-- =================================================================

-- PHASE 1 : Assignation des RÉSERVATIONS NON ASSIGNÉES (priorité absolue)
-- Ordre : 06:00 (3p) → 06:45 (2p) → 06:50 (1p)
-- Véhicule BUS-001 (20 places) prend les 3 non assignées = 6 pers

-- PHASE 2 : Assignation du GROUPE 8:00-8:30 selon RG7/RG11
-- RG7 : Tri par nombre passagers décroissant
-- 08:00 (5p), 08:10 (4p), 08:15 (3p)
-- BUS-001 (14 places restantes) prend 08:00 (5p) = 11 pers
-- BUS-002 (15 places) prend 08:10 (4p) + 08:15 (3p) = 7 pers

-- =================================================================
-- VÉRIFICATION DES RÉSULTATS
-- =================================================================

-- Exécuter la planification pour le 01/04
-- SELECT * FROM Assignation WHERE DATE(date_heure_planification) = '2026-04-01' ORDER BY vehicule_id, reservation_id;

-- Vérifier l'ordre d'assignation :
-- BUS-001 devrait avoir : CLIENT_0600, CLIENT_0645, CLIENT_0650, CLIENT_0800
-- BUS-002 devrait avoir : CLIENT_0810, CLIENT_0815

-- =================================================================
-- REQUÊTES DE VÉRIFICATION
-- =================================================================

-- 1. État des réservations AVANT planification
SELECT 'ÉTAT AVANT PLANIFICATION :' as info;
SELECT r.idReservation, r.client_id, r.nbr_pers, r.date_heure_arrivee::time as heure,
       CASE WHEN a.reservation_id IS NULL THEN '🔴 NON ASSIGNÉE' ELSE '🟢 ASSIGNÉE' END as statut
FROM reservation r
LEFT JOIN assignation a ON r.idReservation = a.reservation_id
WHERE DATE(r.date_heure_arrivee) = '2026-04-01'
ORDER BY r.date_heure_arrivee ASC;

-- 2. Vérifier que la table assignation est vide au départ
SELECT 'VÉRIFICATION - Table assignation vide :' as info;
SELECT COUNT(*) as nb_assignations_existantes
FROM assignation
WHERE reservation_id IN (
    SELECT idReservation FROM reservation
    WHERE DATE(date_heure_arrivee) = '2026-04-01'
);

-- 3. APRÈS PLANIFICATION - Ordre d'assignation
SELECT 'ORDRE D''ASSIGNATION APRÈS PLANIFICATION :' as info;
SELECT r.date_heure_arrivee::time as heure, r.client_id, r.nbr_pers,
       a.vehicule_id, a.nb_pers_assigne,
       CASE WHEN r.date_heure_arrivee < '08:00:00' THEN '⭐ PRIORITÉ (Non assignée)' ELSE '📅 Groupe 08:00-08:30' END as categorie
FROM reservation r
LEFT JOIN assignation a ON r.idReservation = a.reservation_id
WHERE DATE(r.date_heure_arrivee) = '2026-04-01'
ORDER BY a.date_heure_planification ASC NULLS LAST, r.date_heure_arrivee ASC;

-- 4. Analyse de la priorisation
SELECT 'ANALYSE DE LA PRIORISATION :' as info;
SELECT
    CASE WHEN r.date_heure_arrivee < '08:00:00' THEN '⭐ NON ASSIGNÉES ORIGINAL' ELSE '📅 GROUPE 08:00-08:30' END as categorie,
    COUNT(*) as total_reservations,
    SUM(CASE WHEN a.reservation_id IS NOT NULL THEN 1 ELSE 0 END) as assignees
FROM reservation r
LEFT JOIN assignation a ON r.idReservation = a.reservation_id
WHERE DATE(r.date_heure_arrivee) = '2026-04-01'
GROUP BY CASE WHEN r.date_heure_arrivee < '08:00:00' THEN '⭐ NON ASSIGNÉES ORIGINAL' ELSE '📅 GROUPE 08:00-08:30' END;

-- 5. Détail par véhicule
SELECT 'DÉTAIL PAR VÉHICULE :' as info;
SELECT a.vehicule_id, v.reference,
       STRING_AGG(r.client_id || '(' || r.date_heure_arrivee::time || ')', ', ' ORDER BY a.date_heure_planification) as reservations,
       SUM(a.nb_pers_assigne) as total_passagers,
       v.nbr_places as capacite_vehicule
FROM assignation a
JOIN reservation r ON a.reservation_id = r.idReservation
LEFT JOIN vehicule v ON a.vehicule_id = v.idVehicule
WHERE DATE(r.date_heure_arrivee) = '2026-04-01'
GROUP BY a.vehicule_id, v.reference, v.nbr_places
ORDER BY a.vehicule_id;

-- =================================================================
-- INSTRUCTIONS DE TEST
-- =================================================================

-- 1. Exécuter ce script pour charger les données
-- 2. Lancer l'application et planifier pour 2026-04-01
-- 3. Vérifier les assignations dans la base avec les requêtes ci-dessus
-- 4. Vérifier que les non assignées (06:00, 06:45, 06:50) sont bien prioritaires
-- 5. Vérifier que l'ordre correspond à l'exemple de la tâche

-- =================================================================
-- NETTOYAGE FINAL (optionnel)
-- =================================================================
-- Pour relancer les tests :
-- TRUNCATE TABLE Assignation CASCADE;
-- DELETE FROM Reservation WHERE DATE(date_heure_arrivee) = '2026-04-01';

-- Exécuter la planification pour le 26/03
-- SELECT * FROM Assignation WHERE DATE(date_heure_planification) = '2026-03-26' ORDER BY vehicule_id, reservation_id;

-- Vérifier l'ordre d'assignation :
-- BUS-001 devrait avoir : NON-ASSIGNEE-001, NON-ASSIGNEE-002, CLIENT-003
-- BUS-002 devrait avoir : CLIENT-004, CLIENT-005

-- =================================================================
-- INSTRUCTIONS DE TEST
-- =================================================================

-- 1. Exécuter ce script pour charger les données
-- 2. Lancer l'application et planifier pour 2026-04-01
-- 3. Vérifier les assignations dans la base avec les requêtes ci-dessus
-- 4. Vérifier que les non assignées (06:00, 06:45, 06:50) sont bien prioritaires
-- 5. Vérifier que l'ordre correspond à l'exemple de la tâche :
--    06:00 → 06:45 → 06:50 → 08:00 → 08:10 → 08:15

-- =================================================================
-- NETTOYAGE FINAL (optionnel)
-- =================================================================
-- Pour relancer les tests :
-- TRUNCATE TABLE Assignation CASCADE;
-- DELETE FROM Reservation WHERE DATE(date_heure_arrivee) = '2026-04-01';