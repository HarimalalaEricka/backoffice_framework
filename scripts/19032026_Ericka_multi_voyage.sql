
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente) VALUES
(1, 40, 30);


INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aeroport Ivato', 'AIV', 'aeroport'),
(2, 'Hotel1', 'HT1', 'hotel');


INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 40),
(2, 1, 40);


INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES
(1, 'VH-001-D', 8, 'D'),
(2, 'VH-002-ES', 8, 'ES');

-- =================================================================
-- 5) RESERVATIONS (2 groupes)
-- =================================================================
-- Groupe 1 : 09:00 -> 09:30 (depart effectif = 09:30)
-- Groupe 2 : 11:30 -> 12:00
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
-- Groupe 1
(1, 'S6-G1-C1', '2026-03-25 09:00:00', 3, 2),
(2, 'S6-G1-C2', '2026-03-25 09:20:00', 2, 2),

-- Groupe 2
(3, 'S6-G2-C1', '2026-03-25 11:00:00', 3, 2),
(4, 'S6-G2-C2', '2026-03-25 11:30:00', 2, 2);

-- =================================================================
-- 6) RESULTAT ATTENDU (apres appel planification sur 2026-03-20)
-- =================================================================
/*
Calcul attendu du Groupe 1 :
- Heure depart effective = 09:30
- Trajet total = 120 min
- Heure retour = 11:30

Regle Sprint 6 Tache 1 :
- Groupe 2 depart a 11:30
- Vehicule reutilisable si heure_retour <= heure_depart_groupe
- Donc VH-001-D est reutilisable car 11:30 <= 11:30

Attendu fonctionnel :
- Les 4 reservations sont assignees
- VH-001-D est utilise sur les 2 groupes (multi-voyages)
- VH-002-ES n'est pas utilise dans ce scenario
*/

-- =================================================================
-- 7) REQUETES DE VERIFICATION (a lancer apres planification)
-- =================================================================

-- Verifier les assignations creees
SELECT
    a.idAssignation,
    r.client_id,
    r.date_heure_arrivee,
    v.reference AS vehicule,
    v.type_carburant,
    a.date_heure_planification
FROM Assignation a
JOIN Reservation r ON r.idReservation = a.reservation_id
JOIN Vehicule v ON v.idVehicule = a.vehicule_id
ORDER BY r.date_heure_arrivee;

-- Verifier le nombre de reservations par vehicule
SELECT
    v.reference AS vehicule,
    COUNT(*) AS nb_reservations_assignees
FROM Assignation a
JOIN Vehicule v ON v.idVehicule = a.vehicule_id
GROUP BY v.reference
ORDER BY v.reference;

-- Verifier les reservations non assignees (doit etre 0)
SELECT
    r.idReservation,
    r.client_id,
    r.date_heure_arrivee
FROM Reservation r
LEFT JOIN Assignation a ON a.reservation_id = r.idReservation
WHERE a.idAssignation IS NULL
ORDER BY r.date_heure_arrivee;
