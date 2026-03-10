# ✅ SPRINT 3 – PLANIFICATION JOUR J

## 🎯 Objectif
Implémenter la planification automatique des réservations vers les véhicules pour une date donnée.

---

## 🔵 1️⃣ BASE DE DONNÉES

- [x] Vérifier que la table `Assignation` existe (voir [03032026.sql](scripts/03032026.sql))
- [ ] Ajouter index sur `date_heure_planification` si nécessaire
- [ ] Ajouter index sur `vehicule_id` si nécessaire

---

## 🟣 2️⃣ CLASSES À CRÉER / MODIFIER

### 📦 Package : `com.app.repository`

#### AssignationRepository.java (À CRÉER)
- [x] Créer la classe `AssignationRepository` dans [src/com/back/repository/](src/com/back/repository/)
- [x] Méthode `boolean existsByReservationId(int reservationId)`
- [x] Méthode `void save(Assignation assignation)`
- [x] Méthode `List<Assignation> findByDate(LocalDate date)`

### 📦 Package : `com.app.repository` (EXISTANT)

#### ReservationRepository.java
- [x] Vérifier que `findByDate(LocalDate date)` existe dans [ReservationRepository.java](src/com/back/repository/ReservationRepository.java)
- [x] Modifier pour trier par `date_heure_arrivee ASC`, puis `nbr_pers DESC`

#### VehiculeRepository.java
- [x] Utiliser `findAll()` existant dans [VehiculeRepository.java](src/com/back/repository/VehiculeRepository.java)

---

### 📦 Package : `com.app.planification` (À CRÉER)

#### PlanificationResult.java (DTO)
- [x] Créer la classe dans `src/com/back/planification/`
- [x] Attribut `List<VehiculePlanDTO> vehiculesAssignes`
- [x] Attribut `List<Reservation> reservationsNonAssignees`
- [x] Getters/Setters

#### VehiculePlanDTO.java (DTO)
- [x] Créer la classe dans `src/com/back/planification/`
- [x] Attribut `Vehicule vehicule`
- [x] Attribut `List<Reservation> reservations`
- [x] Getters/Setters

#### PlanificationService.java
- [x] Créer la classe dans `src/com/back/planification/`
- [x] Méthode principale `public PlanificationResult planifierJour(LocalDate date)`
- [x] Méthode interne `private Map<LocalDateTime, List<Reservation>> grouperParVol(List<Reservation> list)`
- [x] Méthode interne `private Vehicule trouverVehiculeOptimal(int totalPersonnes, List<Vehicule> disponibles, Set<Integer> vehiculesUtilises)`
- [x] Méthode interne `private void enregistrerAssignation(List<Reservation> group, Vehicule vehicule, LocalDate date)`

#### PlanificationController.java
- [x] Créer la classe dans `src/com/back/controllers/`
- [x] Annoter avec `@Controller`
- [x] Route `@HandleGet("/planification/form")` - déjà partiellement dans [PlanificationController.java](src/com/back/controllers/PlanificationController.java)
- [x] Route `@HandlePost("/planification")` pour lancer la planification
- [x] Route `@HandleGet("/api/planification")` avec `@JsonResponse` pour récupérer le résultat

---

## 🟢 3️⃣ LOGIQUE MÉTIER À IMPLÉMENTER

### Dans PlanificationService.planifierJour(LocalDate date)

- [x] **Étape 1** : Récupérer les réservations du jour via `ReservationRepository.findByDate(date)`
- [x] **Étape 2** : Grouper par vol (même `date_heure_arrivee`)
- [x] **Étape 3** : Trier les groupes par `totalPersonnes DESC` (priorité aux grands groupes)
- [x] **Étape 4** : Pour chaque groupe, calculer `int totalPersonnes = somme(nbr_pers)`
- [x] **Étape 5** : Trouver véhicule optimal selon règles :
  - [x] Capacité ≥ totalPersonnes
  - [x] Capacité la plus proche
  - [x] Si égalité → Diesel ('D') prioritaire
  - [x] Sinon → Random
