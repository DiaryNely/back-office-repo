-- ============================================
-- SPRINT 8 - DONNÉES DE TEST
-- Gestion Dynamique des Véhicules
-- Date: 26 mars 2026
-- ============================================

-- ============================================
-- PRÉ-REQUIS: Exécuter d'abord SPRINT8_migration.sql
-- ============================================

-- 1. Vider les réservations existantes pour ce jour
DELETE FROM reservations WHERE date_heure_arrivee::date = '2026-03-26';

-- 2. Paramètres globaux
UPDATE parametre SET temps_attente = 15, vitesse_moyenne = 50.00;

-- 3. Horaires de disponibilité des véhicules (quand le véhicule revient)
-- Ajuster en fonction de vos véhicules réels
UPDATE vehicules SET heure_disponibilite = '08:00:00' WHERE id = 1;
UPDATE vehicules SET heure_disponibilite = '09:00:00' WHERE id = 2;
UPDATE vehicules SET heure_disponibilite = '10:00:00' WHERE id = 3;
UPDATE vehicules SET heure_disponibilite = '11:00:00' WHERE id = 4;

-- ============================================
-- TEST 1: Départ Immédiat - Véhicule Retournant
-- ============================================
-- Scénario:
-- - VEH-001 retourne à 08:00 avec capacité dispo
-- - Réservations arrivent immédiatement après
-- Attendu: VEH-001 redépart immédiatement (départ impromptu)
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('0001', 3, '2026-03-26 08:15:00', 1),
('0002', 4, '2026-03-26 08:30:00', 2),
('0003', 2, '2026-03-26 08:45:00', 3);

-- ============================================
-- TEST 2: Capacité Exacte
-- ============================================
-- Scénario:
-- - VEH-002 retourne à 09:00
-- - Réservation de passagers = capacité exacte
-- Attendu: VEH-002 redépart avec remplissage optimal
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('0004', 5, '2026-03-26 09:15:00', 1),
('0005', 3, '2026-03-26 09:30:00', 2);

-- ============================================
-- TEST 3: Fractionnement au Retour
-- ============================================
-- Scénario:
-- - VEH-003 retourne à 10:00 avec capacité dispo
-- - Réservations dépassent capacité = fractionnement
-- Attendu: Fractionné entre plusieurs tours ou impromptu
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('0006', 6, '2026-03-26 10:15:00', 3),
('0007', 7, '2026-03-26 10:45:00', 4),
('0008', 4, '2026-03-26 11:00:00', 1);

-- ============================================
-- TEST 4: Attendre - Temps Faible (< 15 min)
-- ============================================
-- Scénario:
-- - VEH-004 retourne à 11:00
-- - Petite réservation + attente courte
-- Attendu: VEH-004 attend le prochain regroupement (pas impromptu)
-- Résumé: Une simple attente d'env. 10-15 min
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('0009', 2, '2026-03-26 11:30:00', 2),
('0010', 3, '2026-03-26 11:40:00', 3);

-- ============================================
-- TEST 5: Partir - Temps Élevé (> 15 min)
-- ============================================
-- Scénario:
-- - VEH-001 redisponible à 12:30
-- - Grandes attentes entre arrivées (> 30 min)
-- Attendu: VEH-001 part immédiatement (impromptu) avec les réservés
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('0011', 2, '2026-03-26 13:00:00', 1),
('0012', 1, '2026-03-26 14:00:00', 2);

-- ============================================
-- TEST 6: Priorité Réservations Non-Assignées
-- ============================================
-- Scénario:
-- - Cycle 1: Trop de réservations → certaines non assignées
-- - Cycle 2: Véhicule revient → réservations non-assignées traitées en priorité
-- Attendu: Réservations non-assignées embarquées avant les nouvelles
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('0013', 8, '2026-03-26 15:00:00', 3),
('0014', 7, '2026-03-26 15:15:00', 4),
('0015', 3, '2026-03-26 16:00:00', 1),
('0016', 2, '2026-03-26 16:30:00', 2);

-- ============================================
-- VÉRIFICATION DES DONNÉES CHARGÉES
-- ============================================
SELECT '=== SPRINT 8 - Données Chargées ===' AS info;
SELECT COUNT(*) as total_reservations FROM reservations WHERE date_heure_arrivee::date = '2026-03-26';
SELECT 'Paramètres:' AS info;
SELECT temps_attente, vitesse_moyenne FROM parametre LIMIT 1;
SELECT 'Véhicules et horaires:' AS info;
SELECT id, reference, nombre_places, heure_disponibilite FROM vehicules ORDER BY id;
SELECT 'Réservations du 26/03/2026:' AS info;
SELECT id, client_id, nombre_passager, date_heure_arrivee, id_hotel FROM reservations 
WHERE date_heure_arrivee::date = '2026-03-26'
ORDER BY date_heure_arrivee;

-- ============================================
-- STATUT: PRÊT POUR LES TESTS
-- ============================================
-- - Cycle 2: T6-002 traitée EN PRIORITÉ avant T6-003, T6-004
-- - Ordre traitement: [T6-002(carryIn), T6-003, T6-004]
-- - assignmentTrackers: T6-002.assignedPassengers mis à jour EN PREMIER

-- ============================================
-- TEST 7: Sélection Passagers Optimale
-- ============================================
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('0017', 4, '2026-03-26 22:00:00', 1),   -- 4 passagers utilisés
('0018', 8, '2026-03-26 22:30:00', 2),   -- Groupe de 8 (le plus grand)
('0019', 3, '2026-03-26 22:45:00', 3),   -- Groupe de 3
('0020', 2, '2026-03-26 23:00:00', 4);   -- Groupe de 2

-- VEH-002 capacité: 7 places
-- Après T7-001(4): 7 - 4 = 3 places
-- Sélection (plus gros d'abord):
--  1. T7-002(8) → prend 3 seulement → fractionné en 3 + 5
--  2. T7-003,T7-004 non sélectionnés

-- Résultat attendu:
-- - T7-002-P1: 3 passagers assignés
-- - T7-002-P2: 5 passagers non assignés
-- - T7-003: 3 passagers non assignés
-- - T7-004: 2 passagers non assignés
-- - Raison: "Capacité insuffisante après fractionnement"

-- ============================================
-- VÉRIFICATION FINALE
-- ============================================
SELECT 
    '=== RÉSERVATIONS 26 MARS 2026 ===' AS info,
    COUNT(*) as total_reservations,
    SUM(nombre_passager) as total_passagers
FROM reservations 
WHERE date_heure_arrivee::date = '2026-03-26';

SELECT 
    '=== VÉHICULES DISPONIBLES ===' AS info,
    id,
    reference,
    nombre_places,
    heure_disponibilite
FROM vehicules 
ORDER BY id;

-- ============================================
-- REQUÊTE DE TEST
-- ============================================
-- Exécuter la planification sur cette date dans l'interface web:
-- http://localhost:8080/back-office/planification?date=2026-03-26
-- GET /back-office/planification/result?date=2026-03-26
