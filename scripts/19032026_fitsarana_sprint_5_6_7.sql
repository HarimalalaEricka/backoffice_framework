
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente) VALUES
(1, 50, 30);


INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aeroport Ivato', 'AIV', 'aeroport'),
(2, 'HotelSimulation', 'HT2', 'simulation'),
(3, 'Hotel1', 'HT1', 'hotel'),
(4, 'Hotel2', 'HT2', 'hotel');


INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 50),
(2, 1, 50),
(1, 3, 90),
(3, 1, 90),
(1, 4, 35),
(4, 1, 35),
(3, 4, 60),
(4, 3, 60);


INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES 
(1, 'VH-001', 5, 'D'),
(2, 'VH-002', 5, 'ES'),
(3, 'VH-003', 12, 'D'),
(4, 'VH-004', 9, 'D'),
(5, 'VH-005', 12, 'ES');

-- Simulation de heure d'arrivee de 2 voiture a l'hotel
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(1, 'sim1', '2026-04-29 11:00:00', 1, 2),
(2, 'sim1', '2026-04-29 06:00:00', 1, 2),
(3, 'sim1', '2026-04-29 07:00:00', 1, 2),
(4, 'sim1', '2026-04-29 07:00:00', 1, 2),
(5, 'sim1', '2026-04-29 07:00:00', 1, 2);

INSERT INTO Assignation(reservation_id, vehicule_id, date_heure_planification, nb_pers_assigne) VALUES
(1, 5, '2026-04-29 00:00:00', 1),
(2, 3, '2026-04-29 00:00:00', 1),
(3, 1, '2026-04-29 00:00:00', 1),
(4, 2, '2026-04-29 00:00:00', 1),
(5, 4, '2026-04-29 00:00:00', 1);

INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(6, 'c1', '2026-04-29 09:00:00', 7, 3),
(7, 'c2', '2026-04-29 08:00:00', 20, 4),
(8, 'c3', '2026-04-29 09:10:00', 3, 3),
(9, 'c4', '2026-04-29 09:15:00', 10, 3),
(10, 'c5', '2026-04-29 09:20:00', 5, 3),
(11, 'c6', '2026-04-29 13:30:00', 12, 3);
