package com.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.example.database.DatabaseConnection;
import com.example.model.Token;

public class TokenDAO {

    public Token findByToken(String tokenValue) throws SQLException {
        if (tokenValue == null || tokenValue.isBlank()) {
            return null;
        }

        String sql = "SELECT id, token, date_expiration FROM tokens WHERE token = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tokenValue.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                Token token = new Token();
                token.setId(resultSet.getInt("id"));
                token.setToken(resultSet.getString("token"));
                Timestamp expiration = resultSet.getTimestamp("date_expiration");
                token.setDateExpiration(expiration != null ? expiration.toLocalDateTime() : null);
                return token;
            }
        }
    }

    public int insert(String tokenValue, LocalDateTime dateExpiration) throws SQLException {
        String sql = "INSERT INTO tokens (token, date_expiration) VALUES (?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, tokenValue);
            statement.setTimestamp(2, Timestamp.valueOf(dateExpiration));
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