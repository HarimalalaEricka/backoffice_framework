-- Script de nettoyage des données pour toutes les tables principales
-- Désactive temporairement les contraintes de clé étrangère
SET session_replication_role = replica;

-- Nettoyage des tables dépendantes en premier (ordre des dépendances)
TRUNCATE TABLE Assignation RESTART IDENTITY CASCADE;
TRUNCATE TABLE Distance RESTART IDENTITY CASCADE;
TRUNCATE TABLE Reservation RESTART IDENTITY CASCADE;
TRUNCATE TABLE Vehicule RESTART IDENTITY CASCADE;
TRUNCATE TABLE Token RESTART IDENTITY CASCADE;
TRUNCATE TABLE Parametre RESTART IDENTITY CASCADE;
TRUNCATE TABLE Hotel RESTART IDENTITY CASCADE;

-- Réactive les contraintes de clé étrangère
SET session_replication_role = DEFAULT;

-- Fin du script de nettoyage