- [x] **Étape 6** : Vérifier contraintes :
  - [x] Ne pas réassigner une réservation déjà assignée (`existsByReservationId`)
  - [x] Un véhicule ne peut pas être utilisé 2 fois le même jour (`Set<Integer> vehiculesUtilises`)
- [x] **Étape 7** : Enregistrer assignation via `AssignationRepository.save()`
- [x] **Étape 8** : Collecter les réservations non assignées

---

## 🟡 4️⃣ INTÉGRATION API

### Endpoint POST /api/planification
- [x] Protéger par token (utiliser [AuthFilter.java](src/com/back/filters/AuthFilter.java) existant)
- [x] Paramètre `date` (format: `2026-03-10`)
- [x] Retour JSON :
  ```json
  {
    "vehiculesAssignes": [
      {
        "vehicule": { "idVehicule": 1, "reference": "V001", ... },
        "reservations": [ ... ]
      }
    ],
    "reservationsNonAssignees": [ ... ]
  }
  ```

---

## 🟠 5️⃣ AFFICHAGE BACKOFFICE

### Page 1 – Formulaire (EXISTANT)
- [x] Vérifier [planifier.jsp](WebContent/planifier.jsp) existant
- [x] S'assurer que le formulaire pointe vers `/planification` en POST

### Page 2 – Résultat (À CRÉER)
- [x] Créer `WebContent/planification_result.jsp`
- [x] Afficher tableau des véhicules assignés avec leurs réservations
- [x] Afficher liste des réservations non assignées

---

## 🟢 6️⃣ CLASSES EXISTANTES À UTILISER

| Classe | Fichier | Utilisation |
|--------|---------|-------------|
| `Reservation` | [Reservation.java](src/com/back/models/Reservation.java) | Modèle réservation |
| `Vehicule` | [Vehicule.java](src/com/back/models/Vehicule.java) | Modèle véhicule |
| `Assignation` | [Assignation.java](src/com/back/models/Assignation.java) | Modèle assignation |
| `Connexion` | [Connexion.java](src/com/back/util/Connexion.java) | Connexion DB |
| `ReservationRepository` | [ReservationRepository.java](src/com/back/repository/ReservationRepository.java) | Accès réservations |
| `VehiculeRepository` | [VehiculeRepository.java](src/com/back/repository/VehiculeRepository.java) | Accès véhicules |
| `TokenService` | [TokenService.java](src/com/back/service/TokenService.java) | Validation token |

---

## 🧪 7️⃣ TESTS À FAIRE

- [x] **Cas simple** : 1 groupe, 1 véhicule disponible
- [x] **Cas capacité égale** : 2 véhicules même capacité → Diesel choisi
- [x] **Cas pas assez capacité** : Réservation → Non assignée
- [x] **Cas plusieurs vols** : Véhicules distincts assignés
- [ ] **Cas réservation déjà assignée** : Ignorée
- [ ] **Cas véhicule déjà utilisé** : Non réutilisé

---

## 📁 8️⃣ STRUCTURE FINALE

```
src/com/back/
├── controllers/
│   └── PlanificationController.java (modifier existant)
├── planification/
│   ├── PlanificationService.java (créer)
│   ├── PlanificationResult.java (créer)
│   └── VehiculePlanDTO.java (créer)
├── repository/
│   └── AssignationRepository.java (créer)
└── models/
    └── Assignation.java (existant ✓)

WebContent/
├── planifier.jsp (existant ✓)
└── planification_result.jsp (créer)
```

---

## 👥 RESPONSABLES (selon To-do-sprint3.txt)

| Tâche | Responsable |
|-------|-------------|
| Interface Planification (Page 1) | ETU003366 |
| Assignation Reservation → Vehicule (Métier) | ETU003350 |
| Affichage Résultat Planification (Page 2) | ETU003366 |

---

## ⚠️ NOTES IMPORTANTES

1. **Connexion DB** : Utiliser les credentials existants
   ```java
   String url = "jdbc:postgresql://localhost:5432/gestion_ticket";
   String username = "postgres";
   String password = "postgres";
   ```

