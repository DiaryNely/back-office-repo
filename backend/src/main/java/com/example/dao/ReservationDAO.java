package com.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.database.DatabaseConnection;
import com.example.model.Reservation;
import com.example.model.ReservationListItem;

public class ReservationDAO {

    public List<ReservationListItem> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT id, client_id, nombre_passager, TO_CHAR(date_heure_arrivee, 'YYYY-MM-DD\"T\"HH24:MI:SS') AS date_heure_arrivee, id_hotel FROM reservations");

        List<Object> params = new ArrayList<>();

        if (dateDebut != null || dateFin != null) {
            sql.append(" WHERE ");

            if (dateDebut != null) {
                sql.append("date(date_heure_arrivee) >= ?");
                params.add(dateDebut);
            }

            if (dateDebut != null && dateFin != null) {
                sql.append(" AND ");
            }

            if (dateFin != null) {
                sql.append("date(date_heure_arrivee) <= ?");
                params.add(dateFin);
            }
        }

        sql.append(" ORDER BY date_heure_arrivee DESC");

        List<ReservationListItem> reservations = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int index = 0; index < params.size(); index++) {
                Object param = params.get(index);
                if (param instanceof LocalDate localDate) {
                    statement.setDate(index + 1, java.sql.Date.valueOf(localDate));
                } else {
                    statement.setObject(index + 1, param);
                }
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ReservationListItem item = new ReservationListItem();
                    item.setId(resultSet.getInt("id"));
                    item.setClientId(resultSet.getString("client_id"));
                    item.setNombrePassager(resultSet.getInt("nombre_passager"));
                    item.setDateHeureArrivee(resultSet.getString("date_heure_arrivee"));
                    item.setIdHotel(resultSet.getInt("id_hotel"));
                    reservations.add(item);
                }
            }
        }

        return reservations;
    }

    public int insert(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservations (client_id, nombre_passager, date_heure_arrivee, id_hotel) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, reservation.getClientId());
            statement.setInt(2, reservation.getNombrePassager());
            statement.setTimestamp(3, Timestamp.valueOf(reservation.getDateHeureArrivee()));
            statement.setInt(4, reservation.getIdHotel());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        return -1;
    }
}
