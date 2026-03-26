#!/bin/bash

# ============================================
# Script: SPRINT 8 - Test Complet
# Description: Charge les données de test pour valider
#              la gestion dynamique des véhicules
# Date: 26 mars 2026
# ============================================

set -e

echo "================================================"
echo "SPRINT 8 - Test Gestion Dynamique des Véhicules"
echo "================================================"
echo ""

# Configuration  
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-hotel_db}
DB_USER=${DB_USER:-postgres}

# Vérifier psql
if ! command -v psql &> /dev/null; then
    echo "❌ Erreur: psql n'est pas installé"
    exit 1
fi

echo "📋 Configuration:"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Créer le fichier SQL de test
TEMP_SQL=$(mktemp)

cat > "$TEMP_SQL" << 'EOF'
-- ============================================
-- SPRINT 8 - DONNÉES DE TEST
-- Gestion Dynamique des Véhicules
-- Date: 26 mars 2026
-- ============================================

-- 1. Vider les réservations existantes pour ce jour
DELETE FROM reservations WHERE date_heure_arrivee::date = '2026-03-26';

-- 2. Mettre à jour les paramètres
UPDATE parametre SET temps_attente = 15, vitesse_moyenne = 50.00;

-- 3. Définir les horaires de disponibilité des véhicules
UPDATE vehicules SET heure_disponibilite = '08:00:00' WHERE id = 1;  -- VEH-001
UPDATE vehicules SET heure_disponibilite = '09:00:00' WHERE id = 2;  -- VEH-002
UPDATE vehicules SET heure_disponibilite = '10:00:00' WHERE id = 3;  -- VEH-003
UPDATE vehicules SET heure_disponibilite = '11:00:00' WHERE id = 4;  -- VEH-004

-- ============================================
-- TEST 1: Départ Immédiat - Véhicule Retournant
-- ============================================
-- Scénario:
-- VEH-001 retourne à 08:00, capacité 12 places
-- Réservations arrivent à 08:15 et 08:45
-- Attendu: VEH-001 part immédiatement avec max passagers
-- 
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('CLI-001', 4, '2026-03-26 08:15:00', 1),   -- VEH-001 prend 4 (Tour 1)
('CLI-002', 3, '2026-03-26 08:45:00', 2),   -- VEH-001 prend 3 avant départ
('CLI-003', 5, '2026-03-26 09:00:00', 3);   -- VEH-001 redépart impromptu

-- ============================================
-- TEST 2: Capacité Exacte
-- ============================================
-- Scénario:
-- VEH-002 (5 places) revient à 09:00
-- Réservation de 5 passagers arrive à 09:05
-- Attendu: VEH-002 redépart avec les 5 passagers
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('CLI-004', 5, '2026-03-26 09:05:00', 1);   -- VEH-002 redépart (capacité exacte)

-- ============================================
-- TEST 3: Départ Rapide - Temps d'Attente Élevé
-- ============================================
-- Scénario:
-- VEH-003 (5 places) revient à 10:00
-- Réservation de 2 passagers + grande attente avant prochain groupe
-- Attendu: VEH-003 part immédiatement (temps > 15 min)
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('CLI-005', 2, '2026-03-26 10:30:00', 2);   -- Petite réservation

-- ============================================
-- TEST 4: Fractionnement au Retour
-- ============================================
-- Scénario:
-- VEH-004 (12 places) revient à 11:00
-- Réservations: 8, 7 et 6 passagers (trop pour 1 véhicule)
-- Attendu: Fractionné entre plusieurs tours
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('CLI-006', 8, '2026-03-26 11:15:00', 3),   -- 8 passagers
('CLI-007', 7, '2026-03-26 11:45:00', 4),   -- 7 passagers
('CLI-008', 6, '2026-03-26 12:00:00', 1);   -- 6 passagers

-- Affichage des résultats
SELECT '=== DONNÉES CHARGÉES ===' AS info;
SELECT COUNT(*) as total_reservations FROM reservations WHERE date_heure_arrivee::date = '2026-03-26';
SELECT 'Véhicules' AS info;
SELECT id, reference, nombre_places, heure_disponibilite FROM vehicules ORDER BY id;
SELECT 'Paramètres' AS info;
SELECT temps_attente, vitesse_moyenne FROM parametre LIMIT 1;
EOF

# Exécuter le script
echo "⚙️  Chargement des données de test..."
echo ""

PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$TEMP_SQL"

RESULT=$?

# Nettoyage
rm -f "$TEMP_SQL"

echo ""
if [ $RESULT -eq 0 ]; then
    echo "✅ Données de test chargées avec succès!"
    echo ""
    echo "📊 PROCHAINES ÉTAPES:"
    echo ""
    echo "1️⃣  Redéployer l'application:"
    echo "    ./deploy-to-tomcat.sh"
    echo ""
    echo "2️⃣  Accéder à l'interface:"
    echo "    URL: http://localhost:8080/back-office/planification"
    echo "    Date: 2026-03-26"
    echo ""
    echo "3️⃣  Vérifier les résultats:"
    echo "    ✓ Tours impromptu créés (départ immédiat)"
    echo "    ✓ Réservations non-assignées en priorité"
    echo "    ✓ Fractionnement correctement appliqué"
    echo ""
    echo "4️⃣  Vérifier les logs:"
    echo "    tail -f /opt/tomcat/logs/catalina.out"
else
    echo "❌ Erreur lors du chargement des données"
    exit 1
fi

echo ""
echo "================================================"
