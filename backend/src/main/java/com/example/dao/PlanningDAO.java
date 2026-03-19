package com.example.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.database.DatabaseConnection;
import com.example.model.PlanningParametre;
import com.example.model.PlanningReservation;

public class PlanningDAO {

    public List<PlanningReservation> findReservationsByDate(LocalDate date) throws SQLException {
        String sql = """
                SELECT r.id,
                       r.client_id,
                       r.nombre_passager,
                       r.date_heure_arrivee,
                       r.id_hotel,
                       h.nom AS hotel_nom,
                       COALESCE(l.code, ('H' || LPAD(r.id_hotel::text, 2, '0'))) AS lieu_code,
                       COALESCE(l.libelle, h.nom) AS lieu_libelle,
                       r.id_vehicule,
                       v.reference AS vehicule_reference
                FROM reservations r
                LEFT JOIN hotels h ON h.id = r.id_hotel
                LEFT JOIN lieu l ON l.code = ('H' || LPAD(r.id_hotel::text, 2, '0'))
                LEFT JOIN vehicules v ON v.id = r.id_vehicule
                WHERE DATE(r.date_heure_arrivee) = ?
                ORDER BY r.date_heure_arrivee ASC, r.id ASC
                """;

        List<PlanningReservation> reservations = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDate(1, java.sql.Date.valueOf(date));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    PlanningReservation reservation = new PlanningReservation();
                    reservation.setId(resultSet.getInt("id"));
                    reservation.setClientId(resultSet.getString("client_id"));
                    reservation.setNombrePassager(resultSet.getInt("nombre_passager"));

                    Timestamp dateHeureArrivee = resultSet.getTimestamp("date_heure_arrivee");
                    reservation
                            .setDateHeureArrivee(dateHeureArrivee != null ? dateHeureArrivee.toLocalDateTime() : null);

                    reservation.setIdHotel(resultSet.getInt("id_hotel"));
                    reservation.setHotelNom(resultSet.getString("hotel_nom"));
                    reservation.setLieuCode(resultSet.getString("lieu_code"));
                    reservation.setLieuLibelle(resultSet.getString("lieu_libelle"));

                    int idVehicule = resultSet.getInt("id_vehicule");
                    if (!resultSet.wasNull()) {
                        reservation.setIdVehicule(idVehicule);
                    }
                    reservation.setVehiculeReference(resultSet.getString("vehicule_reference"));

                    reservations.add(reservation);
                }
            }
        }

        return reservations;
    }

    public void clearAssignmentsByDate(LocalDate date) throws SQLException {
        String sql = "UPDATE reservations SET id_vehicule = NULL WHERE DATE(date_heure_arrivee) = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, java.sql.Date.valueOf(date));
            statement.executeUpdate();
        }
    }

    public void assignVehicule(Integer reservationId, Integer vehiculeId) throws SQLException {
        String sql = "UPDATE reservations SET id_vehicule = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, vehiculeId);
            statement.setInt(2, reservationId);
            statement.executeUpdate();
        }
    }

    public PlanningParametre getPlanningParametre() throws SQLException {
        String sql = "SELECT vitesse_moyenne, temps_attente FROM parametre ORDER BY id DESC LIMIT 1";

        PlanningParametre parametre = new PlanningParametre();
        parametre.setVitesseMoyenne(BigDecimal.valueOf(50.0));
        parametre.setTempsAttente(10);

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                BigDecimal vitesse = resultSet.getBigDecimal("vitesse_moyenne");
                int tempsAttente = resultSet.getInt("temps_attente");

                if (vitesse != null && vitesse.doubleValue() > 0) {
                    parametre.setVitesseMoyenne(vitesse);
                }

                if (tempsAttente >= 0) {
                    parametre.setTempsAttente(tempsAttente);
                }
            }
        }

        return parametre;
    }

    public Map<String, BigDecimal> getDistanceMap() throws SQLException {
        String sql = """
                SELECT lf.code AS from_code,
                       lt.code AS to_code,
                       d.km
                FROM distance d
                JOIN lieu lf ON lf.id = d.from_id
                JOIN lieu lt ON lt.id = d.to_id
                """;

        Map<String, BigDecimal> distances = new HashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String fromCode = resultSet.getString("from_code");
                String toCode = resultSet.getString("to_code");
                BigDecimal km = resultSet.getBigDecimal("km");

                if (fromCode != null && toCode != null && km != null) {
                    distances.put(distanceKey(fromCode, toCode), km);
                }
            }
        }

        return distances;
    }

    private String distanceKey(String from, String to) {
        return from + "->" + to;
    }
}