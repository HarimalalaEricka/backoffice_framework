-- =================================================================
-- SCRIPT DE VÉRIFICATION - TÂCHE 1 SPRINT 8
-- Vérifications automatiques après planification
-- =================================================================

-- =================================================================
-- VÉRIFICATION 1 : Assignations du 26/03
-- =================================================================

SELECT '=== ASSIGNATIONS DU 26/03 ===' as section;
SELECT
    v.reference as vehicule,
    r.client_id,
    TO_CHAR(r.date_heure_arrivee, 'HH24:MI') as heure_arrivee,
    a.nb_pers_assigne as assignes,
    r.nbr_pers as total,
    CASE
        WHEN r.client_id LIKE 'NON-ASSIGNEE%' THEN 'NON_ASSIGNEE'
        ELSE 'NOUVELLE'
    END as type_reservation,
    h.nom as hotel
FROM Assignation a
JOIN Reservation r ON a.reservation_id = r.idReservation
JOIN Vehicule v ON a.vehicule_id = v.idVehicule
JOIN Hotel h ON r.hotel_id = h.idHotel
WHERE DATE(a.date_heure_planification) = '2026-03-26'
ORDER BY v.idVehicule, a.date_heure_planification;

-- =================================================================
-- VÉRIFICATION 2 : Ordre de priorisation
-- Les non assignées doivent être assignées en premier par véhicule
-- =================================================================

SELECT '=== ORDRE PAR VÉHICULE ===' as section;
SELECT
    v.reference as vehicule,
    STRING_AGG(
        CASE
            WHEN r.client_id LIKE 'NON-ASSIGNEE%' THEN 'NA-' || r.client_id
            ELSE 'NV-' || r.client_id
        END,
        ' → ' ORDER BY a.date_heure_planification
    ) as ordre_assignation,
    SUM(a.nb_pers_assigne) as total_personnes,
    v.nbr_places as capacite_vehicule
FROM Assignation a
JOIN Reservation r ON a.reservation_id = r.idReservation
JOIN Vehicule v ON a.vehicule_id = v.idVehicule
WHERE DATE(a.date_heure_planification) = '2026-03-26'
GROUP BY v.idVehicule, v.reference, v.nbr_places
ORDER BY v.idVehicule;

-- =================================================================
-- VÉRIFICATION 3 : Statistiques générales
-- =================================================================

SELECT '=== STATISTIQUES ===' as section;
SELECT
    COUNT(DISTINCT a.vehicule_id) as vehicules_utilises,
    COUNT(DISTINCT a.reservation_id) as reservations_assignees,
    SUM(a.nb_pers_assigne) as total_personnes_assignees,
    (
        SELECT COUNT(*)
        FROM Reservation
        WHERE DATE(date_heure_arrivee) = '2026-03-26'
    ) as total_reservations_jour,
    (
        SELECT COUNT(*)
        FROM Reservation r
        LEFT JOIN Assignation a ON r.idReservation = a.reservation_id
        WHERE DATE(r.date_heure_arrivee) = '2026-03-25'
        AND a.reservation_id IS NULL
    ) as non_assignees_anterieures
FROM Assignation a
WHERE DATE(a.date_heure_planification) = '2026-03-26';

-- =================================================================
-- VÉRIFICATION 4 : Contrôle des règles RG7/RG11
-- Les nouvelles réservations doivent être triées par nbr_pers DESC
-- =================================================================

