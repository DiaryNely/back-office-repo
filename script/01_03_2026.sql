-- SPRINT 3 – Affectation des véhicules aux réservations
-- Date: 01/03/2026

-- ================================================================
-- 1. TABLE PARAMETRE
-- Paramètres globaux pour les calculs de trajet
-- ================================================================
CREATE TABLE IF NOT EXISTS parametre (
    id              SERIAL PRIMARY KEY,
    vitesse_moyenne NUMERIC(6,2) NOT NULL DEFAULT 50.00,  -- km/h
    temps_attente   INTEGER      NOT NULL DEFAULT 10,      -- minutes entre trajets
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Paramètre par défaut
INSERT INTO parametre (vitesse_moyenne, temps_attente)
VALUES (50.00, 10)
ON CONFLICT DO NOTHING;

-- ================================================================
-- 2. TABLE LIEU
-- Stocke tous les lieux : hôtels ET aéroports
-- ================================================================
CREATE TABLE IF NOT EXISTS lieu (
    id      SERIAL PRIMARY KEY,
    code    VARCHAR(10)  NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    type    VARCHAR(10)  NOT NULL CHECK (type IN ('HOTEL', 'AEROPORT')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lieu_code ON lieu(code);
CREATE INDEX IF NOT EXISTS idx_lieu_type ON lieu(type);

-- Données de test : 1 aéroport + quelques hôtels
INSERT INTO lieu (code, libelle, type) VALUES
('AER',  'Aéroport d''Ivato',     'AEROPORT'),
('H01',  'Hôtel Ibis Antananarivo', 'HOTEL'),
('H02',  'Carlton Madagascar',     'HOTEL'),
('H03',  'Hôtel Colbert',          'HOTEL'),
('H04',  'Sakamanga Hotel',        'HOTEL')
ON CONFLICT (code) DO NOTHING;

-- ================================================================
-- 3. TABLE DISTANCE
-- Distance en km entre deux lieux (bidirectionnel)
-- temps_trajet = km / vitesse_moyenne  (calculé à la volée)
-- ================================================================
CREATE TABLE IF NOT EXISTS distance (
    id      SERIAL PRIMARY KEY,
    from_id INTEGER NOT NULL REFERENCES lieu(id) ON DELETE CASCADE,
    to_id   INTEGER NOT NULL REFERENCES lieu(id) ON DELETE CASCADE,
    km      NUMERIC(7,2) NOT NULL CHECK (km > 0),
    CONSTRAINT uq_distance UNIQUE (from_id, to_id)
);

CREATE INDEX IF NOT EXISTS idx_distance_from ON distance(from_id);
CREATE INDEX IF NOT EXISTS idx_distance_to   ON distance(to_id);

-- Distances de test (aéroport ↔ hôtels)
INSERT INTO distance (from_id, to_id, km)
SELECT l_from.id, l_to.id, d.km
FROM (VALUES
    ('AER','H01', 15.0),
    ('AER','H02', 12.5),
    ('AER','H03', 13.8),
    ('AER','H04', 14.2),
    ('H01','AER', 15.0),
    ('H02','AER', 12.5),
    ('H03','AER', 13.8),
    ('H04','AER', 14.2),
    ('H01','H02',  3.5),
    ('H02','H01',  3.5),
    ('H01','H03',  2.2),
    ('H03','H01',  2.2),
    ('H02','H03',  1.8),
    ('H03','H02',  1.8)
) AS d(from_code, to_code, km)
JOIN lieu l_from ON l_from.code = d.from_code
JOIN lieu l_to   ON l_to.code   = d.to_code
ON CONFLICT (from_id, to_id) DO NOTHING;

-- ================================================================
-- 4. Colonne id_vehicule dans reservations (si pas encore présente)
-- ================================================================
ALTER TABLE reservations

    ADD COLUMN IF NOT EXISTS id_vehicule INTEGER REFERENCES vehicules(id) ON DELETE SET NULL;

-- ================================================================
-- Vérification finale
-- ================================================================
SELECT 'parametre' AS table_name, COUNT(*) AS rows FROM parametre
UNION ALL
SELECT 'lieu',     COUNT(*) FROM lieu
UNION ALL
SELECT 'distance', COUNT(*) FROM distance;
