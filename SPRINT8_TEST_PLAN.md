# SPRINT 8 - Plan de Test Complet
## Gestion Dynamique des Véhicules
**Date:** 26 mars 2026

---

## 📋 Objectifs du Sprint

| Objectif | Description | Test ID |
|----------|-------------|---------|
| **OBJ1** | Retour de véhicule → part immédiatement avec max passagers | T1-T3 |
| **OBJ2** | Places dispo > passagers → décision attendre/partir | T4-T5 |
| **OBJ3** | Nouveau regroupement → réservations non assignées prioritaires | T6-T7 |

---

## 🧪 Scénarios de Test

### **TEST 1: Départ Immédiat - Véhicule Retournant (OBJ1)**

**Contexte:**
- Véhicule avec capacité restante retourne à l'aéroport
- Réservations non assignées en attente
- Doit partir IMMÉDIATEMENT

**Données de test:**
```sql
-- Insérer des réservations pour 26 mars avec retour anticipé
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('T1-001', 4, '2026-03-26 08:00:00', 1),  -- 4 passagers, first trip
('T1-002', 3, '2026-03-26 09:00:00', 2),  -- 3 passagers, retour
('T1-003', 5, '2026-03-26 10:00:00', 3);  -- 5 passagers, should depart immediately
```

**Véhicules concernés:**
- VEH-001: 5 places (Essence) - prendra T1-001 (4)
- VEH-004: 5 places (Hybride) - retour rapide pour T1-003 (5)

**Résultat attendu:**
✅ VEH-001 redépart avec 4 passagers immédiatement  
✅ Tour impromptu créé avec temps_depart = nextAvailableAt  
✅ Nombre de tours = 2 (initial + impromptu)  

**Vérification:**
```bash
curl "http://localhost:8080/back-office/planification/result?date=2026-03-26" \
  -H "Accept: application/json"
```

**Assertions:**
- `toursAssignes.size()` ≥ 2
- Dernier tour has `heureDepart` = retour du véhicule
- `reservationsNonAssignees.isEmpty()` = true

---

### **TEST 2: Capacité à la Limite (OBJ1)**

**Contexte:**
- Véhicule avec capacité exacte nécessaire
- Doit repartir sans attendre

**Données de test:**
```sql
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('T2-001', 7, '2026-03-26 08:30:00', 1),  -- VEH-002 (7 places)
('T2-002', 7, '2026-03-26 10:30:00', 2),  -- VEH-002 redépart
('T2-003', 9, '2026-03-26 12:30:00', 3);  -- VEH-005 (9 places)
```

**Résultat attendu:**
✅ VEH-002 (7 places) prend T2-001 et redépart avec T2-002  
✅ Pas d'attente (decision.shouldDepart = true)  
✅ Tous assignés

---

### **TEST 3: Fractionnement au Retour (OBJ1)**

**Contexte:**
- Réservation groupe > capacité restante
- Doit se fractionner

**Données de test:**
```sql
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('T3-001', 3, '2026-03-26 08:00:00', 1),   -- 3 passagers
('T3-002', 2, '2026-03-26 09:00:00', 2),   -- 2 passagers  
('T3-003', 8, '2026-03-26 10:00:00', 3);   -- 8 passagers → sera fractionné
```

**Résultat attendu:**
✅ T3-001,T3-002 sur VEH-001 (4 places)  
✅ T3-003 se fractionne: 1 part de T3-003(5) + reste non assigné(3)  
✅ `fractionReference` = R*-P1, R*-P2

---

### **TEST 4: Décision Attendre - Temps Faible (OBJ2)**

**Contexte:**
- Places disponibles **>** passagers non assignés
- Temps d'attente **< 15 minutes** → ATTENDRE

**Données de test:**
```sql
UPDATE parametre SET temps_attente = 10;  -- Config: 10 min

INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('T4-001', 8, '2026-03-26 08:00:00', 1),   -- 8 passagers
('T4-002', 2, '2026-03-26 08:05:00', 2),   -- 2 passagers
('T4-003', 3, '2026-03-26 08:45:00', 3);   -- 3 passagers (après attente)
```

**Capacité disponible:** VEH-005 = 9 places  
**Passagers non assignés après T4-001,T4-002:** 0 + 3 = 3  
**Décision:** Attendre le regroupement

**Résultat attendu:**
✅ VEH-005 n'a pas de redépart impromptu
✅ T4-003 assigné au même tour dans le regroupement
✅ 1 seul tour (pas 2)

---

### **TEST 5: Décision Partir - Temps Élevé (OBJ2)**

**Contexte:**
- Places disponibles > passagers
- Temps d'attente **≥ 15 minutes** → PARTIR

**Données de test:**
```sql
UPDATE parametre SET temps_attente = 30;  -- Config: 30 min

INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('T5-001', 4, '2026-03-26 08:00:00', 1),   -- 4 passagers
('T5-002', 3, '2026-03-26 10:00:00', 2),   -- 3 passagers (2h d'attente)
('T5-003', 2, '2026-03-26 10:30:00', 3);   -- 2 passagers
```

