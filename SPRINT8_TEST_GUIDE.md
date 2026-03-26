# SPRINT 8 - Gestion Dynamique des Véhicules
**Test Guide**

## 📋 Contenu

Ce dossier contient les fichiers SQL pour tester la fonctionnalité **SPRINT 8**:
- Retour de véhicule avec départ immédiat
- Places disponibles > passagers : attendre ou partir
- Nouveau regroupement : réservations non assignées prioritaires

---

## 🚀 Étapes d'exécution

### **Étape 1: Appliquer la migration**
```bash
psql -U postgres -d hotel_db -f script/SPRINT8_migration.sql
```

**Résultat attendu:**
```
ALTER TABLE
UPDATE 0
ALTER TABLE
Migration appliquée ✓
```

---

### **Étape 2: Charger les données de test**
```bash
psql -U postgres -d hotel_db -f script/SPRINT8_test_data.sql
```

**Résultat attendu:**
```
DELETE NN
UPDATE 5
UPDATE 4
INSERT 0 3
INSERT 0 2
INSERT 0 3
INSERT 0 2
INSERT 0 2
INSERT 0 4
=== SPRINT 8 - Données Chargées ===
 total_reservations 
-----;---------
                 20
(1 row)
```

---

### **Étape 3: Redéployer l'application**
```bash
./deploy-to-tomcat.sh
```

Ou avec Maven:
```bash
cd backend && mvn clean package tomcat7:deploy -DskipTests
```

---

### **Étape 4: Accéder à l'interface**
Ouvrir le navigateur:
```
http://localhost:8080/back-office/planification
```

Entrez la date: **2026-03-26**  
Cliquez: **Planifier**

---

## ✅ Ce qu'il faut vérifier

| Test | Scénario | Attendu |
|------|----------|---------|
| **T1** | Véhicule retour rapide | ✓ Tours impromptu créés (départ immédiat) |
| **T2** | Capacité exacte | ✓ Remplissage optimal (100%) |
| **T3** | Fractionnement | ✓ fractionReference: R*-P1, R*-P2, etc. |
| **T4** | Attendre (< 15 min) | ✓ 1 seul tour (pas impromptu) |
| **T5** | Partir (> 15 min) | ✓ Tours impromptu créés |
| **T6** | Priorité non-assignées | ✓ Non-assignées embarquées en premier |

---

## 📊 Éléments à vérifier dans le résultat

### **Tours Assignés**
```
✓ Numérotation des trajets (1, 2, 3...)
✓ Horaires de départ réels vs théoriques
✓ Taux de remplissage
✓ Route calculée (Nearest Neighbor)
```

### **Réservations Non-Assignées**
```
✓ Nombre réduit (vs cycle précédent)
✓ Raison d'assignation documentée
✓ Priorité appliquée au cycle suivant
```

### **Logs Tomcat**
```bash
tail -f /opt/tomcat/logs/catalina.out
```

Chercher les messages relatifs à:
- Départs impromptu
- Allocation dynamique
- Décisions attendre/partir

---

## 🛠️ Dépannage

### **❌ Erreur: column v.heure_disponibilite does not exist**
→ Exécuter d'abord: `SPRINT8_migration.sql`

### **❌ Pas de tours impromptu créés**
→ Vérifier que `heure_disponibilite` est < date de réservation

### **❌ Réservations non assignées non traitées**
→ Vérifier les logs Tomcat pour les exceptions

### **❌ Connexion PostgreSQL échouée**
```bash
# Vérifier le statut
sudo systemctl status postgresql

# Redémarrer si nécessaire
sudo systemctl restart postgresql
```

---

## 📦 Fichiers utilisés

| Fichier | Description |
|---------|-------------|
| `SPRINT8_migration.sql` | Ajoute colonne `heure_disponibilite` |
| `SPRINT8_test_data.sql` | 6 scénarios de test avec données |

---

## 💾 Sauvegarde avant test

Avant de charger les données:
```bash
pg_dump -U postgres hotel_db > backup_sprint8_$(date +%Y%m%d_%H%M%S).sql
```

Restaurer si besoin:
```bash
psql -U postgres -d hotel_db < backup_sprint8_20260326_082249.sql
```

---

## 🔍 Requêtes SQL utiles

**Vérifier les véhicules:**
```sql
SELECT id, reference, nombre_places, heure_disponibilite FROM vehicules ORDER BY id;
```

**Voir les réservations du jour:**
```sql
SELECT id, client_id, nombre_passager, date_heure_arrivee 
FROM reservations 
WHERE date_heure_arrivee::date = '2026-03-26'
ORDER BY date_heure_arrivee;
```

**Réinitialiser les assignations:**
```sql
DELETE FROM reservations WHERE date_heure_arrivee::date = '2026-03-26';
DELETE FROM assignation_planification_details WHERE planification_id IN (
  SELECT id FROM planification WHERE date = '2026-03-26'
);
```

---

## 📝 Notes

- Les fichiers SQL incluent des commentaires détaillés pour chaque test
- Les horaires peuvent être ajustés selon vos besoins
- Les hôtels doivent exister dans la base (vérifier `id_hotel`)

---

**Date création:** 26 mars 2026  
**Statut:** ✅ Prêt pour test