2. **Annotations Framework** : Utiliser les annotations existantes
   - `@Controller`
   - `@HandleGet` / `@HandlePost`
   - `@JsonResponse`
   - `@RequestParam`

3. **Modèle Assignation existant** : Utiliser les méthodes de [Assignation.java](src/com/back/models/Assignation.java)
   - `setReservationId(int)`
   - `setVehiculeId(int)`
   - `setDateHeurePlanification(LocalDateTime)`


---



## [SUITE ERICKA]

# 🚗 SPRINT 3 – RÉUTILISATION DES VÉHICULES (Calcul Trajet) 

## 🎯 Objectif
Permettre à un véhicule d'effectuer **plusieurs trajets par jour** en calculant son heure de retour à l'aéroport.

---

## 🔵 9️⃣ TABLES EXISTANTES À UTILISER

| Table | Utilisation |
|-------|-------------|
| `Distance` | Distances entre aéroport et hôtels (km) |
| `Parametre` | `vitesse_moyenne` (km/h), `temps_attente` (min) |
| `Hotel` | Identifier l'aéroport (`libelle = 'aeroport'`) |

---

## 🟣 🔟 CLASSES À CRÉER / MODIFIER

### 📦 Package : `com.app.repository`

#### DistanceRepository.java (À CRÉER)
- [x] Créer la classe `DistanceRepository` dans [src/com/back/repository/](src/com/back/repository/)
- [x] Méthode `int getDistance(int fromHotelId, int toHotelId)` — retourne distance en km
- [x] Méthode `List<Distance> findAll()`

#### ParametreRepository.java (À CRÉER)
- [x] Créer la classe `ParametreRepository` dans [src/com/back/repository/](src/com/back/repository/)
- [x] Méthode `Parametre getParametre()` — retourne vitesse_moyenne et temps_attente

#### HotelRepository.java (EXISTANT)
- [x] Vérifier méthode `Hotel findAeroport()` — retourne l'hôtel avec `libelle = 'aeroport'`
- [x] Si absente, l'ajouter

---

### 📦 Package : `com.app.planification`

#### TrajetCalculator.java (À CRÉER)
- [x] Créer la classe dans `src/com/back/planification/`
- [x] Attribut `DistanceRepository distanceRepository`
- [x] Attribut `ParametreRepository parametreRepository`
- [x] Attribut `HotelRepository hotelRepository`
- [x] Méthode `LocalDateTime calculerHeureRetour(LocalDateTime heureDepart, List<Reservation> reservations)`

---

### 📦 Package : `com.app.planification` (EXISTANT)

#### PlanificationService.java (MODIFIER)
- [x] Remplacer `Set<Integer> vehiculesUtilises` par `Map<Integer, LocalDateTime> vehiculesHeureRetour`
- [x] Modifier `trouverVehiculeOptimal()` pour vérifier `heureRetour <= heureVolActuel`
- [x] Après assignation, calculer et stocker `heureRetour` du véhicule

---

## 🟢 1️⃣1️⃣ LOGIQUE MÉTIER – CALCUL TRAJET

### Formules

### Algorithme : calculerHeureRetour(heureDepart, reservations)

- [x] **Étape 1** : Récupérer l'ID de l'aéroport via `HotelRepository.findAeroport()`
- [x] **Étape 2** : Récupérer les paramètres (vitesse_moyenne, temps_attente)
- [x] **Étape 3** : Construire le trajet :

- [x] **Étape 4** : Trier les hôtels par distance croissante depuis l'aéroport (optimisation)
- [x] **Étape 5** : Pour chaque segment, calculer :
    - int distanceKm = distanceRepository.getDistance(fromId, toId);
    - double tempsHeures = distanceKm / vitesseMoyenne;
    - int tempsMinutes = (int) (tempsHeures * 60) + tempsAttente;
    - heureActuelle = heureActuelle.plusMinutes(tempsMinutes);

