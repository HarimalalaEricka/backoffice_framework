@echo off
echo ===========================================
echo SPRINT 8 - TACHE 1 : TEST DE PRIORISATION
echo ===========================================
echo.

echo Étape 1 : Configuration des données de test...
echo Execution du script SQL de test...
psql -h localhost -U postgres -d gestion_ticket -f "scripts\test_sprint8_tache1.sql"
if %errorlevel% neq 0 (
    echo ❌ Erreur lors de l'execution du script SQL
    pause
    exit /b 1
)
echo ✅ Données de test configurées
echo.

echo Étape 2 : Compilation du projet...
javac -cp "WebContent/WEB-INF/lib/*;." src/com/back/*.java src/com/back/*/*.java src/com/back/*/*/*.java
if %errorlevel% neq 0 (
    echo ❌ Erreur de compilation
    pause
    exit /b 1
)
echo ✅ Compilation réussie
echo.

echo Étape 3 : Execution du test SPRINT 8 - TACHE 1...
java -cp "WebContent/WEB-INF/lib/*;." com.app.Main
if %errorlevel% neq 0 (
    echo ❌ Erreur lors de l'execution
    pause
    exit /b 1
)
echo ✅ Test terminé
echo.

echo Étape 4 : Vérification finale des résultats...
echo Execution des requêtes de vérification...
psql -h localhost -U postgres -d gestion_ticket -c "
SELECT 'VERIFICATION FINALE - Ordre d''assignation :' as info;
SELECT r.date_heure_arrivee::time as heure, r.client_id, r.nbr_pers,
       a.vehicule_id, a.nb_pers_assigne
FROM reservation r
LEFT JOIN assignation a ON r.idReservation = a.reservation_id
WHERE DATE(r.date_heure_arrivee) = '2026-04-01'
ORDER BY a.date_heure_planification ASC NULLS LAST, r.date_heure_arrivee ASC;
"
echo.

echo ===========================================
echo TEST TERMINÉ !
echo Vérifiez que l'ordre d'assignation respecte :
echo 1. 06:00 (3 pers) - PRIORITÉ
echo 2. 06:45 (2 pers) - PRIORITÉ
echo 3. 06:50 (1 pers) - PRIORITÉ
echo 4. 08:00 (5 pers) - GROUPE SUIVANT
echo 5. 08:10 (4 pers) - GROUPE SUIVANT
echo 6. 08:15 (3 pers) - GROUPE SUIVANT
echo ===========================================
pause