-- =========================================================
-- Sprint 8 - Tâche 2 : Données de test (4 cas)
-- Base: gestion_ticket
-- Pré-requis: schéma créé (scripts/03032026.sql)
-- =========================================================

\c gestion_ticket

-- Nettoyage (attention: efface tout)
TRUNCATE TABLE Assignation CASCADE;
TRUNCATE TABLE Reservation CASCADE;
TRUNCATE TABLE Vehicule CASCADE;
TRUNCATE TABLE Distance CASCADE;
TRUNCATE TABLE Parametre CASCADE;
TRUNCATE TABLE Hotel CASCADE;

-- =========================================================
-- Données communes
-- - vitesse_moyenne = 60 km/h  => 1 km = 1 minute (approx exact ici)
-- - temps_attente = 30 minutes (fenêtre [t ; t+30])
-- =========================================================
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente) VALUES
(1, 60, 30);

-- Hôtels (1 = aéroport)
INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aéroport Ivato', 'AIV', 'aeroport'),
(2, 'Hotel D22 (aller/retour 22min)', 'H22', 'hotel'),
(3, 'Hotel D1 (aller/retour 1min)', 'H01', 'hotel');

-- Distances (bidirectionnelles via la requête OR du repo)
-- Hotel 2 : 22 km => 22 min par segment à 60 km/h, A/R = 44 min
-- Hotel 3 : 1 km  => 1 min par segment, A/R = 2 min
INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 22),
(1, 3, 1);

-- =========================================================
-- CAS 1 (2026-04-29)
-- RN(11) + RN(3) + fenêtre pour compléter 6 places -> véhicule plein
-- Attendu: véhicule dispo à 11:00, charge d'abord <=11:00 puis complète dans [11:00;11:30]
-- Exemple extension:
--   - Avant 11:00: R_C1_A=11 (10:00), R_C1_B=3 (10:05)
--   - Fenêtre: R_C1_C=4 (11:10), R_C1_D=2 (11:20)
--   - Véhicule 20 places => total 20 (plein), départ attendu ~11:20 (dernier vol chargé)
-- =========================================================
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(101, 'V-S8-CAS1-20P', 20, 'D');

-- SIM pour fixer l'heureRetour du véhicule à 11:00
-- Départ = max(arrivée SIM) = 10:16
-- Hotel 2 (22km): aller 22min + retour 22min => retour à 11:00
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(1001, 'SIM-C1', '2026-04-29 10:16:00', 1, 2);

INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES
(1001, 101, '2026-04-29 00:00:00', 1);

-- Réservations non assignées (avant dispo)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(1002, 'C1-RN-11', '2026-04-29 10:00:00', 11, 2),
(1003, 'C1-RN-3',  '2026-04-29 10:05:00',  3, 2);

-- Réservations dans la fenêtre [11:00; 11:30]
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(1004, 'C1-WIN-4', '2026-04-29 11:10:00', 4, 2),
(1005, 'C1-WIN-2', '2026-04-29 11:20:00', 2, 2);

-- =========================================================
-- CAS 2 (2026-04-29)
-- Véhicule dispo 09:45, fenêtre [09:45;10:15]
-- Attendu: ne PAS attendre 10:40, prendre les vols 10:00/10:10 si capacité
-- Extension: on met une RN avant 09:45 pour tester la priorité
--   - Priorité <=09:45: C2-RN-2 (08:30)
--   - Fenêtre: C2-10:00 (4) + C2-10:10 (4) -> complète
--   - Après fenêtre: C2-10:40 (5) doit rester pour un autre regroupement
-- =========================================================
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(102, 'V-S8-CAS2-10P', 10, 'ES');

-- SIM pour fixer l'heureRetour du véhicule à 09:45
-- Départ = 09:01, A/R = 44 min => 09:45
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(2001, 'SIM-C2', '2026-04-29 09:01:00', 1, 2);

INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES
(2001, 102, '2026-04-29 00:00:00', 1);

-- RN avant dispo (doit être prise en priorité à 09:45)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(2002, 'C2-RN-2', '2026-04-29 08:30:00', 2, 2);

-- Fenêtre [09:45;10:15]
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(2003, 'C2-10H00-4', '2026-04-29 10:00:00', 4, 2),
(2004, 'C2-10H10-4', '2026-04-29 10:10:00', 4, 2);

-- Après fenêtre
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(2005, 'C2-10H40-5', '2026-04-29 10:40:00', 5, 2);

-- =========================================================
-- CAS 3 (2026-04-30)
-- Après split, la prochaine assignation doit traiter d'abord le plus gros restant
-- Attendu:
--   - À 12:00: prendre R2(12) d'abord -> split 11 + reste 1
--   - Puis R1(11) avant le reste(1)
-- Extension: distance 1km => le véhicule revient vite et peut repartir plusieurs fois
-- =========================================================
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(103, 'V-S8-CAS3-11P', 11, 'D');

-- SIM pour fixer l'heureRetour à 12:00
-- Départ = 11:58, Hotel 3 (A/R 2 min) => retour 12:00
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(3001, 'SIM-C3', '2026-04-30 11:58:00', 1, 3);

INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES
(3001, 103, '2026-04-30 00:00:00', 1);

-- Deux grosses réservations avant 12:00 (doivent rester non assignées tant que véhicule occupé)
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(3002, 'C3-R1-11', '2026-04-30 11:30:00', 11, 3),
(3003, 'C3-R2-12', '2026-04-30 11:30:00', 12, 3);

-- Réservation tardive pour garantir un "prochain groupe" après 12:00
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(3004, 'C3-LATE-1', '2026-04-30 13:00:00', 1, 3);

