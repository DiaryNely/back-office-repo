package com.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.example.database.DatabaseConnection;
import com.example.model.Hotel;

public class HotelDAO {

    public List<Hotel> findAll() throws SQLException {
        String sql = "SELECT id, nom, adresse FROM hotels ORDER BY nom ASC";
        List<Hotel> hotels = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Hotel hotel = new Hotel();
                hotel.setId(resultSet.getInt("id"));
                hotel.setNom(resultSet.getString("nom"));
                hotel.setAdresse(resultSet.getString("adresse"));
                hotels.add(hotel);
            }
        }

        return hotels;
    }

    public boolean existsById(Integer idHotel) throws SQLException {
        if (idHotel == null) {
            return false;
        }

        String sql = "SELECT 1 FROM hotels WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idHotel);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
