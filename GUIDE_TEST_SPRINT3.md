# 🧪 GUIDE DE TEST - SPRINT 3
## Réutilisation des Véhicules & Calcul de Trajet

**Date:** 10 Mars 2026  
**Système:** Gestion de Tickets avec Planification Automatique

---

## 📋 TABLE DES MATIÈRES

1. [Prérequis](#prérequis)
2. [Installation de la Base de Données](#installation-de-la-base-de-données)
3. [Test Automatique (Java)](#test-automatique-java)
4. [Test Manuel (Interface Web)](#test-manuel-interface-web)
5. [Scénarios de Test](#scénarios-de-test)
6. [Résultats Attendus](#résultats-attendus)

---

## 🔧 PRÉREQUIS

### Logiciels nécessaires
- ✅ PostgreSQL (version 12+)
- ✅ Java JDK 8+
- ✅ Apache Tomcat (ou serveur web compatible)
- ✅ Framework compilé

### Vérification
```cmd
# Vérifier PostgreSQL
psql --version

# Vérifier Java
java -version
javac -version
```

---

## 💾 INSTALLATION DE LA BASE DE DONNÉES

### Étape 1 : Créer la structure de la base

```cmd
cd D:\S5\Framework\Framework\fram\backoffice_framework\scripts

# Sous Windows avec PostgreSQL
psql -U postgres -f 03032026.sql
```

**OU via psql interactif:**
```cmd
psql -U postgres
\i D:/S5/Framework/Framework/fram/backoffice_framework/scripts/03032026.sql
\q
```

### Étape 2 : Charger les données de test

```cmd
# Charger les données de test Sprint 3
psql -U postgres -d gestion_ticket -f test_sprint3_data.sql
```

**OU via psql interactif:**
```cmd
psql -U postgres -d gestion_ticket
\i D:/S5/Framework/Framework/fram/backoffice_framework/scripts/test_sprint3_data.sql
\q
```

### Étape 3 : Vérifier les données

```sql
psql -U postgres -d gestion_ticket

-- Vérifier les hôtels
SELECT * FROM Hotel;

-- Vérifier les distances
SELECT * FROM Distance;

-- Vérifier les paramètres
SELECT * FROM Parametre;

-- Vérifier les véhicules
SELECT * FROM Vehicule ORDER BY nbr_places;

-- Vérifier les réservations
SELECT 
    date_heure_arrivee, 
    COUNT(*) as nb_reservations, 
    SUM(nbr_pers) as total_personnes 
FROM Reservation 
GROUP BY date_heure_arrivee 
ORDER BY date_heure_arrivee;

\q
```

### ✅ Résultat attendu
- **6 hôtels** (1 aéroport + 5 hôtels)
- **11 distances**
- **1 paramètre** (vitesse: 60 km/h, attente: 10 min)
- **8 véhicules** (capacités variées)
- **12 réservations** (6 vols différents)

---

## 🔬 TEST AUTOMATIQUE (JAVA)

### Compilation du test

```cmd
cd D:\S5\Framework\Framework\fram\backoffice_framework

# Compiler les classes
.\script.bat
```

### Exécution des tests

```cmd
# Exécuter le test Sprint 3
java -cp "bin;lib/*" com.back.test.TestSprint3
```

### Tests exécutés automatiquement

1. **Test 1** : Calcul de trajet simple (1 hôtel)
2. **Test 2** : Calcul de trajet avec plusieurs hôtels
3. **Test 3** : Vérification de la réutilisation de véhicule
4. **Test 4** : Planification complète avec réutilisation

### ✅ Résultats attendus

```
╔════════════════════════════════════════════════════════════╗
║  TEST SPRINT 3 - RÉUTILISATION VÉHICULES & CALCUL TRAJET  ║
╚════════════════════════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TEST 1 : Calcul de trajet simple (1 hôtel)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Test 1 réussi

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TEST 2 : Calcul de trajet avec 3+ hôtels
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Test 2 réussi

...
✅ Tous les tests sont terminés!
```

---

## 🌐 TEST MANUEL (INTERFACE WEB)

### Étape 1 : Démarrer le serveur

```cmd
cd D:\S5\Framework\Framework\fram\backoffice_framework

# Démarrer Tomcat (ou votre serveur)
# Le serveur devrait être accessible sur http://localhost:8080
```

### Étape 2 : Accéder à l'interface

1. Ouvrir un navigateur
2. Aller sur: `http://localhost:8080/backoffice/planification/form`

### Étape 3 : Planifier une date

1. Sélectionner la date: **10 Mars 2026** (2026-03-10)
2. Cliquer sur **"Planifier"**
3. Observer les résultats

### Étape 4 : Vérifier les résultats

La page de résultats devrait afficher pour chaque véhicule assigné :

**📍 Informations du véhicule:**
- Référence
- Capacité
- Type de carburant

**📍 Détails du trajet:**
- 🛫 Heure de départ
- 🛬 Heure de retour
- 📏 Distance totale (km)

**📍 Itinéraire détaillé:**
| Ordre | Hôtel | Heure d'arrivée | Distance segment | Distance cumulée |
|-------|-------|-----------------|------------------|------------------|
| ...   | ...   | ...             | ...              | ...              |

---

## 🎯 SCÉNARIOS DE TEST

### TEST 1 : Trajet Simple (Vol 10h00)
**Données:**
- 1 réservation : 3 personnes → Hotel Carton (15 km)

**Attendu:**
- ✅ 1 véhicule assigné (capacité 4, Diesel prioritaire)
- Distance totale : 30 km (aller-retour)
- Temps estimé : ~40 min (trajet + attente)
- Retour : ~10h40

---

### TEST 2 : Multi-Hôtels (Vol 11h00)
**Données:**
- 3 réservations : 8 personnes total → 3 hôtels différents
  - 2 pers → Lokanga (12 km)
  - 3 pers → Carton (15 km)
  - 3 pers → Radisson (18 km)

**Attendu:**
- ✅ 1 véhicule 12 places (peut transporter les 8 personnes)
- Itinéraire optimisé : Aéroport → Lokanga → Carton → Radisson → Aéroport
- Distance totale : calculée selon l'optimisation
- 3 arrêts visibles dans le détail

---

### TEST 3 : Réutilisation de Véhicule (Vol 13h00)
**Données:**
- 2 réservations : 4 personnes total
- Vol précédent (10h00) devrait être terminé

**Attendu:**
- ✅ Réutilisation du véhicule du vol de 10h00
- Le système vérifie que le véhicule est revenu avant 13h00
- Log : "Véhicule VH-XXX réutilisable (retour: XX:XX, vol actuel: 13:00)"

---

### TEST 4 : Groupe Trop Grand (Vol 14h30)
**Données:**
- 2 réservations : 25 personnes total
  - 15 pers → Novotel
  - 10 pers → Ibis

**Attendu:**
- ✅ 2 véhicules distincts assignés
- Véhicule 1 : 20 places pour 15 personnes
- Véhicule 2 : 12 places pour 10 personnes
- OU traitement individuel si pas assez de capacité

---

### TEST 5 : Capacité Optimale (Vol 16h00)
**Données:**
- 3 réservations : 12 personnes total (exactement)

**Attendu:**
- ✅ Véhicule 12 places Diesel (capacité exacte + priorité Diesel)
- Pas de gaspillage de capacité
- Pas de véhicule 20 places sélectionné

---

### TEST 6 : Distance Longue (Vol 17h00)
**Données:**
- 1 réservation : 6 personnes → Ibis (25 km, le plus éloigné)

**Attendu:**
- ✅ Distance totale : 50 km (aller-retour)
- Temps de trajet plus long
- Heure de retour calculée correctement

---

## 📊 RÉSULTATS ATTENDUS GLOBAUX

### Après planification du 10 Mars 2026

**Statistiques:**
- 🚗 Véhicules utilisés : 6-8 véhicules
- ✅ Réservations assignées : 12 réservations
- 👥 Personnes transportées : ~59 personnes
- ⚠️ Non assignées : 0 (toutes doivent être assignées)

**Vérifications clés:**
1. ✅ Aucun véhicule n'est utilisé deux fois au même moment
2. ✅ Les véhicules sont réutilisés si retournés à temps
3. ✅ Toutes les distances sont calculées correctement
4. ✅ Les itinéraires multi-arrêts sont optimisés
5. ✅ Les horaires de retour sont cohérents

---

## 🔍 VÉRIFICATION EN BASE DE DONNÉES

### Après la planification

```sql
psql -U postgres -d gestion_ticket

-- Vérifier les assignations créées
SELECT 
    a.idAssignation,
    r.client_id,
    r.nbr_pers,
    v.reference as vehicule,
    v.nbr_places,
    a.date_heure_planification
FROM Assignation a
JOIN Reservation r ON a.reservation_id = r.idReservation
JOIN Vehicule v ON a.vehicule_id = v.idVehicule
ORDER BY a.date_heure_planification, v.reference;

-- Compter les assignations par véhicule
SELECT 
    v.reference,
    v.nbr_places,
    COUNT(a.idAssignation) as nb_trajets,
    SUM(r.nbr_pers) as total_personnes
FROM Vehicule v
LEFT JOIN Assignation a ON v.idVehicule = a.vehicule_id
LEFT JOIN Reservation r ON a.reservation_id = r.idReservation
GROUP BY v.idVehicule, v.reference, v.nbr_places
ORDER BY nb_trajets DESC;
```

---

## 🐛 DÉBOGAGE

### Si les tests échouent

1. **Vérifier la connexion DB:**
   ```cmd
   psql -U postgres -d gestion_ticket -c "SELECT COUNT(*) FROM Reservation;"
   ```

2. **Vérifier les logs:**
   - Regarder la console Java pour les messages d'erreur
   - Vérifier les logs Tomcat

3. **Réinitialiser les données:**
   ```cmd
   psql -U postgres -d gestion_ticket -f test_sprint3_data.sql
   ```

4. **Supprimer les assignations:**
   ```sql
   TRUNCATE TABLE Assignation CASCADE;
   ```

---

## 📞 CONTACT

Pour toute question sur les tests :
- **Sprint 3 - Calcul Trajet** : Responsable ETU003350
- **Sprint 3 - Interface** : Responsable ETU003366

---

# 🚀 SPRINT 4 - RÈGLES MÉTIER AVANCÉES

## 📋 NOUVELLES FONCTIONNALITÉS

### RG7 : Tri par Nombre de Personnes
**Description:** Les réservations d'un même vol sont triées par nombre de personnes (décroissant) avant assignation.

**Impact:** 
- Les groupes importants sont traités en priorité
- Optimisation du remplissage des véhicules

### RG8 : Remplissage Progressif
**Description:** Un véhicule peut effectuer PLUSIEURS VOYAGES (aller-retour) pour un même vol en remplissant progressivement sa capacité.

**Fonctionnement:**
1. Prise de la plus grande réservation
2. Recherche du véhicule optimal
3. Ajout d'autres réservations du même vol tant que la capacité le permet
4. Création d'un voyage avec toutes les réservations ajoutées
5. Répétition jusqu'à assignation complète

**Exemple:**
- Vol 18h00 : 5 pers (Carton), 4 pers (Radisson), 3 pers (Radisson)
- Véhicule 12 places :
  - **Voyage 1** : 5 personnes → Carton
  - **Voyage 2** : 4 + 3 = 7 personnes → Radisson

### RG9 : Algorithme Nearest-First (Plus Proche Voisin)
**Description:** L'ordre de visite des hôtels est optimisé en choisissant à chaque étape l'hôtel non visité le plus proche de la position actuelle.

**Algorithme:**
1. Départ depuis l'aéroport
2. À chaque étape, chercher l'hôtel le plus proche parmi les non visités
3. Se déplacer vers cet hôtel
4. Répéter jusqu'à visiter tous les hôtels
5. Retour à l'aéroport

**Avantage:** Réduction de la distance totale parcourue

### RG11 : Tri Lexicographique en Cas d'Égalité
**Description:** Si deux hôtels sont à égale distance, l'ordre alphabétique du nom détermine le choix.

**Exemple:**
- Si "Hotel Carton" et "Hotel Lokanga" sont tous deux à 15 km
- On choisit "Hotel Carton" (C < L)

### AUTO-RESET : Réinitialisation des Assignations
**Description:** Avant chaque recalcul de planification pour une date, toutes les assignations existantes pour cette date sont automatiquement supprimées.

**Impact:** Permet de recalculer entièrement la planification sans conflit

---

## 🎯 SCÉNARIOS DE TEST SPRINT 4

### TEST 7 : Remplissage Progressif (Vol 18h00) - RG8

**Données:**
- Vol 18h00 : 3 réservations
  - 5 pers → Carton
  - 4 pers → Radisson
  - 3 pers → Radisson

**Attendu:**
- ✅ 1 seul véhicule 12 places Diesel
- ✅ 2 voyages distincts pour ce véhicule :
  - **Voyage 1** : 5 personnes → Carton (départ 18h00)
  - **Voyage 2** : 7 personnes (4+3) → Radisson (départ après retour du voyage 1)

**Vérifications:**
- Le véhicule fait 2 aller-retours
- Total personnes transportées : 12
- Chaque voyage a son propre itinéraire
- Les heures de départ/retour sont cohérentes

---

### TEST 8 : Algorithme Nearest-First (Vol 19h00) - RG9

**Données:**
- Vol 19h00 : 3 réservations vers 3 hôtels
  - 2 pers → Carton (15 km de l'aéroport)
  - 2 pers → Radisson (18 km de l'aéroport)
  - 2 pers → Lokanga (12 km de l'aéroport)

**Distances:**
- Aéroport → Lokanga : 12 km
- Aéroport → Carton : 15 km
- Aéroport → Radisson : 18 km
- Lokanga → Carton : 8 km
- Carton → Radisson : 5 km

**Ordre attendu (nearest-first):**
1. Aéroport → **Lokanga** (12 km - le plus proche)
2. Lokanga → **Carton** (8 km - plus proche que Radisson)
3. Carton → **Radisson** (5 km)
4. Radisson → Aéroport (18 km)

**Distance totale optimale:** 12 + 8 + 5 + 18 = **43 km**

**⚠️ NE PAS** suivre l'ordre simple par distance depuis aéroport :
- Aéroport → Lokanga (12 km)
- Lokanga → Carton (8 km)
- Carton → Radisson (5 km)
- VS ordre simple : Aéroport → Lokanga → Carton → Radisson donnerait 12 + 8 + 5 = 25 km (aller) + distances retour

**Vérification:**
- ✅ L'itinéraire affiché suit l'ordre nearest-first
- ✅ La distance totale est optimisée
- ✅ Les logs indiquent "Ordre de visite (nearest-first) : Lokanga → Carton → Radisson"

---

### TEST 9 : Tri Alphabétique (Vol 20h00) - RG11

**Données:**
- Vol 20h00 : 3 réservations de 2 personnes chacune
  - CLIENT-019 → Zebra Palace Hotel (ID 7)
  - CLIENT-020 → Alpha Star Hotel (ID 8)
  - CLIENT-021 → Mikado Resort Hotel (ID 9)

**Distances (TOUTES ÉGALES):**
- Aéroport → Zebra Palace : **22 km**
- Aéroport → Alpha Star : **22 km**
- Aéroport → Mikado Resort : **22 km**
- Entre les 3 hôtels : **5 km** (distances égales dans toutes les directions)

**Comportement attendu selon RG8 (Remplissage progressif):**

Puisque chaque réservation a 2 personnes (toutes égales après tri RG7), l'algorithme :
1. Prend la première réservation (2 pers)
2. Trouve le véhicule optimal = VH-001-D (4 places, le plus petit disponible)
3. Ajoute une autre réservation (2 pers) pour remplir → Total: 4/4 places
4. Prend la troisième réservation (2 pers)
5. Trouve un nouveau véhicule = VH-002-D (4 places)

**Résultat attendu:**

**Véhicule 1 (VH-001-D ou VH-002-D, 4 places):**
- 2 réservations parmi les 3
- L'ordre de visite des 2 hôtels doit suivre RG11 (alphabétique car distance égale)
- Si Alpha et Mikado : ordre **Alpha → Mikado**
- Si Alpha et Zebra : ordre **Alpha → Zebra**
- Si Mikado et Zebra : ordre **Mikado → Zebra**

**Véhicule 2 (autre véhicule 4 places):**
- 1 réservation restante
- Trajet simple : Aéroport → Hôtel → Aéroport

**Vérification RG11:**
- ✅ Dans l'itinéraire du véhicule 1, l'ordre alphabétique est respecté
- ✅ Les logs montrent "Ordre de visite (nearest-first)" avec tri alphabétique
- ✅ Si plusieurs hôtels à égale distance, le nom qui commence par 'A' vient avant 'M', 'M' avant 'Z'

**Distance totale par véhicule:**
- Véhicule 1 (2 hôtels) : 22 + 5 + 22 = 49 km
- Véhicule 2 (1 hôtel) : 22 + 22 = 44 km

**⚠️ Note sur RG8:**
L'algorithme de remplissage progressif sélectionne le véhicule **optimal pour la première réservation**, pas pour l'ensemble du groupe. C'est pourquoi 2 véhicules de 4 places sont utilisés au lieu d'un véhicule de 7 places. C'est le comportement correct selon la spécification RG8.

---

### TEST 10 : Réinitialisation Automatique - AUTO-RESET

**Procédure:**
1. Planifier le 10 Mars 2026 (première fois)
2. Vérifier les assignations créées :
   ```sql
   SELECT COUNT(*) FROM Assignation WHERE DATE(date_heure_planification) = '2026-03-10';
   ```
3. Planifier à nouveau le 10 Mars 2026
4. Vérifier que les anciennes assignations ont été supprimées et remplacées

**Attendu:**
- ✅ Le nombre d'assignations reste cohérent (pas de doublons)
- ✅ Log : "Assignations supprimées pour la date 2026-03-10 : X ligne(s)"
- ✅ Nouvelle planification identique à la première

---

## 📊 VÉRIFICATION DES RÈGLES SPRINT 4

### Vérifier RG7 (Tri par nombre de personnes)

```sql
-- Les réservations dans l'ordre de traitement
SELECT 
    date_heure_arrivee,
    client_id,
    nbr_pers,
    hotel_id
FROM Reservation
WHERE DATE(date_heure_arrivee) = '2026-03-10'
ORDER BY date_heure_arrivee, nbr_pers DESC;
```

**Attendu:** Pour chaque vol, les réservations avec le plus de personnes apparaissent en premier.

---

### Vérifier RG8 (Remplissage progressif)

```sql
-- Compter les voyages par véhicule et par vol
SELECT 
    v.reference,
    DATE_TRUNC('hour', a.date_heure_planification) as heure_vol,
    COUNT(DISTINCT r.idReservation) as nb_reservations,
    SUM(r.nbr_pers) as total_personnes
FROM Assignation a
JOIN Reservation r ON a.reservation_id = r.idReservation
JOIN Vehicule v ON a.vehicule_id = v.idVehicule
WHERE DATE(a.date_heure_planification) = '2026-03-10'
GROUP BY v.reference, DATE_TRUNC('hour', a.date_heure_planification)
HAVING COUNT(DISTINCT r.idReservation) > 1
ORDER BY heure_vol, v.reference;
```

**Attendu:** Certains véhicules ont plusieurs réservations pour un même vol (plusieurs voyages).

---

### Vérifier RG9 (Nearest-first) et RG11 (Lexicographique)

**Via les logs :**
- Rechercher dans les logs : "Ordre de visite (nearest-first)"
- Vérifier que l'ordre suit bien l'algorithme du plus proche voisin

**Via l'interface :**
- Observer l'itinéraire affiché dans la JSP
- Comparer avec l'ordre attendu selon les distances

---

## 🐛 DÉBOGAGE SPRINT 4

### Problème : Les voyages multiples ne s'affichent pas

**Solution :**
1. Vérifier que VehiculePlanDTO contient bien la propriété `voyages`
2. Vérifier que la JSP itère sur `plan.voyages`
3. Vérifier les logs : "Ajout d'une réservation au voyage en cours"

### Problème : L'ordre de visite n'est pas optimisé

**Solution :**
1. Vérifier que TrajetCalculator utilise bien l'algorithme nearest-first (pas un simple tri)
2. Vérifier les distances dans la table Distance
3. Chercher les logs : "Ordre de visite (nearest-first)"

### Problème : Les assignations se dupliquent

**Solution :**
1. Vérifier que PlanificationService appelle `reinitialiserAssignations(date)` au début
2. Vérifier que AssignationRepository.deleteByDate() fonctionne
3. Exécuter manuellement :
   ```sql
   DELETE FROM Assignation WHERE DATE(date_heure_planification) = '2026-03-10';
   ```

---

## 📈 COMPARAISON SPRINT 3 VS SPRINT 4

| Aspect | Sprint 3 | Sprint 4 |
|--------|----------|----------|
| **Voyages par véhicule** | 1 seul voyage | Plusieurs voyages possibles (RG8) |
| **Ordre de visite** | Tri simple par distance | Nearest-first optimisé (RG9) |
| **Tri des réservations** | Ordre aléatoire | Par nombre décroissant (RG7) |
| **Égalité de distance** | Non géré | Tri alphabétique (RG11) |
| **Recalcul** | Manuel (TRUNCATE) | Auto-reset automatique |

---

**✅ BONNE CHANCE POUR LES TESTS SPRINT 4 ! 🚀**