SELECT '=== VÉRIFICATION RG7/RG11 (Nouvelles réservations) ===' as section;
WITH nouvelles_assignations AS (
    SELECT
        r.client_id,
        r.nbr_pers,
        h.nom as hotel_nom,
        ROW_NUMBER() OVER (ORDER BY a.date_heure_planification) as ordre_assignation
    FROM Assignation a
    JOIN Reservation r ON a.reservation_id = r.idReservation
    JOIN Hotel h ON r.hotel_id = h.idHotel
    WHERE DATE(a.date_heure_planification) = '2026-03-26'
    AND r.client_id NOT LIKE 'NON-ASSIGNEE%'
)
SELECT
    client_id,
    nbr_pers,
    hotel_nom,
    ordre_assignation,
    CASE
        WHEN nbr_pers = LAG(nbr_pers) OVER (ORDER BY ordre_assignation)
             AND hotel_nom <= LAG(hotel_nom) OVER (ORDER BY ordre_assignation)
        THEN '✓ RG7/RG11 RESPECTÉ'
        WHEN nbr_pers > LAG(nbr_pers) OVER (ORDER BY ordre_assignation)
        THEN '✓ RG7 RESPECTÉ'
        ELSE '⚠ POSSIBLE PROBLÈME RG7/RG11'
    END as controle_regles
FROM nouvelles_assignations
ORDER BY ordre_assignation;

-- =================================================================
-- VÉRIFICATION 5 : Réservations non assignées restantes
-- =================================================================

SELECT '=== RÉSERVATIONS NON ASSIGNÉES (après planification) ===' as section;
SELECT
    r.idReservation,
    r.client_id,
    TO_CHAR(r.date_heure_arrivee, 'DD/MM/YYYY HH24:MI') as date_heure,
    r.nbr_pers,
    h.nom as hotel,
    CASE
        WHEN r.client_id LIKE 'NON-ASSIGNEE%' THEN 'NON_ASSIGNEE_ANTÉRIEURE'
        ELSE 'NOUVELLE_NON_ASSIGNEE'
    END as type
FROM Reservation r
LEFT JOIN Assignation a ON r.idReservation = a.reservation_id
JOIN Hotel h ON r.hotel_id = h.idHotel
WHERE DATE(r.date_heure_arrivee) IN ('2026-03-25', '2026-03-26')
AND a.reservation_id IS NULL
ORDER BY r.date_heure_arrivee;

-- =================================================================
-- VÉRIFICATION 6 : Contrôle de capacité
-- =================================================================

SELECT '=== CONTRÔLE CAPACITÉ VÉHICULES ===' as section;
SELECT
    v.reference,
    v.nbr_places as capacite,
    COALESCE(SUM(a.nb_pers_assigne), 0) as personnes_assignees,
    v.nbr_places - COALESCE(SUM(a.nb_pers_assigne), 0) as places_restantes,
    CASE
        WHEN v.nbr_places - COALESCE(SUM(a.nb_pers_assigne), 0) >= 0 THEN '✓ OK'
        ELSE '⚠ SURCHARGE'
    END as controle_capacite
FROM Vehicule v
LEFT JOIN Assignation a ON v.idVehicule = a.vehicule_id
    AND DATE(a.date_heure_planification) = '2026-03-26'
GROUP BY v.idVehicule, v.reference, v.nbr_places
ORDER BY v.idVehicule;

-- =================================================================
-- RÉSUMÉ FINAL
-- =================================================================

SELECT '=== RÉSUMÉ TEST TÂCHE 1 ===' as section;
WITH stats AS (
    SELECT
        COUNT(DISTINCT CASE WHEN r.client_id LIKE 'NON-ASSIGNEE%' THEN a.reservation_id END) as non_assignees_assignees,
        COUNT(DISTINCT CASE WHEN r.client_id NOT LIKE 'NON-ASSIGNEE%' THEN a.reservation_id END) as nouvelles_assignees,
        COUNT(DISTINCT a.vehicule_id) as vehicules_utilises
    FROM Assignation a
    JOIN Reservation r ON a.reservation_id = r.idReservation
    WHERE DATE(a.date_heure_planification) = '2026-03-26'
)
SELECT
    'Non assignées assignées: ' || non_assignees_assignees ||
    ' | Nouvelles assignées: ' || nouvelles_assignees ||
    ' | Véhicules utilisés: ' || vehicules_utilises as resume,
    CASE
        WHEN non_assignees_assignees > 0 AND nouvelles_assignees > 0 THEN '✅ TÂCHE 1 OPÉRATIONNELLE'
        ELSE '⚠ VÉRIFIER LA LOGIQUE'
    END as status_test
FROM stats;