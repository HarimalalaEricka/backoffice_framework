-- =================================================================
-- SCRIPT DE TEST POUR LA PLANIFICATION JOUR J
-- Sprint 3 - Tests unitaires des différents cas métier
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
-- DONNÉES DE BASE (Hôtels et Paramètres)
-- =================================================================

-- Hôtels
INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aéroport Ivato', 'AIV', 'aeroport'),
(2, 'Hotel Carlton', 'HCT', 'hotel'),
(3, 'Hotel Colbert', 'HCB', 'hotel'),
(4, 'Hotel Ibis', 'HIB', 'hotel'),
(5, 'Hotel Panorama', 'HPN', 'hotel');

-- Paramètres
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente) VALUES
(1, 40, 15);

-- Distances
INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 25),
(1, 3, 20),
(1, 4, 18),
(1, 5, 30),
(2, 3, 5),
(2, 4, 8),
(2, 5, 10),
(3, 4, 6),
(3, 5, 12),
(4, 5, 15);

-- =================================================================
-- CAS 1 : CAS SIMPLE - 1 groupe, 1 véhicule disponible
-- Date de test : 2026-03-10
-- Attendu : Le véhicule V001 est assigné au groupe de 5 personnes
-- =================================================================

-- Véhicule pour Cas 1
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(1, 'V001-SIMPLE', 8, 'D');

-- Réservations pour Cas 1 (même vol = même date_heure_arrivee)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(1, 'CLIENT001', '2026-03-10 10:00:00', 3, 2),
(2, 'CLIENT002', '2026-03-10 10:00:00', 2, 3);
-- Total : 5 personnes, 1 groupe (vol de 10h00)
-- Véhicule V001-SIMPLE (8 places) doit être assigné

-- =================================================================
-- CAS 2 : CAPACITÉ ÉGALE - 2 véhicules même capacité → Diesel choisi
-- Date de test : 2026-03-11
-- Attendu : V002-DIESEL (Diesel) est choisi plutôt que V003-ESSENCE
-- =================================================================

-- Véhicules pour Cas 2 (même capacité, carburants différents)
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(2, 'V002-DIESEL', 6, 'D'),    -- Diesel → Prioritaire
(3, 'V003-ESSENCE', 6, 'ES');  -- Essence → Non prioritaire

-- Réservations pour Cas 2
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(3, 'CLIENT003', '2026-03-11 14:00:00', 4, 2),
(4, 'CLIENT004', '2026-03-11 14:00:00', 2, 4);
-- Total : 6 personnes, 1 groupe (vol de 14h00)
-- Les 2 véhicules ont 6 places → V002-DIESEL doit être choisi (Diesel prioritaire)

-- =================================================================
-- CAS 3 : PAS ASSEZ DE CAPACITÉ - Réservation → Non assignée
-- Date de test : 2026-03-12
-- Attendu : La réservation de 15 personnes reste non assignée
-- =================================================================

-- Véhicules pour Cas 3 (capacité insuffisante)
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(4, 'V004-PETIT', 4, 'D'),
(5, 'V005-MOYEN', 8, 'ES');

-- Réservations pour Cas 3
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(5, 'CLIENT005', '2026-03-12 09:00:00', 15, 2);
-- Total : 15 personnes, 1 groupe
-- Aucun véhicule avec 15+ places → Réservation non assignée

-- =================================================================
-- CAS 4 : PLUSIEURS VOLS - Véhicules distincts assignés
-- Date de test : 2026-03-13
-- Attendu : V006 pour vol 08h00, V007 pour vol 12h00, V008 pour vol 18h00
-- =================================================================

-- Véhicules pour Cas 4
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(6, 'V006-VOL1', 10, 'D'),
(7, 'V007-VOL2', 8, 'ES'),
(8, 'V008-VOL3', 6, 'EL');

-- Réservations pour Cas 4 (3 vols différents = 3 groupes)
-- Vol 1 : 08h00 → 7 personnes
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(6, 'CLIENT006', '2026-03-13 08:00:00', 4, 2),
(7, 'CLIENT007', '2026-03-13 08:00:00', 3, 3);

-- Vol 2 : 12h00 → 5 personnes
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(8, 'CLIENT008', '2026-03-13 12:00:00', 3, 4),
(9, 'CLIENT009', '2026-03-13 12:00:00', 2, 5);

-- Vol 3 : 18h00 → 4 personnes
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(10, 'CLIENT010', '2026-03-13 18:00:00', 4, 2);

-- Attendu :
-- - Groupe 08h00 (7 pers) → V006-VOL1 (10 places, Diesel)
-- - Groupe 12h00 (5 pers) → V008-VOL3 (6 places, plus proche capacité)
-- - Groupe 18h00 (4 pers) → V007-VOL2 (8 places, restant)

-- =================================================================
-- CAS 5 : RÉSERVATION DÉJÀ ASSIGNÉE - Ignorée
-- Date de test : 2026-03-14
-- Attendu : Réservation 11 déjà assignée, seule réservation 12 est traitée
-- =================================================================

