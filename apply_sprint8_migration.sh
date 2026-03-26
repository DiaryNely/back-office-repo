#!/bin/bash

# ============================================
# Script: Appliquer SPRINT8 Migration
# Description: Ajoute la colonne heure_disponibilite aux véhicules
# Date: 26 mars 2026
# ============================================

set -e  # Arrêter si une erreur survient

echo "================================================"
echo "SPRINT 8 - Migration Base de Données"
echo "Ajout colonne heure_disponibilite"
echo "================================================"

# Configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-hotel_db}
DB_USER=${DB_USER:-postgres}

# Vérifier si psql est disponible
if ! command -v psql &> /dev/null; then
    echo "❌ Erreur: psql n'est pas installé"
    exit 1
fi

echo ""
echo "📋 Connexion à: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
echo ""

# Créer le fichier SQL propre (sans la ligne de migration si table n'existe pas)
TEMP_SQL=$(mktemp)
cat > "$TEMP_SQL" << 'EOF'
-- Ajouter la colonne heure_disponibilite
ALTER TABLE vehicules
ADD COLUMN IF NOT EXISTS heure_disponibilite TIME DEFAULT '00:00:00';

-- Mettre à jour les valeurs NULL
UPDATE vehicules 
SET heure_disponibilite = '00:00:00' 
WHERE heure_disponibilite IS NULL;

-- Ajouter contrainte NOT NULL
ALTER TABLE vehicules
ALTER COLUMN heure_disponibilite SET NOT NULL;

-- Vérifier le résultat
SELECT 'Migration appliquée ✓' AS statut;
SELECT COUNT(*) as total_vehicules FROM vehicules;
SELECT id, reference, nombre_places, heure_disponibilite FROM vehicules ORDER BY id;
EOF

# Exécuter la migration
echo "⚙️  Exécution de la migration..."
echo ""

PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$TEMP_SQL"

RESULT=$?

# Nettoyage
rm -f "$TEMP_SQL"

echo ""
if [ $RESULT -eq 0 ]; then
    echo "✅ Migration appliquée avec succès!"
    echo ""
    echo "Prochaines étapes:"
    echo "1. Redéployer l'application: ./deploy-to-tomcat.sh"
    echo "2. Tester via: http://localhost:8080/back-office/planification"
else
    echo "❌ Erreur lors de la migration"
    exit 1
fi

echo ""
echo "================================================"
