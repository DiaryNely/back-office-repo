-- SPRINT 2 - Création des tables VEHICULE et TOKEN
-- Date: 09/02/2026
-- Mis à jour: 13/02/2026

-- Table TYPE_CARBURANT
CREATE TABLE IF NOT EXISTS type_carburant (
    id SERIAL PRIMARY KEY,
    code VARCHAR(2) NOT NULL UNIQUE,
    nom VARCHAR(50) NOT NULL
);

-- Insertion des types de carburant
INSERT INTO type_carburant (code, nom) VALUES
('D', 'Diesel'),
('E', 'Essence'),
('H', 'Hybride'),
('EL', 'Electrique');

-- Table VEHICULE
CREATE TABLE IF NOT EXISTS vehicules (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    nombre_places INT NOT NULL CHECK (nombre_places > 0),
    type_carburant_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (type_carburant_id) REFERENCES type_carburant(id)
);

-- Table TOKEN pour la protection des API
CREATE TABLE IF NOT EXISTS tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    date_expiration DATE NOT NULL,
    heure_expiration TIME NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour améliorer les performances de recherche
CREATE INDEX IF NOT EXISTS idx_tokens_token ON tokens(token);
CREATE INDEX IF NOT EXISTS idx_tokens_expiration ON tokens(date_expiration, heure_expiration);
CREATE INDEX IF NOT EXISTS idx_vehicules_reference ON vehicules(reference);

-- Données de test pour les véhicules
INSERT INTO vehicules (reference, nombre_places, type_carburant_id) VALUES
('VEH-001', 5, (SELECT id FROM type_carburant WHERE code = 'E')),
('VEH-002', 7, (SELECT id FROM type_carburant WHERE code = 'D')),
('VEH-003', 4, (SELECT id FROM type_carburant WHERE code = 'EL')),
('VEH-004', 5, (SELECT id FROM type_carburant WHERE code = 'H')),
('VEH-005', 9, (SELECT id FROM type_carburant WHERE code = 'D')),
('VEH-006', 2, (SELECT id FROM type_carburant WHERE code = 'E')),
('VEH-007', 5, (SELECT id FROM type_carburant WHERE code = 'EL'));

-- Affichage des données créées
SELECT * FROM type_carburant ORDER BY id;
SELECT v.*, tc.code, tc.nom FROM vehicules v 
JOIN type_carburant tc ON v.type_carburant_id = tc.id 
ORDER BY v.id;