**Capacité disponible après T5-001:** VEH-004 = 5 - 4 = 1 place  
**Passagers non assignés:** 3 + 2 = 5  
**Décision:** PARTIR (temps d'attente 30 min > 15 min)

**Résultat attendu:**
✅ Tour impromptu créé avec 1 passager
✅ T5-002,T5-003 dans un nouveau tour normal
✅ 2 tours pour VEH-004

---

### **TEST 6: Priorité aux Non-Assignées (OBJ3)**

**Contexte:**
- Réservations non assignées du cycle N
- Nouveau regroupement N+1
- Doivent être traitées EN PRIORITÉ

**Données de test:**
```sql
-- Cycle 1 : Sursaturation
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('T6-001', 15, '2026-03-26 08:00:00', 1),  -- 15 passagers! → saturé
('T6-002', 12, '2026-03-26 08:15:00', 2),  -- 12 passagers

-- Cycle 2 : Regroupement avec capacité
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('T6-003', 5, '2026-03-26 10:00:00', 3),   -- 5 passagers
('T6-004', 4, '2026-03-26 10:15:00', 4);   -- 4 passagers
```

**Attentes:**
- Cycle 1: T6-001(15) → fractionné ?  T6-002 → non assignée
- Cycle 2: T6-002(12) traitée EN PREMIER avant T6-003,T6-004

**Résultat attendu:**
✅ T6-002 assignée avant T6-003  
✅ WorkLoad: [T6-002 (carryIn), T6-003, T6-004]  
✅ reservationsNonAssignees exclus T6-002

---

### **TEST 7: Sélection Passagers Optimale (OBJ3)**

**Contexte:**
- Plusieurs réservations non assignées
- Véhicule retourne avec capacité limitée
- Doit sélectionner les **plus gros groupes** d'abord

**Données de test:**
```sql
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('T7-001', 4, '2026-03-26 08:00:00', 1),   -- 4 places utilisées
('T7-002', 8, '2026-03-26 09:00:00', 2),   -- Non assigné (groupe de 8)
('T7-003', 3, '2026-03-26 09:30:00', 3),   -- Non assigné (groupe de 3)
('T7-004', 2, '2026-03-26 10:00:00', 4);   -- Non assigné (groupe de 2)
```

**VEH-005 capacité:** 9 places  
**Après T7-001:** 9 - 4 = **5 places restantes**  
**Groupes non assignés:** 8 + 3 + 2 = 13 passagers  

**Sélection (plus gros d'abord):**
1. T7-002(8) → prend 5
2. Reste: T7-002(3 rest), T7-003(3), T7-004(2)

**Résultat attendu:**
✅ Tour impromptu contient:
  - T7-002: 5 passagers assignés (fractionné)
  - PAS T7-003 ou T7-004
✅ reservationsNonAssignees contient:
  - T7-002-P2: 3 passagers
  - T7-003: 3 passagers
  - T7-004: 2 passagers

---

## 🔧 Exécution des Tests

### **Étape 1: Préparer la base de données**

```bash
# Connexion à PostgreSQL
psql -U postgres -d hotel_db

-- Créer les données pour SPRINT 8
-- Voir les scripts INSERT ci-dessus
```

### **Étape 2: Lancer le serveur**

```bash
cd /home/clapinou/back-office-repo
./deploy-to-tomcat.sh
```

### **Étape 3: Tester via API**

```bash
# TEST 1
curl -X GET "http://localhost:8080/back-office/planification/result?date=2026-03-26" \
  -H "Content-Type: application/json" | jq '.toursAssignes | length'

# Vérifier les tours impromptu
curl -X GET "http://localhost:8080/back-office/planification/result?date=2026-03-26" \
  -H "Content-Type: application/json" | jq '.toursAssignes[] | {vehicule: .vehicule.reference, heure_depart: .heureDepart, total_passagers: .totalPassagers}'

# Vérifier les non-assignées
curl -X GET "http://localhost:8080/back-office/planification/result?date=2026-03-26" \
  -H "Content-Type: application/json" | jq '.reservationsNonAssignees | length'
```

### **Étape 4: Vérifier via Interface Web**

```
http://localhost:8080/back-office/planification
Saisir: 2026-03-26
Cliquer: Planifier
```

**Chercher:**
- ✅ Tours supplémentaires = départs impromptu
- ✅ Heures de départ cohérentes
- ✅ Pas de réservations non assignées (ou très peu)

---

## 📊 Critères d'Acceptation

| Critère | Seuil | Formule |
|---------|-------|---------|
| Tours impromptu créés | ≥ 2 | COUNT(toursAssignes) |
| Taux remplissage moyen | ≥ 75% | SUM(capaciteUtilisee) / SUM(capaciteVehicule) × 100 |
| Non-assignées réduites | ≤ 15% | COUNT(reservationsNonAssignees) / totalReservations × 100 |
| Temps cohérent | ✅ | heureDepart ≤ heureRetour |
| Priorité respectée | ✅ | carryIn traités avant newReservations |

---

## 🐛 Déboguer si Problème

```bash
# Voir les logs Tomcat
tail -100f /opt/tomcat/logs/catalina.out | grep -E "WARN|ERROR|dynamique|return|decision"

# Requête SQL pour inspection
psql -U postgres -d hotel_db
SELECT id, client_id, nombre_passager, id_vehicule, fraction_reference 
FROM reservations 
WHERE date_heure_arrivee::date = '2026-03-26'
ORDER BY date_heure_arrivee;
```

---

## ✅ Checklist Finale

- [ ] Test 1: Départ Immédiat valide
- [ ] Test 2: Capacité limite valide
- [ ] Test 3: Fractionnement valide
- [ ] Test 4: Attendre (T < 15) valide
- [ ] Test 5: Partir (T ≥ 15) valide
- [ ] Test 6: Priorité non-assignées valide
- [ ] Test 7: Sélection optimale valide
- [ ] Tous critères d'acceptation ✅
- [ ] Aucun regression sprint précédents
- [ ] Code compilé sans erreurs

