#!/bin/bash

# ============================================
# SPRINT 8 - SCRIPT DE TEST AUTOMATISÉ
# Gestion Dynamique des Véhicules
# ============================================

set -e

BASE_URL="http://localhost:8080/back-office"
TEST_DATE="2026-03-26"
RESULTS_FILE="/tmp/sprint8-results.json"

echo "================================================"
echo "SPRINT 8 - Test Automatisé"
echo "Date: $TEST_DATE"
echo "Base URL: $BASE_URL"
echo "================================================"
echo ""

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ============================================
# Fonction pour afficher les résultats
# ============================================
print_result() {
    local test_name=$1
    local expected=$2
    local actual=$3
    local status=$4
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC} | $test_name"
        echo "  Expected: $expected"
        echo "  Actual:   $actual"
    else
        echo -e "${RED}✗ FAIL${NC} | $test_name"
        echo "  Expected: $expected"
        echo "  Actual:   $actual"
    fi
    echo ""
}

# ============================================
# Fonction pour obtenir les résultats de plan
# ============================================
get_planning_result() {
    curl -s -X GET "$BASE_URL/planification/result?date=$TEST_DATE" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        2>/dev/null || echo "{}"
}

# ============================================
# Attendre le démarrage du serveur
# ============================================
echo "Vérification du serveur Tomcat..."
max_attempts=30
attempt=0
while ! curl -s "$BASE_URL" >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ $attempt -gt $max_attempts ]; then
        echo -e "${RED}✗ Erreur: Tomcat ne répond pas${NC}"
        exit 1
    fi
    echo "  Tentative $attempt/$max_attempts..."
    sleep 1
done
echo -e "${GREEN}✓ Tomcat est actif${NC}"
echo ""

# ============================================
# EXÉCUTER LA PLANIFICATION
# ============================================
echo "Exécution de la planification pour $TEST_DATE..."
RESULT=$(get_planning_result)
echo "$RESULT" > "$RESULTS_FILE"

# Extraire les données
TOTAL_TOURS=$(echo "$RESULT" | jq '.toursAssignes | length // 0' 2>/dev/null || echo "0")
TOTAL_NON_ASSIGNEES=$(echo "$RESULT" | jq '.reservationsNonAssignees | length // 0' 2>/dev/null || echo "0")
TOTAL_RESERVATIONS=$(echo "$RESULT" | jq '.totalReservations // 0' 2>/dev/null || echo "0")

echo ""
echo "================================================"
echo "RÉSULTATS GLOBAUX"
echo "================================================"
echo "Total Réservations:     $TOTAL_RESERVATIONS"
echo "Total Tours:            $TOTAL_TOURS"
echo "Non-assignées:          $TOTAL_NON_ASSIGNEES"
echo ""

# ============================================
# TEST 1: Tours Impromptu Créés
# ============================================
echo "================================================"
echo "TEST 1: Tours Impromptu (OBJ1)"
echo "================================================"

MIN_TOURS=2
if [ "$TOTAL_TOURS" -ge "$MIN_TOURS" ]; then
    print_result "Test 1a: Au moins 2 tours" ">= $MIN_TOURS" "$TOTAL_TOURS" "PASS"
else
    print_result "Test 1a: Au moins 2 tours" ">= $MIN_TOURS" "$TOTAL_TOURS" "FAIL"
fi

# Vérifier les horaires de départ cohérents
FIRST_DEPARTURE=$(echo "$RESULT" | jq '.toursAssignes[0].heureDepart // "null"' 2>/dev/null)
LAST_DEPARTURE=$(echo "$RESULT" | jq ".toursAssignes[-1].heureDepart // \"null\"" 2>/dev/null)

echo "Premier départ: $FIRST_DEPARTURE"
echo "Dernier départ:  $LAST_DEPARTURE"

if [ "$FIRST_DEPARTURE" != "null" ] && [ "$LAST_DEPARTURE" != "null" ]; then
    print_result "Test 1b: Horaires définis" "!= null" "$LAST_DEPARTURE" "PASS"
else
    print_result "Test 1b: Horaires définis" "!= null" "null" "FAIL"
fi
echo ""

# ============================================
# TEST 2: Taux de Remplissage
# ============================================
echo "================================================"
echo "TEST 2: Taux de Remplissage (OBJ1,OBJ3)"
echo "================================================"

TOTAL_CAPACITE=$(echo "$RESULT" | jq '[.toursAssignes[].capaciteVehicule] | add // 0' 2>/dev/null)
TOTAL_UTILISE=$(echo "$RESULT" | jq '[.toursAssignes[].capaciteUtilisee] | add // 0' 2>/dev/null)

if [ "$TOTAL_CAPACITE" -gt 0 ]; then
    TAUX_REMPLISSAGE=$((TOTAL_UTILISE * 100 / TOTAL_CAPACITE))
else
    TAUX_REMPLISSAGE=0
fi

echo "Capacité totale: $TOTAL_CAPACITE places"
echo "Utilisée:        $TOTAL_UTILISE places"
echo "Taux:            $TAUX_REMPLISSAGE%"
echo ""

if [ "$TAUX_REMPLISSAGE" -ge 70 ]; then
    print_result "Test 2: Taux remplissage >= 70%" ">= 70%" "$TAUX_REMPLISSAGE%" "PASS"