-- =========================================================
-- CAS 4 (2026-04-30)
-- Véhicule entamé: reste 2 places => choisir la réservation la plus proche (2) pas (3)
-- Attendu:
--   - À 15:00: charger R1(11) puis choisir R3(2) (delta 0) plutôt que R2(3)
--   - R2(3) reste pour un départ suivant (le véhicule revient vite, donc possible dans la même boucle)
-- =========================================================
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(104, 'V-S8-CAS4-13P', 13, 'D');

-- SIM pour fixer l'heureRetour à 15:00
-- Départ = 14:58, A/R 2 min => retour 15:00
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(4001, 'SIM-C4', '2026-04-30 14:58:00', 1, 3);

INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES
(4001, 104, '2026-04-30 00:00:00', 1);

-- Non assignées avant 15:00
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(4002, 'C4-R1-11', '2026-04-30 14:00:00', 11, 3),
(4003, 'C4-R2-3',  '2026-04-30 14:05:00',  3, 3),
(4004, 'C4-R3-2',  '2026-04-30 14:10:00',  2, 3);

-- Réservation tardive pour garantir un prochain groupe
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(4005, 'C4-LATE-1', '2026-04-30 16:00:00', 1, 3);


-- =========================================================
-- CAS 5 (2026-04-30)
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(105, 'V-S8-CAS5-12P', 12, 'D');

-- SIM : fixe heureRetour de V1 à 09:50
-- départ = 09:06, Hotel D22 (22km, A/R=44min) => retour = 09:50
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(5001, 'SIM-C5',    '2026-05-01 09:06:00',  1, 2);
 
INSERT INTO Assignation (reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES
(5001, 105, '2026-05-01 00:00:00', 1);
 
-- R1 : non assignée, arrivée 09:30 (avant dispo V1 à 09:50)
-- Sera récupérée par Sprint5-Tâche1 lors du traitement du groupe [10:00;10:30]
-- car arrivée 09:30 < debutIntervalle 10:00
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(5002, 'C5-R1-7-NA',  '2026-05-01 08:30:00',  7, 2);
-- Pas d'Assignation pour 5002 → elle reste "non assignée"
 
-- Réservations dans la fenêtre [10:00 ; 10:30]
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(5003, 'C5-R2-10',    '2026-05-01 10:00:00', 10, 2),
(5004, 'C5-R3-7',     '2026-05-01 10:10:00',  7, 2),
(5005, 'C5-R4-3',     '2026-05-01 10:15:00',  3, 2);
 
-- Réservation tardive : force un 3e groupe [11:00;11:30]
-- Permet à V1 (retour=10:44) d'être disponible pour un 2e voyage
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(5006, 'C5-LATE-1',   '2026-05-01 11:00:00',  1, 2);
 

-- =========================================================
-- REQUÊTES DE CONTRÔLE (avant exécution de la planification)
-- =========================================================
-- Vérifier paramètres
SELECT * FROM Parametre;

-- Vérifier distances
SELECT * FROM Distance ORDER BY from_hotel_id, to_hotel_id;

-- Vérifier données par date
SELECT DATE(date_heure_arrivee) AS d, COUNT(*) AS nb_resa, SUM(nbr_pers) AS pax
FROM Reservation
GROUP BY DATE(date_heure_arrivee)
ORDER BY d;

-- Vérifier que les SIM sont bien déjà assignées (restants=0 pour elles)
SELECT r.idReservation, r.client_id, r.nbr_pers,
       COALESCE(SUM(a.nb_pers_assigne),0) AS deja_assigne,
       (r.nbr_pers - COALESCE(SUM(a.nb_pers_assigne),0)) AS restants
FROM Reservation r
LEFT JOIN Assignation a ON a.reservation_id = r.idReservation
WHERE r.idReservation IN (1001,2001,3001,4001)
GROUP BY r.idReservation, r.client_id, r.nbr_pers
ORDER BY r.idReservation;

-- =========================================================
-- APRÈS avoir lancé planifierJour(date) via l'API/UI,
-- utilise ces requêtes pour vérifier le "restants" par réservation.
-- (Note: date_heure_planification est toujours à 00:00 dans votre code)
-- =========================================================
-- Exemple: remplacer la date selon le cas
-- 2026-04-29 (cas 1), 2026-04-29 (cas 2), 2026-04-30 (cas 3), 2026-04-30 (cas 4)

-- Vérif restants par réservation (pour une date donnée)
-- SELECT r.idReservation, r.client_id, r.date_heure_arrivee, r.nbr_pers,
--        COALESCE(SUM(a.nb_pers_assigne),0) AS assigned,
--        (r.nbr_pers - COALESCE(SUM(a.nb_pers_assigne),0)) AS restants
-- FROM Reservation r
-- LEFT JOIN Assignation a ON a.reservation_id = r.idReservation
-- WHERE DATE(r.date_heure_arrivee) = DATE '2026-04-29'
-- GROUP BY r.idReservation, r.client_id, r.date_heure_arrivee, r.nbr_pers
-- ORDER BY r.date_heure_arrivee, r.idReservation;

-- Vérif totaux assignés par véhicule (pour une date donnée)
-- SELECT a.vehicule_id, v.reference, SUM(a.nb_pers_assigne) AS pax_assigned
-- FROM Assignation a
-- JOIN Vehicule v ON v.idVehicule = a.vehicule_id
-- JOIN Reservation r ON r.idReservation = a.reservation_id
-- WHERE DATE(r.date_heure_arrivee) = DATE '2026-04-29'
-- GROUP BY a.vehicule_id, v.reference
-- ORDER BY a.vehicule_id;