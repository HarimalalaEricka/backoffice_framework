
# SCENARIO DE TEST APRES SPRINT 7 :
On a les donnees suivantes :
- Parametre
INSERT INTO Parametre (idParametre, vitesse_moyenne, temps_attente) VALUES
(1, 50, 30);

- Hotel
INSERT INTO Hotel (idHotel, nom, code, libelle) VALUES
(1, 'Aeroport Ivato', 'AIV', 'aeroport'),
(2, 'Hotel1', 'HT1', 'hotel'),
(3, 'Hotel2', 'HT2', 'hotel');

- Distance
INSERT INTO Distance (from_hotel_id, to_hotel_id, distance_km) VALUES
(1, 2, 90),
(1, 3, 35),
(2, 3, 60);

- Vehicule
INSERT INTO Vehicule (idVehicule, reference, nbr_places, type_carburant) VALUES 
(1, 'VH-001', 5, 'D'),
(2, 'VH-002', 5, 'ES'),
(3, 'VH-003', 12, 'D'),
(4, 'VH-004', 9, 'D'),
(5, 'VH-005', 12, 'ES');

- Heure de disponibile (a partir de ...)
Heure de disponibilite des vehicules :
VH-001 -> 00:00:00
VH-002 -> 00:00:00
VH-003 -> 00:00:00
VH-004 -> 00:00:00
VH-005 -> 13:00:00

- Reservation
INSERT INTO Reservation (idReservation, client_id, date_heure_arrivee, nbr_pers, hotel_id) VALUES
(6, 'c1', '2026-03-28 09:00:00', 7, 3),
(7, 'c2', '2026-03-28 08:00:00', 20, 4),
(8, 'c3', '2026-03-28 09:10:00', 3, 3),
(9, 'c4', '2026-03-28 09:15:00', 10, 3),
(10, 'c5', '2026-03-28 09:20:00', 5, 3),
(11, 'c6', '2026-03-28 13:30:00', 12, 3);

# RESULTAT ATTENDU CORRECT :
**Resultat du 28/03/26 (regroupé par véhicule)**

- **vehicule3**
  - Client2 — nb pers: 12 — heure depart: 08:00:00 — heure retour: 09:24:00 — min duree: 84
  - Client4 — nb pers: 10 — heure depart: 09:24:00 — heure retour: 13:00:00 — min duree: 216
  - Client3 — nb pers: 2 — heure depart: 09:24:00 — heure retour: 13:00:00 — min duree: 216

- **vehicule4**
  - Client2 — nb pers: 8 — heure depart: 09:24:00 — heure retour: 13:06:00 — min duree: 222
  - Client3 — nb pers: 1 — heure depart: 09:24:00 — heure retour: 13:06:00 — min duree: 222

- **vehicule1**
  - Client1 — nb pers: 5 — heure depart: 09:24:00 — heure retour: 13:00:00 — min duree: 216
  - Client5 — nb pers: 2 — heure depart: 13:30:00 — heure retour: 17:06:00 — min duree: 216

- **vehicule2**
  - Client1 — nb pers: 2 — heure depart: 09:24:00 — heure retour: 13:00:00 — min duree: 216
  - Client5 — nb pers: 3 — heure depart: 09:24:00 — heure retour: 13:00:00 — min duree: 216

- **vehicule5**
  - Client6 — nb pers: 12 — heure depart: 13:30:00 — heure retour: 17:06:00 — min duree: 216



# LE RESULTAT QU'ON A OBTENU FAUX :
**Resultat du 28/03/26 (regroupé par véhicule)**

- **vehicule3**
  - Client2 — nb pers: 12 — heure depart: 08:00:00 — heure retour: 09:24:00 — min duree:
  - Client4 — nb pers: 10 — heure depart: 09:24:00 — heure retour: 13:06:00 — min duree: 
  - Client3 — nb pers: 2 — heure depart: 09:24:00 — heure retour: 13:06:00 — min duree: 
  - Client6 — nb pers: 12 — heure depart: 13:30:00 — heure retour: 17:06:00 — min duree: 

- **vehicule4**
  - Client2 — nb pers: 3 — heure depart: 09:20:00 — heure retour: 13:02:00 — min duree: 
  - Client1 — nb pers: 2 — heure depart: 09:20:00 — heure retour: 13:02:00 — min duree:
  - Client5 — nb pers: 4 — heure depart: 09:20:00 — heure retour: 13:02:00 — min duree:

- **vehicule1**
  - Client2 — nb pers: 5 — heure depart: 09:00:00 — heure retour: 10:24:00 — min duree:
  - Client3 — nb pers: 1 — heure depart: 10:24:00 — heure retour: 14:00:00 — min duree:
  - Client5 — nb pers: 1 — heure depart: 10:24:00 — heure retour: 14:00:00 — min duree:

- **vehicule2**
  - Client1 — nb pers: 5 — heure depart: 09:00:00 — heure retour: 12h36:00 — min duree:

- **vehicule5**
  - Client6 — nb pers: 12 — heure depart: 13:30:00 — heure retour: 17:06:00 — min duree: 216


# Probleme a corriger :
- Choix de vehicules apres regroupement :
    - toutes les vehicules (deja entamme avec reste ou non entamme) sont tous candidats



# RESULTAT CORRIGE SEULEMENT EN PARTIE :

**Résultat du 29/03/26 (regroupé par véhicule)**

* **vehicule VH-003**

  * Client2 — nb pers: 12 — heure depart: 08:00:00 — heure retour: 09:24:00 — min duree: 84
  * Client4 — nb pers: 10 — heure depart: 09:24:00 — heure retour: 13:00:00 — min duree: 216
  * Client1 — nb pers: 2 — heure depart: 09:24:00 — heure retour: 13:00:00 — min duree: 216

* **vehicule VH-004**

  * Client2 — nb pers: 8 — heure depart: 09:10:00 — heure retour: 12:52:00 — min duree: 222
  * Client3 — nb pers: 1 — heure depart: 09:10:00 — heure retour: 12:52:00 — min duree: 222

* **vehicule VH-001**

  * Client1 — nb pers: 5 — heure depart: 09:00:00 — heure retour: 12:36:00 — min duree: 216
  * Client3 — nb pers: 2 — heure depart: 12:36:00 — heure retour: 16:12:00 — min duree: 216

- **VH-002**
  - c5 — nb pers: 5 — heure depart: 09:20:00 — heure retour: 12:56:00 — min duree: 216

- **VH-005**
  - c6 — nb pers: 12 — heure depart: 13:30:00 — heure retour: 17:06:00 — min duree: 216