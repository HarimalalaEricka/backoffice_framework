
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente) VALUES
(1, 50, 30);


INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aeroport Ivato', 'AIV', 'aeroport'),
(2, 'Hotel1', 'HT1', 'hotel');


INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 40),
(2, 1, 40);

INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES 
(1, 'VH-001-D', 8, 'D'),
(2, 'VH-002-D', 3, 'ES');


INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(1, 'r1', '2026-03-25 09:00:00', 6, 2),
(2, 'r2', '2026-03-25 09:20:00', 4, 2),
(3, 'r3', '2026-03-25 09:10:00', 1, 2);

-- Resultat attendu :
/*
    V1 : r1 (6 pers) + r2 (2 pers) = 8 pers -> complet
    V2 : r2 (2 pers restants) + r3 (3 pers) = 5 pers -> complet
    Non assigné : r3 (2 pers) -> pas de véhicule dispo 
*/
