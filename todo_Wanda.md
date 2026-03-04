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
- [ ] Créer la classe dans `src/com/back/planification/`
- [ ] Méthode principale `public PlanificationResult planifierJour(LocalDate date)`
- [ ] Méthode interne `private Map<LocalDateTime, List<Reservation>> grouperParVol(List<Reservation> list)`
- [ ] Méthode interne `private Vehicule trouverVehiculeOptimal(int totalPersonnes, List<Vehicule> disponibles, Set<Integer> vehiculesUtilises)`
- [ ] Méthode interne `private void enregistrerAssignation(List<Reservation> group, Vehicule vehicule, LocalDate date)`

#### PlanificationController.java
- [ ] Créer la classe dans `src/com/back/controllers/`
- [ ] Annoter avec `@Controller`
- [ ] Route `@HandleGet("/planification/form")` - déjà partiellement dans [PlanificationController.java](src/com/back/controllers/PlanificationController.java)
- [ ] Route `@HandlePost("/planification")` pour lancer la planification
- [ ] Route `@HandleGet("/api/planification")` avec `@JsonResponse` pour récupérer le résultat

---

## 🟢 3️⃣ LOGIQUE MÉTIER À IMPLÉMENTER

### Dans PlanificationService.planifierJour(LocalDate date)

- [ ] **Étape 1** : Récupérer les réservations du jour via `ReservationRepository.findByDate(date)`
- [ ] **Étape 2** : Grouper par vol (même `date_heure_arrivee`)
  ```java
  Map<LocalDateTime, List<Reservation>> grouped = 
      reservations.stream()
      .collect(Collectors.groupingBy(Reservation::getDateHeureArrivee));
  ```
- [ ] **Étape 3** : Trier les groupes par `totalPersonnes DESC` (priorité aux grands groupes)
- [ ] **Étape 4** : Pour chaque groupe, calculer `int totalPersonnes = somme(nbr_pers)`
- [ ] **Étape 5** : Trouver véhicule optimal selon règles :
  - [ ] Capacité ≥ totalPersonnes
  - [ ] Capacité la plus proche
  - [ ] Si égalité → Diesel ('D') prioritaire
  - [ ] Sinon → Random
- [ ] **Étape 6** : Vérifier contraintes :
  - [ ] Ne pas réassigner une réservation déjà assignée (`existsByReservationId`)
  - [ ] Un véhicule ne peut pas être utilisé 2 fois le même jour (`Set<Integer> vehiculesUtilises`)
- [ ] **Étape 7** : Enregistrer assignation via `AssignationRepository.save()`
- [ ] **Étape 8** : Collecter les réservations non assignées

---

## 🟡 4️⃣ INTÉGRATION API

### Endpoint POST /api/planification
- [ ] Protéger par token (utiliser [AuthFilter.java](src/com/back/filters/AuthFilter.java) existant)
- [ ] Paramètre `date` (format: `2026-03-10`)
- [ ] Retour JSON :
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
- [ ] Vérifier [planifier.jsp](WebContent/planifier.jsp) existant
- [ ] S'assurer que le formulaire pointe vers `/planification` en POST

### Page 2 – Résultat (À CRÉER)
- [ ] Créer `WebContent/planification_result.jsp`
- [ ] Afficher tableau des véhicules assignés avec leurs réservations
- [ ] Afficher liste des réservations non assignées

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

- [ ] **Cas simple** : 1 groupe, 1 véhicule disponible
- [ ] **Cas capacité égale** : 2 véhicules même capacité → Diesel choisi
- [ ] **Cas pas assez capacité** : Réservation → Non assignée
- [ ] **Cas plusieurs vols** : Véhicules distincts assignés
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