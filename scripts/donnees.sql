INSERT INTO Hotel (nom) VALUES ('Auberge du Parc');

-- Exemples de réservations (hotel_id = 1)
INSERT INTO Reservation (client_id, date_heure, nbr_pers, hotel_id) VALUES
  ('C001', '2026-02-10 14:00:00', 2, 1),
  ('C002', '2026-02-11 19:30:00', 4, 1),
  ('C003', '2026-02-12 09:00:00', 1, 1),
  ('C004', '2026-02-15 16:45:00', 3, 1),
  ('C005', '2026-03-01 12:15:00', 2, 1);

-- Vérifier
SELECT * FROM Hotel;
SELECT * FROM Reservation;