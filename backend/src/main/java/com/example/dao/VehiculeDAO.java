package com.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.example.database.DatabaseConnection;
import com.example.model.TypeCarburant;
import com.example.model.Vehicule;

public class VehiculeDAO {

    public List<Vehicule> findAll() throws SQLException {
        String sql = """
                SELECT v.id, v.reference, v.nombre_places, v.type_carburant_id,
                       tc.code AS type_code, tc.nom AS type_nom
                FROM vehicules v
                JOIN type_carburant tc ON tc.id = v.type_carburant_id
                ORDER BY v.id DESC
                """;

        List<Vehicule> vehicules = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                vehicules.add(mapVehicule(resultSet));
            }
        }

        return vehicules;
    }

    public Vehicule findById(Integer id) throws SQLException {
        if (id == null) {
            return null;
        }

        String sql = """
                SELECT v.id, v.reference, v.nombre_places, v.type_carburant_id,
                       tc.code AS type_code, tc.nom AS type_nom
                FROM vehicules v
                JOIN type_carburant tc ON tc.id = v.type_carburant_id
                WHERE v.id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapVehicule(resultSet);
                }
            }
        }

        return null;
    }

    public int insert(Vehicule vehicule) throws SQLException {
        String sql = "INSERT INTO vehicules (reference, nombre_places, type_carburant_id) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, vehicule.getReference());
            statement.setInt(2, vehicule.getNombrePlaces());
            statement.setInt(3, vehicule.getTypeCarburantId());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        return -1;
    }

    public boolean update(Vehicule vehicule) throws SQLException {
        String sql = "UPDATE vehicules SET reference = ?, nombre_places = ?, type_carburant_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, vehicule.getReference());
            statement.setInt(2, vehicule.getNombrePlaces());
            statement.setInt(3, vehicule.getTypeCarburantId());
            statement.setInt(4, vehicule.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(Integer id) throws SQLException {
        if (id == null) {
            return false;
        }

        String sql = "DELETE FROM vehicules WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean existsByReference(String reference) throws SQLException {
        if (reference == null || reference.isBlank()) {
            return false;
        }

        String sql = "SELECT 1 FROM vehicules WHERE reference = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, reference.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean existsByReferenceExcludingId(String reference, Integer id) throws SQLException {
        if (reference == null || reference.isBlank()) {
            return false;
        }

        String sql = "SELECT 1 FROM vehicules WHERE reference = ? AND id <> ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, reference.trim());
            statement.setInt(2, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean existsTypeCarburant(Integer typeCarburantId) throws SQLException {
        if (typeCarburantId == null) {
            return false;
        }

        String sql = "SELECT 1 FROM type_carburant WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, typeCarburantId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<TypeCarburant> findTypesCarburant() throws SQLException {
        String sql = "SELECT id, code, nom FROM type_carburant ORDER BY id ASC";
        List<TypeCarburant> types = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                TypeCarburant type = new TypeCarburant();
                type.setId(resultSet.getInt("id"));
                type.setCode(resultSet.getString("code"));
                type.setNom(resultSet.getString("nom"));
                types.add(type);
            }
        }

        return types;
    }

    private Vehicule mapVehicule(ResultSet resultSet) throws SQLException {
        Vehicule vehicule = new Vehicule();
        vehicule.setId(resultSet.getInt("id"));
        vehicule.setReference(resultSet.getString("reference"));
        vehicule.setNombrePlaces(resultSet.getInt("nombre_places"));
        vehicule.setTypeCarburantId(resultSet.getInt("type_carburant_id"));
        vehicule.setTypeCarburantCode(resultSet.getString("type_code"));
        vehicule.setTypeCarburantNom(resultSet.getString("type_nom"));
        return vehicule;
    }
}