## 🟢 1️⃣2️⃣ LOGIQUE MÉTIER – RÉUTILISATION VÉHICULE
## Dans PlanificationService.planifierJour(LocalDate date)
- [x] Modifier Étape 6 : Un véhicule est disponible si :
    Véhicule jamais utilisé OU déjà revenu
    - !vehiculesHeureRetour.containsKey(vehiculeId) || vehiculesHeureRetour.get(vehiculeId).isBefore(heureVolActuel)

- [x] Après assignation : Calculer et stocker heure retour
    - LocalDateTime heureRetour = trajetCalculator.calculerHeureRetour(heureVol, reservationsGroupe);
    - vehiculesHeureRetour.put(vehicule.getIdVehicule(), heureRetour);

## 🧪 1️⃣3️⃣ TESTS À FAIRE
- Cas réutilisation : Véhicule revenu avant prochain vol → réassigné
- Cas non réutilisation : Véhicule pas encore revenu → autre véhicule choisi
- Cas plusieurs hôtels : Calcul trajet avec 3+ hôtels

## 🟠 1️⃣4️⃣ AFFICHAGE RÉSULTAT – DÉTAILS TRAJET

### Informations à afficher par véhicule

| Information | Description |
|-------------|-------------|
| **Heure de départ** | Heure de départ du véhicule de l'aéroport (= heure arrivée vol) |
| **Heure de retour** | Heure de retour à l'aéroport après le trajet complet |
| **Distance totale** | Distance totale parcourue (aller + retour) en km |

### Informations à afficher par arrêt (hôtel)

| Information | Description |
|-------------|-------------|
| **Ordre de passage** | Numéro d'ordre (1, 2, 3...) si plusieurs hôtels |
| **Nom de l'hôtel** | Nom complet de l'hôtel (pas juste l'ID) |
| **Heure d'arrivée** | Heure d'arrivée estimée à cet hôtel |
| **Distance parcourue** | Distance depuis l'arrêt précédent (km) |
| **Distance cumulée** | Distance totale depuis l'aéroport (km) |


### 📦 Package : `com.app.planification`

#### TrajetDetailDTO.java (À CRÉER)
- [x] Créer la classe dans `src/com/back/planification/`
- [x] Attribut `int ordre` — ordre de passage (1, 2, 3...)
- [x] Attribut `String nomHotel` — nom de l'hôtel
- [x] Attribut `LocalDateTime heureArrivee` — heure d'arrivée à l'hôtel
- [x] Attribut `int distanceSegment` — distance depuis arrêt précédent (km)
- [x] Attribut `int distanceCumulee` — distance totale depuis aéroport (km)
- [x] Getters/Setters

#### VehiculePlanDTO.java (MODIFIER)
- [x] Ajouter attribut `LocalDateTime heureDepart`
- [x] Ajouter attribut `LocalDateTime heureRetour`
- [x] Ajouter attribut `int distanceTotale` — distance totale parcourue (km)
- [x] Ajouter attribut `List<TrajetDetailDTO> detailsTrajet` — liste des arrêts avec détails
- [x] Getters/Setters

---


### 📦 Package : `com.app.planification` (EXISTANT)

#### TrajetCalculator.java (MODIFIER)
- [x] Modifier méthode `calculerHeureRetour()` pour retourner un objet complet avec tous les détails
- [x] Nouvelle méthode `TrajetComplet calculerTrajetComplet(LocalDateTime heureDepart, List<Reservation> reservations)`
- [x] Retourner : heureRetour, distanceTotale, List<TrajetDetailDTO>


### 🖥️ Page planification_result.jsp (MODIFIER)

#### Section par véhicule — Ajouter :
- [x] Afficher **Heure de départ** : `<%= vp.getHeureDepart() %>`
- [x] Afficher **Heure de retour** : `<%= vp.getHeureRetour() %>`
- [x] Afficher **Distance totale** : `<%= vp.getDistanceTotale() %> km`

#### Nouveau tableau — Détails du trajet :
- [x] Colonne **Ordre** : numéro de passage
- [x] Colonne **Hôtel** : nom de l'hôtel
- [x] Colonne **Heure arrivée** : heure estimée
- [x] Colonne **Distance segment** : km depuis arrêt précédent
- [x] Colonne **Distance cumulée** : km depuis aéroport

---