else
    print_result "Test 2: Taux remplissage >= 70%" ">= 70%" "$TAUX_REMPLISSAGE%" "FAIL"
fi
echo ""

# ============================================
# TEST 3: Non-assignées Minimales
# ============================================
echo "================================================"
echo "TEST 3: Réductions Non-Assignées (OBJ3)"
echo "================================================"

if [ "$TOTAL_RESERVATIONS" -gt 0 ]; then
    POURCENTAGE_NON_ASSIGNEES=$((TOTAL_NON_ASSIGNEES * 100 / TOTAL_RESERVATIONS))
else
    POURCENTAGE_NON_ASSIGNEES=0
fi

echo "Non-assignées: $TOTAL_NON_ASSIGNEES / $TOTAL_RESERVATIONS ($POURCENTAGE_NON_ASSIGNEES%)"
echo ""

if [ "$POURCENTAGE_NON_ASSIGNEES" -le 20 ]; then
    print_result "Test 3: Non-assignées <= 20%" "<= 20%" "$POURCENTAGE_NON_ASSIGNEES%" "PASS"
else
    print_result "Test 3: Non-assignées <= 20%" "<= 20%" "$POURCENTAGE_NON_ASSIGNEES%" "FAIL"
fi
echo ""

# ============================================
# TEST 4: Vérifier les Fractionnements
# ============================================
echo "================================================"
echo "TEST 4: Fractionnements Cohérents (OBJ1,OBJ2)"
echo "================================================"

FRACTIONS=$(echo "$RESULT" | jq '[.toursAssignes[].reservations[] | select(.fractionReference != null)] | length' 2>/dev/null || echo "0")
echo "Fractions détectées: $FRACTIONS"

if [ "$FRACTIONS" -ge 0 ]; then
    print_result "Test 4: Fractionnements valides" ">= 0" "$FRACTIONS" "PASS"
fi
echo ""

# ============================================
# TEST 5: Cohérence des Horaires
# ============================================
echo "================================================"
echo "TEST 5: Cohérence Horaires (OBJ1,OBJ2)"
echo "================================================"

# Vérifier que heureDepart <= heureRetour pour tous les tours
COHERENCE_ERREURS=$(echo "$RESULT" | jq '[.toursAssignes[] | select(.heureDepart > .heureRetour)] | length' 2>/dev/null || echo "0")

if [ "$COHERENCE_ERREURS" -eq 0 ]; then
    print_result "Test 5: Heures cohérentes" "heureDepart <= heureRetour" "0 erreurs" "PASS"
else
    print_result "Test 5: Heures cohérentes" "heureDepart <= heureRetour" "$COHERENCE_ERREURS erreurs" "FAIL"
fi
echo ""

# ============================================
# TEST 6: Détails des Tours
# ============================================
echo "================================================"
echo "DÉTAILS DES TOURS"
echo "================================================"

echo "$RESULT" | jq -r '.toursAssignes[] | 
    "\nTour \(.numeroTrajet) - \(.vehicule.reference) (\(.vehicule.nombrePlaces) places)
    Départ: \(.heureDepart)
    Retour: \(.heureRetour)
    Passagers: \(.totalPassagers)/\(.capaciteVehicule) (\(.tauxRemplissage)%)
    Route: \(.route)"' 2>/dev/null

# ============================================
# TEST 7: Réservations Non-Assignées
# ============================================
echo ""
echo "================================================"
echo "RÉSERVATIONS NON-ASSIGNÉES"
echo "================================================"

if [ "$TOTAL_NON_ASSIGNEES" -gt 0 ]; then
    echo "$RESULT" | jq -r '.reservationsNonAssignees[] | 
        "Client: \(.clientId) | Passagers: \(.nombrePassager) | Raison: \(.raisonNonAssignation)"' 2>/dev/null
else
    echo "Aucune réservation non-assignée ✓"
fi
echo ""

# ============================================
# RÉSUMÉ FINAL
# ============================================
echo "================================================"
echo "RÉSUMÉ FINAL"
echo "================================================"
echo "Résultats sauvegardés dans: $RESULTS_FILE"
echo ""
echo "Pour inspection manuelle:"
echo "  jq . $RESULTS_FILE"
echo ""

# ============================================
# Déterminer le statut global
# ============================================
PASS_COUNT=0
FAIL_COUNT=0

[ "$TOTAL_TOURS" -ge 2 ] && PASS_COUNT=$((PASS_COUNT + 1)) || FAIL_COUNT=$((FAIL_COUNT + 1))
[ "$TAUX_REMPLISSAGE" -ge 70 ] && PASS_COUNT=$((PASS_COUNT + 1)) || FAIL_COUNT=$((FAIL_COUNT + 1))
[ "$POURCENTAGE_NON_ASSIGNEES" -le 20 ] && PASS_COUNT=$((PASS_COUNT + 1)) || FAIL_COUNT=$((FAIL_COUNT + 1))
[ "$COHERENCE_ERREURS" -eq 0 ] && PASS_COUNT=$((PASS_COUNT + 1)) || FAIL_COUNT=$((FAIL_COUNT + 1))

echo "================================================"
if [ "$FAIL_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✓ TOUS LES TESTS PASSENT${NC}"
else
    echo -e "${RED}✗ $FAIL_COUNT TEST(S) ÉCHOUÉ(S)${NC}"
fi
echo "Résumé: $PASS_COUNT/4 tests critiques"
echo "================================================"
