drop table reservations;

drop table hotels;

create table hotels (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    adresse VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    client_id VARCHAR(4) NOT NULL,
    nombre_passager INT NOT NULL,
    date_heure_arrivee TIMESTAMP,
    id_hotel INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_hotel) REFERENCES hotels(id)
);


INSERT INTO hotels (nom, adresse) VALUES
('Hotel Colbert', '456 Rue de la Paix, Lyon'),
('Hotel Novotel', '789 Boulevard Haussmann, Marseille'),
('Hotel Ibis', '321 Rue du Commerce, Toulouse'),
('Hotel Lokanga', '654 Avenue Jean Médecin, Nice');

INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES
('4631', 11, '2026-02-05 00:01:00', 3),
('4394', 1,  '2026-02-05 23:55:00', 3),
('8054', 2,  '2026-02-09 10:17:00', 1),
('1432', 4,  '2026-02-01 15:25:00', 2),
('7861', 4,  '2026-01-28 07:11:00', 1),
('3308', 5,  '2026-01-28 07:45:00', 1),
('4484', 13, '2026-02-28 08:25:00', 2),
('9687', 8,  '2026-02-28 13:00:00', 2),
('6302', 7,  '2026-02-15 13:00:00', 1),
('8640', 1,  '2026-02-18 22:55:00', 4);


