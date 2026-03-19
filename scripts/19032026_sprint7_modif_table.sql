
\c gestion_ticket

BEGIN;

-- 1) Ajouter la colonne nb_pers_assigne avec valeur temporaire
ALTER TABLE Assignation
ADD COLUMN IF NOT EXISTS nb_pers_assigne INTEGER;

-- 2) Initialiser les anciennes lignes avec la valeur complète de la réservation
UPDATE Assignation a
SET nb_pers_assigne = r.nbr_pers
FROM Reservation r
WHERE a.reservation_id = r.idReservation
  AND a.nb_pers_assigne IS NULL;

-- 3) Rendre la colonne obligatoire + contrainte positive
ALTER TABLE Assignation
ALTER COLUMN nb_pers_assigne SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'assignation_nb_pers_assigne_check'
    ) THEN
        ALTER TABLE Assignation
        ADD CONSTRAINT assignation_nb_pers_assigne_check CHECK (nb_pers_assigne > 0);
    END IF;
END $$;

-- 4) Supprimer la contrainte d'unicité sur reservation_id (si présente)
DO $$
DECLARE
    unique_constraint_name TEXT;
BEGIN
    SELECT con.conname INTO unique_constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
    WHERE rel.relname = 'assignation'
      AND con.contype = 'u'
      AND att.attname = 'reservation_id'
    LIMIT 1;

    IF unique_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE Assignation DROP CONSTRAINT %I', unique_constraint_name);
    END IF;
END $$;

COMMIT;


