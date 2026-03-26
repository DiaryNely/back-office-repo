-- ============================================
-- SPRINT 8 - TEST 6 SEULEMENT
-- Priorité Réservations Non-Assignées
-- Date: 26 mars 2026
-- ============================================

-- 1. Vider les réservations existantes
DELETE FROM reservations WHERE date_heure_arrivee::date = '2026-03-26';

-- 2. Paramètres globaux
UPDATE parametre SET temps_attente = 30, vitesse_moyenne = 50.00;

-- 3. Horaires de disponibilité des véhicules
UPDATE vehicules SET heure_disponibilite = '08:00:00' WHERE id = 1;
UPDATE vehicules SET heure_disponibilite = '09:00:00' WHERE id = 2;
UPDATE vehicules SET heure_disponibilite = '10:00:00' WHERE id = 3;
UPDATE vehicules SET heure_disponibilite = '11:00:00' WHERE id = 4;

-- ============================================
-- TEST 6: Priorité Réservations Non-Assignées
-- ============================================
-- Scénario:
-- 
-- CYCLE 1 (Sursaturation):
-- - VEH-001 (12 places) disponible à 08:00
-- - VEH-002 (5 places) disponible à 09:00  
-- - Arrivées: 08:15 (8 pax), 08:30 (7 pax), 08:45 (6 pax)
-- - Total: 21 passagers, capacité: 17 places
-- - Résultat: 17 assignés, 4 NON-ASSIGNÉS (derrière 08:45)
--
-- CYCLE 2 (Nouveau groupe - Arrivées tardives):
-- - Nouvelles réservations: 16:00 (3 pax), 16:15 (2 pax)
-- - Total cycle 2: 5 passagers
--
-- ATTENDU:
-- ✓ CYCLE 1: 
--    - VEH-001: 12 passagers (8 + 4 du groupe 7)
--    - VEH-002: 5 passagers (7 ou ses 5 places)
--    - NON-ASSIGNÉES: 4 passagers (reste du groupe de 7) ou 6 (groupe complet)
-- ✓ CYCLE 2:
--    - Les 4 NON-ASSIGNÉES du cycle 1 sont TRAITÉES EN PRIORITÉ
--    - Puis les 5 nouveaux passagers
--    - Résultat: 9 assignés au cycle 2
--
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
-- CYCLE 1 - Sursaturation
('0014', 8, '2026-03-26 08:15:00', 3),   -- Grand groupe 1
('0015', 7, '2026-03-26 08:30:00', 4),   -- Grand groupe 2 (sera fractionné)
('0016', 6, '2026-03-26 08:45:00', 1),   -- Grand groupe 3

-- CYCLE 2 - Nouveau groupe avec réservations non-assignées du cycle 1
('0017', 3, '2026-03-26 16:00:00', 1),   -- Petit groupe 1
('0018', 2, '2026-03-26 16:15:00', 2);   -- Petit groupe 2

-- ============================================
-- VÉRIFICATION DES DONNÉES
-- ============================================
SELECT '=== TEST 6 - Données Chargées ===' AS info;

SELECT 'Véhicules:' AS info;
SELECT id, reference, nombre_places, heure_disponibilite FROM vehicules ORDER BY id;

SELECT 'Paramètres:' AS info;
SELECT temps_attente, vitesse_moyenne FROM parametre LIMIT 1;

SELECT 'Résumé Réservations:' AS info;
SELECT 
    COUNT(*) as total_reservations,
    SUM(nombre_passager) as total_passagers,
    COUNT(*) FILTER (WHERE date_heure_arrivee < '2026-03-26 12:00:00') as cycle1,
    COUNT(*) FILTER (WHERE date_heure_arrivee >= '2026-03-26 12:00:00') as cycle2
FROM reservations 
WHERE date_heure_arrivee::date = '2026-03-26';

SELECT 'Réservations détail:' AS info;
SELECT id, client_id, nombre_passager, date_heure_arrivee, id_hotel 
FROM reservations 
WHERE date_heure_arrivee::date = '2026-03-26'
ORDER BY date_heure_arrivee;

-- ============================================
-- DESCRIPTION TEST 6
-- ============================================
-- 
-- CYCLE 1 - Capacité insuffisante:
-- ┌─────────────────────────────────────────────┐
-- │ Heure    │ Client │ Passagers │ Hôtel       │
-- ├─────────────────────────────────────────────┤
-- │ 08:15    │ 0014   │     8     │ H03 (Colb)  │
-- │ 08:30    │ 0015   │     7     │ H04 (Loka)  │  
-- │ 08:45    │ 0016   │     6     │ H01 (Ibis)  │
-- │ TOTAL    │        │    21     │             │
-- └─────────────────────────────────────────────┘
--
-- Véhicules dispo:
--  - VEH-001 (12 places, retour 08:00)
--  - VEH-002 (5 places, retour 09:00)
--  - Total capacité: 17 places
--
-- Résultat attendu CYCLE 1:
--  - VEH-001 prend 12 pax (8 + 4 de 0015)
--  - VEH-002 prend 5 pax (reste de 0015 = 3, plus 2 de 0016)
--  - NON-ASSIGNÉES: 4 pax (reste de 0016)
--
-- CYCLE 2 - Nouveau groupe avec redépart:
-- ┌─────────────────────────────────────────────┐
-- │ Heure    │ Client │ Passagers │ Hôtel       │
-- ├─────────────────────────────────────────────┤
-- │ 16:00    │ 0017   │     3     │ H01 (Ibis)  │
-- │ 16:15    │ 0018   │     2     │ H02 (Novo)  │
-- │ TOTAL    │        │     5     │             │
-- └─────────────────────────────────────────────┘
--
-- Ce qui doit se passer:
-- ✓ Les 4 NON-ASSIGNÉES (rest of 0016) sont EMBARQUÉES EN PRIORITÉ
-- ✓ Puis les 5 nouveaux (0017 + 0018)
-- ✓ Order: [0016-rest(4) → 0017(3) → 0018(2)]
--
-- ============================================
-- VÉRIFICATION FINALE
-- ============================================
SELECT '=== PRÊT POUR EXÉCUTION ===' AS info;
SELECT 'Exécutez la planification:' as instruction;
SELECT 'http://localhost:8080/back-office/planification?date=2026-03-26' as url;

SELECT 'Vérifiez:' as checklist;
SELECT '✓ Nombre de réservations non-assignées CYCLE 1' as check1;
SELECT '✓ Réservations non-assignées traitées EN PRIORITÉ CYCLE 2' as check2;
SELECT '✓ Ordre d''assignation: non-assignées → nouvelles' as check3;
SELECT '✓ Taux remplissage optimal' as check4;
