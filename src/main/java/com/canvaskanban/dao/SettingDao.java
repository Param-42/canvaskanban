package com.canvaskanban.dao;

import com.canvaskanban.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * A tiny key/value store for application settings, for example the theme of the
 * last opened board or the id of the board that was open when the app was closed.
 */
public class SettingDao {

    private final Database database;

    public SettingDao(Database database) {
        this.database = database;
    }

    public Optional<String> find(String key) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.ofNullable(resultSet.getString("value")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the setting \"" + key + "\".", e);
        }
    }

    public String findOrDefault(String key, String fallback) {
        return find(key).orElse(fallback);
    }

    /** Adds the setting or replaces its previous value. */
    public void save(String key, String value) {
        String sql = "INSERT INTO settings (key, value) VALUES (?, ?) "
                + "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the setting \"" + key + "\".", e);
        }
    }

    public void delete(String key) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM settings WHERE key = ?")) {
            statement.setString(1, key);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the setting \"" + key + "\".", e);
        }
    }
}
