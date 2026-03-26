-- ============================================
-- SPRINT 8 - TEST 1 SEULEMENT
-- Départ Immédiat - Véhicule Retournant
-- Date: 26 mars 2026
-- ============================================

-- 1. Vider les réservations existantes
DELETE FROM reservations WHERE date_heure_arrivee::date = '2026-03-26';

-- 2. Paramètres globaux
UPDATE parametre SET temps_attente = 15, vitesse_moyenne = 50.00;

-- 3. Horaires de disponibilité des véhicules
UPDATE vehicules SET heure_disponibilite = '08:00:00' WHERE id = 1;
UPDATE vehicules SET heure_disponibilite = '09:00:00' WHERE id = 2;
UPDATE vehicules SET heure_disponibilite = '10:00:00' WHERE id = 3;
UPDATE vehicules SET heure_disponibilite = '11:00:00' WHERE id = 4;

-- ============================================
-- TEST 1: Départ Immédiat - Véhicule Retournant
-- ============================================
-- Scénario:
-- - VEH-001 retourne à 08:00 avec capacité dispo (12 places)
-- - Réservations arrivent immédiatement après (08:15, 08:30, 08:45)
-- - Total: 3 + 4 + 2 = 9 passagers
-- 
-- Attendu:
-- ✓ VEH-001 redépart IMMÉDIATEMENT (départ impromptu autour de 08:15)
-- ✓ Prend les 9 passagers en 1 tour (capacité = 12 places)
-- ✓ Taux remplissage = 75% (9/12)
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('0001', 3, '2026-03-26 08:15:00', 1),
('0002', 4, '2026-03-26 08:30:00', 2),
('0003', 2, '2026-03-26 08:45:00', 3);

-- ============================================
-- VÉRIFICATION DES DONNÉES
-- ============================================
SELECT '=== TEST 1 - Données Chargées ===' AS info;

SELECT 'Véhicules:' AS info;
SELECT id, reference, nombre_places, heure_disponibilite FROM vehicules ORDER BY id;

SELECT 'Paramètres:' AS info;
SELECT temps_attente, vitesse_moyenne FROM parametre LIMIT 1;

SELECT 'Réservations du 26/03/2026:' AS info;
SELECT id, client_id, nombre_passager, date_heure_arrivee, id_hotel 
FROM reservations 
WHERE date_heure_arrivee::date = '2026-03-26'
ORDER BY date_heure_arrivee;

SELECT 'Résumé:' AS info;
SELECT 
    COUNT(*) as total_reservations,
    SUM(nombre_passager) as total_passagers
FROM reservations 
WHERE date_heure_arrivee::date = '2026-03-26';

-- ============================================
-- STATUT: PRÊT POUR EXÉCUTION
-- ============================================
-- Exécutez la planification:
-- http://localhost:8080/back-office/planification?date=2026-03-26
-- 
-- Vérifiez dans les résultats:
-- ✓ Nombre de tours créés pour VEH-001
-- ✓ Nombre total de passagers assignés (9)
-- ✓ Taux de remplissage (environ 75%)
-- ✓ Horaires de départ réels
