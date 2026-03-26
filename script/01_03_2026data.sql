hotel_db=# -- ============================================
-- DONNÉES DE TEST PLANIFICATION - MARS 2026
-- À exécuter : psql -U postgres -d hotel_db -f 04_03_2026_test_mars.sql
-- Dates à tester : 5, 6, 7 mars 2026
-- ============================================

-- Corriger la table lieu pour matcher les noms d'hôtels
UPDATE lieu SET libelle = 'Hotel Ibis' WHERE code = 'H01';
UPDATE lieu SET libelle = 'Hotel Novotel' WHERE code = 'H02';
UPDATE lieu SET libelle = 'Hotel Colbert' WHERE code = 'H03';
UPDATE lieu SET libelle = 'Hotel Lokanga' WHERE code = 'H04';

-- Ajouter lieu Aéroport s'il n'est pas lié aux distances
-- L'aéroport est id=1 dans lieu

-- ============================================
-- RÉSERVATIONS POUR LE 5 MARS 2026 (6 réservations)
-- ============================================
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('A001', 3, '2026-03-05 08:00:00', 1),  -- 3 passagers, Hotel Colbert
('A002', 2, '2026-03-05 09:30:00', 2),  -- 2 passagers, Hotel Novotel
('A003', 6, '2026-03-05 10:00:00', 3),  -- 6 passagers, Hotel Ibis
('A004', 4, '2026-03-05 11:30:00', 4),  -- 4 passagers, Hotel Lokanga
('A005', 8, '2026-03-05 14:00:00', 1),  -- 8 passagers, Hotel Colbert
('A006', 5, '2026-03-05 16:00:00', 2);  -- 5 passagers, Hotel Novotel

-- ============================================
-- RÉSERVATIONS POUR LE 6 MARS 2026 (4 réservations)
-- ============================================
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('B001', 4, '2026-03-06 08:00:00', 1),  -- Test capacité exacte 4 places (VEH-003)
('B002', 7, '2026-03-06 10:00:00', 3),  -- Test capacité exacte 7 places (VEH-002)
('B003', 5, '2026-03-06 12:00:00', 2),  -- Test capacité 5 places (priorité Diesel)
('B004', 9, '2026-03-06 14:30:00', 4);  -- Test capacité exacte 9 places (VEH-005)

-- ============================================
-- RÉSERVATIONS POUR LE 7 MARS 2026 (3 réservations)
-- Test priorité Diesel : plusieurs véhicules 5 places
-- VEH-001 (5 places, Essence), VEH-004 (5 places, Hybride), VEH-007 (5 places, Electrique)
-- ============================================
INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('C001', 5, '2026-03-07 09:00:00', 1),  -- 5 passagers → devrait prendre un 5 places
('C002', 5, '2026-03-07 11:00:00', 2),  -- 5 passagers → devrait prendre un autre 5 places
('C003', 5, '2026-03-07 14:00:00', 3);  -- 5 passagers → devrait prendre le dernier 5 places

ORDER BY date;date_heure_arrivee)-03-05'sagers= tc.idcarburante < '2026-03-08'
UPDATE 1
UPDATE 1
UPDATE 1
UPDATE 1
INSERT 0 6
INSERT 0 4
INSERT 0 3
              info              
--------------------------------
 === Réservations MARS 2026 ===
(1 row)

ERROR:  column reference "id" is ambiguous
LINE 1: SELECT id, client_id, nombre_passager, 
               ^
             info              
-------------------------------
 === Véhicules disponibles ===
(1 row)

 id | reference | nombre_places | carburant  
----+-----------+---------------+------------
  6 | VEH-006   |             2 | Essence
  3 | VEH-003   |             4 | Electrique
  7 | VEH-007   |             5 | Electrique
  1 | VEH-001   |             5 | Essence
  4 | VEH-004   |             5 | Hybride
  2 | VEH-002   |             7 | Diesel
  5 | VEH-005   |             9 | Diesel
(7 rows)

          info           
-------------------------
 === Résumé par date ===
(1 row)

    date    | nb_reservations | total_passagers 
------------+-----------------+-----------------
 2026-03-05 |               7 |              31
 2026-03-06 |               4 |              25
 2026-03-07 |               3 |              15
(3 rows)

hotel_db=# 

