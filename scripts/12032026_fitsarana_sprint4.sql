INSERT INTO Hotel (nom, code, libelle) VALUES
('Aéroport International Ivato', 'AERO', 'aeroport'),
('Hotel Carton Madagascar', 'H001', 'hotel');

INSERT INTO Parametre (vitesse_moyenne, temps_attente) VALUES (50, 0);

INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 12, 50); 


INSERT INTO Vehicule (reference, nbr_places, type_carburant) VALUES
('VH-001-D', 12, 'D'),   
('VH-002-E', 5, 'ES'),   
('VH-003-D', 5, 'D'), 
('VH-004-E', 12, 'ES');   

INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
('CLIENT-001', '2026-03-12 09:00:00', 7, 2),
('CLIENT-002', '2026-03-12 09:00:00', 11, 2),
('CLIENT-003', '2026-03-12 09:00:00', 3, 2),
('CLIENT-004', '2026-03-12 09:00:00', 1, 2),
('CLIENT-005', '2026-03-12 09:00:00', 2, 2),
('CLIENT-006', '2026-03-12 09:00:00', 20, 2);
