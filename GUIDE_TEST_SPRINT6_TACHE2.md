# GUIDE TEST - SPRINT 6 TÂCHE 2
## Nouveau Critère : Sélection Véhicule par Moins de Trajets Effectués

---

## 📋 RÈGLES DE SÉLECTION (par ordre d'application)

1. **Capacité véhicule** ≥ nombre de personnes
2. **Capacité minimale** (minimiser la capacité)
3. **[NOUVEAU] Moins de trajets effectués** sur la date
4. **Priorité Diesel** ('D') si égalité
5. **Random** si encore égalité

---

## 📍 RÈGLE POUR `heure_depart`

```
SI dernierVol < heure_retour
  => heure_depart = heure_retour
  
SINON (dernierVol >= heure_retour)
  => heure_depart = dernierVol
```

**Implémentation** : [PlanificationService.java](src/com/back/planification/PlanificationService.java#L555-L570)

---

## 🧪 DONNÉES DE TEST

5 véhicules + 7 groupes de clients créent 6 scénarios distincts :

| Groupe | Heure | Clients | Personnes | Scénario |
|--------|-------|---------|-----------|----------|
| G1 | 09:00 | C1, C2 | 5+3=8 | Tous vierges => Diesel prioritaire |
| G2 | 10:30 | C3, C4 | 4+3=7 | V3 (8 places, 0 trajets) vs autre |
| G3 | 11:30 | C5, C6 | 3+4=7 | Égalité trajets => capacité min |
| G4 | 13:00 | C7, C8 | 5+2=7 | V5 Diesel (nouveau, 0 trajets) |
| G5 | 14:30 | C9, C10 | 6+1=7 | Test heure_depart complexe |
| G6 | 16:00 | C11 | 2 | Petit groupe => capacité minimale |
| G7 | 17:00 | C12, C13 | 5+3=8 | Grand groupe => remplissage |

**Fichier** : [test_sprint6_2.sql](scripts/test_sprint6_2.sql)

---

## 🔧 ÉTAPES POUR TESTER VIA L'INTERFACE

### **ÉTAPE 1** : Initialiser la base de données

```sql
-- Exécuter le fichier de test depuis pgAdmin ou CLI
psql -U postgres -d gestion_ticket -f scripts/test_sprint6_2.sql
```

**Résultat** : 
- 5 véhicules créés
- 13 réservations (7 groupes) créées
- Base nettoyée (Assignations réinitialisées)

---

### **ÉTAPE 2** : Lancer la planification automatique

**Via l'interface web** :
1. Accéder à `/planifier` (page de planification)
2. Sélectionner la date : **2026-03-20**
3. Cliquer sur **"Planifier"**

**Ou via API/Backend** :
```bash
POST /api/planification/planifier?date=2026-03-20
```

---

### **ÉTAPE 3** : Observer les résultats

**Tableau résultat affichera** :
- **Véhicules assignés** avec nombre de voyages
- **Réservations assignées** groupées par véhicule
- **Heure départ/retour** de chaque voyage
- **Réservations non assignées** (le cas échéant)

---

## ✅ SCÉNARIOS À VALIDER

### **Scénario 1 - Priorité Diesel (tous vierges)**
- **Attendu** : G1 (09:00) assigné à **V1** (Diesel, 10 places)
- **Raison** : V1 et V2 vierges (0 trajets), V1 Diesel => priorité

### **Scénario 2 - Moins de trajets**
- **Attendu** : G2 (10:30) assigné à **V3** (8 places, 0 trajets)
- **Raison** : V1 et V2 ont 1 trajet chacun, V3 a 0 trajets
- **Pas assignable à** : V4 (6 places < 7 personnes)

### **Scénario 3 - Capacité minimale (égalité trajets)**
- **Attendu** : G3 (11:30) assigné à **V3** (8 places)
- **Raison** : V1, V2, V3 ont 1 trajet, V3 a capacité minimale (8 vs 10)

### **Scénario 4 - Nouveau Diesel (0 trajets)**
- **Attendu** : G4 (13:00) assigné à **V5** (Diesel, 10 places, 0 trajets)
- **Raison** : V5 a 0 trajets => prioritaire

### **Scénario 5 - heure_depart complexe**
- **Attendu** : Calcul d'heure départ respectant dernierVol vs heure_retour
- **Cas 1** : Si V1 retour = 13:45 et dernier client = 13:50
  - => heure_depart = 13:50 (car 13:45 < 13:50)
- **Cas 2** : Si V2 retour = 13:40 et dernier client = 13:30
  - => heure_depart = 13:40 (car 13:40 > 13:30)

### **Scénario 6 - Petit groupe**
- **Attendu** : G6 (16:00) assigné à **V4** (6 places)
- **Raison** : 2 personnes => capacité minimale suffisante

### **Scénario 7 - Remplissage progressif**
- **Attendu** : G7 (17:00) peut remplir UN seul véhicule (8 personnes)
- **Raison** : Capacité des véhicules permet le remplissage

---

## 🔍 POINTS DE CONTRÔLE DANS LE CODE

### **1. Sélection véhicule** 
**Fichier** : [PlanificationService.java](src/com/back/planification/PlanificationService.java#L330-L380)

```java
// Tri par critères (dans l'ordre) :
candidats.sort((v1, v2) -> {
    // 1) Capacité croissante
    int compareCapacite = Integer.compare(v1.getNbrPlaces(), v2.getNbrPlaces());
    if (compareCapacite != 0) return compareCapacite;
    
    // 2) NOUVEAU : Moins de trajets effectués
    int trajetsV1 = vehiculePlans.containsKey(v1.getIdVehicule()) ? 
                    vehiculePlans.get(v1.getIdVehicule()).getNombreVoyages() : 0;
    int trajetsV2 = vehiculePlans.containsKey(v2.getIdVehicule()) ? 
                    vehiculePlans.get(v2.getIdVehicule()).getNombreVoyages() : 0;
    int compareTrajets = Integer.compare(trajetsV1, trajetsV2);
    if (compareTrajets != 0) return compareTrajets;
    
    // 3) Priorité Diesel
    boolean v1Diesel = "D".equals(v1.getTypeCarburant());
    boolean v2Diesel = "D".equals(v2.getTypeCarburant());
    if (v1Diesel && !v2Diesel) return -1;
    if (!v1Diesel && v2Diesel) return 1;
    
    // 4) Random
    return 0;
});
```

### **2. Calcul heure_depart**
**Fichier** : [PlanificationService.java](src/com/back/planification/PlanificationService.java#L555-L575)

```java
if (heureRetourPrecedente != null && heureRetourPrecedente.isAfter(heureDernierVol)) {
    heureDepartVehicule = heureRetourPrecedente;  // Cas : heure_retour > dernier_vol
} else {
    heureDepartVehicule = heureDernierVol;        // Cas : dernier_vol >= heure_retour
}
```

---

## 🧠 UTILISATIONS DE `VehiculePlanDTO`

**Classe** : [VehiculePlanDTO.java](src/com/back/planification/VehiculePlanDTO.java)

Le compteur `nombreVoyages` est automatiquement incrémenté lors de l'ajout d'un voyage :

```java
vehiculePlan.addVoyage(voyage);  // Incrémente getNombreVoyages()
```

---

## 📊 VÉRIFICATION DES RÉSULTATS

Après planification, vérifier dans la **base de données** :

```sql
-- Afficher toutes les assignations de la journée
SELECT 
    a.idAssignation,
    r.idReservation,
    r.client_id,
    r.date_heure_arrivee,
    r.nbr_pers,
    v.reference,
    v.nbr_places,
    v.type_carburant
FROM Assignation a
JOIN Reservation r ON a.reservation_id = r.idReservation
JOIN Vehicule v ON a.vehicule_id = v.idVehicule
WHERE DATE(r.date_heure_arrivee) = '2026-03-20'
ORDER BY v.reference, r.date_heure_arrivee;
```

---

## 🐛 LOGS À SURVEILLER

**Logs du serveur** : Rechercher les messages starting par :
- ✿ `Sprint 6 - Tâche 2 :` → Sélection " moins de trajets"
- ✿ `Sprint 6 - Véhicule` → Calcul heure_depart
- ✿ `Voyage N° créé` → Confirmé la création

---

## 📝 RÉSUMÉ DE L'IMPLÉMENTATION

| Aspect | Statut | Localisation |
|--------|--------|--------------|
| Base de données | ✅ Prête | [test_sprint6_2.sql](scripts/test_sprint6_2.sql) |
| Critère sélection | ✅ Fait | [PlanificationService.java#L340](src/com/back/planification/PlanificationService.java#L340) |
| Calcul heure_depart | ✅ Fait | [PlanificationService.java#L555](src/com/back/planification/PlanificationService.java#L555) |
| VehiculePlanDTO | ✅ Utile | [VehiculePlanDTO.java](src/com/back/planification/VehiculePlanDTO.java) |
| Interface test | ⏳ À tester | Planifier la date 2026-03-20 |
