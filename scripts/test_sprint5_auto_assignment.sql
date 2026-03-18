-- =================================================================
-- SCRIPT DE TEST POUR L'ASSIGNATION AUTOMATIQUE SPRINT 5
-- Date de test : 2026-03-20
-- Test de la TACHE 1 : Assignation automatique des réservations non assignées
-- =================================================================

\c gestion_ticket

-- =================================================================
-- NETTOYAGE ET DONNÉES DE BASE
-- =================================================================
TRUNCATE TABLE Assignation CASCADE;
TRUNCATE TABLE Reservation CASCADE;
TRUNCATE TABLE Vehicule CASCADE;
TRUNCATE TABLE Hotel CASCADE;
TRUNCATE TABLE Distance CASCADE;
TRUNCATE TABLE Parametre CASCADE;

-- Hôtels
INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aéroport Ivato', 'AIV', 'aeroport'),
(2, 'Hotel Carlton', 'HCT', 'hotel');

-- Paramètres (temps_attente = 30 minutes pour créer des tranches)
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente) VALUES
(1, 40, 30);

-- Distances
INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 25),
(2, 1, 25);

-- Véhicules disponibles
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(1, 'V001-TEST', 10, 'D'),
(2, 'V002-TEST', 8, 'ES');

-- =================================================================
-- DONNÉES DE TEST POUR 2026-03-20
-- =================================================================

-- Réservations existantes (non assignées)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(22, 'CLIENT-021', '2026-03-15 20:00:00', 2, 2),  -- Date différente (pas concernée)
(23, 'TEST-C1001', '2026-03-20 06:00:00', 2, 2),   -- Non assignée
(24, 'TEST-C1002', '2026-03-20 06:45:00', 3, 2),   -- Non assignée
(25, 'TEST-C1003', '2026-03-20 07:30:00', 1, 2);   -- Non assignée

-- =================================================================
-- SCÉNARIO DE TEST
-- =================================================================
/*
Lors de la planification pour 2026-03-20 :

1. Grouper par tranches de 30 minutes :
   - Tranche 1: 06:00 - 06:30 → Réservations: TEST-C1001 (06:00), TEST-C1002 (06:45)
     → Total: 2 + 3 = 5 personnes
     → Intervalle créé: [06:00 - 06:45] (dernier vol = 06:45)

2. Pour cet intervalle [06:00 - 06:45] :
   - debutIntervalle = 06:00 (première réservation du groupe)
   - Chercher réservations non assignées avant 06:00
   - TEST-C1003 (07:30) arrive APRÈS 06:00 → NON incluse
   - Aucune réservation avant 06:00 → Groupe reste avec 5 personnes

3. Assignation :
   - V001-TEST (10 places) assigné au groupe de 5 personnes
   - TEST-C1003 (07:30) reste non assignée (nouveau groupe)

Résultat attendu :
- 2 réservations assignées (23, 24) à V001-TEST
- 1 réservation non assignée (25)
*/

-- =================================================================
-- INSTRUCTIONS DE TEST
-- =================================================================
/*
1. Exécuter ce script pour insérer les données de test
2. Compiler et exécuter Main.java pour lancer le test automatique
3. Vérifier que :
   - TEST-C1001 et TEST-C1002 sont assignées à V001-TEST
   - TEST-C1003 reste non assignée
   - Les logs montrent l'assignation automatique
*/