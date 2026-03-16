-- TEST HEURE DE DEPART SELON TEMPS D'ATTENTE

INSERT INTO Hotel (nom, code, libelle) VALUES
('Aéroport International Ivato', 'AERO', 'aeroport'),
('Hotel Carton Madagascar', 'H001', 'hotel');

INSERT INTO Parametre (vitesse_moyenne, temps_attente) VALUES (50, 30);

INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 50); 


INSERT INTO Vehicule (reference, nbr_places, type_carburant) VALUES
('VH-001-D', 12, 'D'),   
('VH-002-E', 5, 'ES'),   
('VH-003-D', 5, 'D'), 
('VH-004-E', 12, 'ES');  


INSERT INTO Reservation (client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
    ('CLIENT-001', '2026-03-20 09:00:00', 7, 2),
    ('CLIENT-002', '2026-03-20 09:05:00', 4, 2),
    ('CLIENT-004', '2026-03-20 09:10:00', 1, 2),
    ('CLIENT-005', '2026-03-20 09:11:00', 3, 2),
    ('CLIENT-006', '2026-03-20 09:15:00', 6, 2),
    ('CLIENT-008', '2026-03-20 09:20:00', 5, 2),
    ('CLIENT-010', '2026-03-20 09:28:00', 3, 2);

    -- ('CLIENT-014', '2026-03-20 11:30:00', 4, 2),
    -- ('CLIENT-015', '2026-03-20 11:40:00', 2, 2),
    -- ('CLIENT-016', '2026-03-20 11:50:00', 6, 2),
    -- ('CLIENT-017', '2026-03-20 12:00:00', 3, 2);