-- Véhicules pour Cas 5
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(9, 'V009-DEJA', 8, 'D'),
(10, 'V010-NOUVEAU', 6, 'ES');

-- Réservations pour Cas 5
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(11, 'CLIENT011', '2026-03-14 10:00:00', 3, 2),  -- Déjà assignée
(12, 'CLIENT012', '2026-03-14 10:00:00', 2, 3);  -- À assigner

-- Assignation existante (réservation 11 déjà assignée à V009)
INSERT INTO Assignation (idAssignation, reservation_id, vehicule_id, date_heure_planification) VALUES
(1, 11, 9, '2026-03-14 00:00:00');

-- Attendu :
-- - Réservation 11 : Ignorée (déjà assignée)
-- - Réservation 12 : Assignée à V010-NOUVEAU

-- =================================================================
-- CAS 6 : VÉHICULE DÉJÀ UTILISÉ - Non réutilisé
-- Date de test : 2026-03-15
-- Attendu : V011 déjà utilisé, V012 doit être choisi pour le 2ème groupe
-- =================================================================

-- Véhicules pour Cas 6
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(11, 'V011-UTILISE', 8, 'D'),
(12, 'V012-LIBRE', 8, 'ES');

-- Réservations pour Cas 6 (2 vols différents)
-- Vol 1 : 09h00 (déjà assigné)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(13, 'CLIENT013', '2026-03-15 09:00:00', 4, 2);

-- Vol 2 : 15h00 (à assigner)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(14, 'CLIENT014', '2026-03-15 15:00:00', 3, 3);

-- V011 déjà utilisé ce jour (09h00)
INSERT INTO Assignation (idAssignation, reservation_id, vehicule_id, date_heure_planification) VALUES
(2, 13, 11, '2026-03-15 00:00:00');

-- Attendu :
-- - Vol 15h00 : V011-UTILISE ne peut pas être réutilisé
-- - V012-LIBRE doit être assigné même si ES (Diesel déjà pris)

-- =================================================================
-- RÉSUMÉ DES CAS DE TEST
-- =================================================================
/*
| Cas | Date       | Description                          | Résultat attendu                    |
|-----|------------|--------------------------------------|-------------------------------------|
| 1   | 2026-03-10 | 1 groupe, 1 véhicule                 | V001-SIMPLE assigné                 |
| 2   | 2026-03-11 | 2 véhicules même capacité            | V002-DIESEL choisi (Diesel prio)    |
| 3   | 2026-03-12 | Capacité insuffisante                | Réservation non assignée            |
| 4   | 2026-03-13 | 3 vols différents                    | 3 véhicules distincts assignés      |
| 5   | 2026-03-14 | Réservation déjà assignée            | Seule resa 12 traitée               |
| 6   | 2026-03-15 | Véhicule déjà utilisé                | V012-LIBRE assigné (V011 exclu)     |
*/

-- =================================================================
-- REQUÊTES DE VÉRIFICATION
-- =================================================================

-- Vérifier les réservations par date
SELECT 
    DATE(date_heure_arrivee) as date_vol,
    COUNT(*) as nb_reservations,
    SUM(nbr_pers) as total_personnes
FROM Reservation
GROUP BY DATE(date_heure_arrivee)
ORDER BY date_vol;

-- Vérifier les véhicules disponibles
SELECT * FROM Vehicule ORDER BY idVehicule;

-- Vérifier les assignations existantes
SELECT 
    a.idAssignation,
    r.client_id,
    r.date_heure_arrivee,
    v.reference as vehicule,
    a.date_heure_planification
FROM Assignation a
JOIN Reservation r ON a.reservation_id = r.idReservation
JOIN Vehicule v ON a.vehicule_id = v.idVehicule
ORDER BY a.date_heure_planification;

-- =================================================================
-- COMMANDES DE TEST (à exécuter après le script)
-- =================================================================
/*
-- Test Cas 1 (curl ou browser)
curl -X POST "http://localhost:8080/api/planification?date=2026-03-10" \
     -H "Authorization: Bearer <token>"

-- Test Cas 2
curl -X POST "http://localhost:8080/api/planification?date=2026-03-11" \
     -H "Authorization: Bearer <token>"

-- Test Cas 3
curl -X POST "http://localhost:8080/api/planification?date=2026-03-12" \
     -H "Authorization: Bearer <token>"

-- Test Cas 4
curl -X POST "http://localhost:8080/api/planification?date=2026-03-13" \
     -H "Authorization: Bearer <token>"

-- Test Cas 5
curl -X POST "http://localhost:8080/api/planification?date=2026-03-14" \
     -H "Authorization: Bearer <token>"

-- Test Cas 6
curl -X POST "http://localhost:8080/api/planification?date=2026-03-15" \
     -H "Authorization: Bearer <token>"
*/