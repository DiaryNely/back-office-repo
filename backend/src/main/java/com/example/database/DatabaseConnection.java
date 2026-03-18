package com.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    public static Connection getConnection() throws SQLException {
        String url = getEnvOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/projet");
        String user = getEnvOrDefault("DB_USER", "postgres");
        String password = getEnvOrDefault("DB_PASSWORD", "postgres");

        return DriverManager.getConnection(url, user, password);
    }
}
