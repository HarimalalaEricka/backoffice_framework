-- Données de test pour Vehicule
INSERT INTO Vehicule (reference, nbr_places, type_carburant) VALUES
('V001-DIESEL', 5, 'D'),
('V002-ESSENCE', 4, 'ES'),
('V003-ELECTRIQUE', 5, 'EL'),
('V004-HYBRIDE', 7, 'H'),
('V005-DIESEL', 9, 'D'),
('V006-ELECTRIQUE', 2, 'EL'),
('V007-ESSENCE', 5, 'ES'),
('V008-HYBRIDE', 5, 'H');

-- Données de test pour Token
-- Token valide (expire dans 24h)
INSERT INTO Token (token, date_heure_expiration) VALUES
('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', TIMESTAMP '2026-03-03 12:00:00');

-- Token valide (expire dans 2h)
INSERT INTO Token (token, date_heure_expiration) VALUES
('f1e2d3c4-b5a6-4978-8fed-cba9876543210', TIMESTAMP '2026-03-02 16:00:00');

-- Token valide (expire dans 1 semaine)
INSERT INTO Token (token, date_heure_expiration) VALUES
('12345678-1234-1234-1234-123456789abc', TIMESTAMP '2026-03-09 10:00:00');

-- Token expiré (pour tester la vérification)
INSERT INTO Token (token, date_heure_expiration) VALUES
('expired-token-1234-5678-9abc-def012345678', TIMESTAMP '2026-03-01 10:00:00');

-- Vérification
SELECT * FROM Vehicule;
SELECT * FROM Token;
