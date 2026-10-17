package com.canvaskanban.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for creating the SQLite database. Every test uses a temporary file, so the
 * real application database in the user's home folder is never touched.
 */
class DatabaseTest {

    @TempDir
    Path tempDir;

    private Database newDatabase() {
        return new Database(tempDir.resolve("test.db"));
    }

    @Test
    void createsTheDatabaseFileAndAllTables() throws SQLException {
        Database database = newDatabase();
        assertFalse(Files.exists(database.getDatabaseFile()), "the file should not exist yet");

        database.initialize();

        assertTrue(Files.exists(database.getDatabaseFile()), "the file should have been created");

        List<String> tables = tableNames(database);
        assertTrue(tables.contains("boards"), "boards table missing, found: " + tables);
        assertTrue(tables.contains("tasks"), "tasks table missing, found: " + tables);
        assertTrue(tables.contains("settings"), "settings table missing, found: " + tables);
    }

    @Test
    void initializeCanBeCalledMoreThanOnce() throws SQLException {
        Database database = newDatabase();
        database.initialize();
        database.initialize();
        database.initialize();

        assertEquals(3, tableNames(database).size());
    }

    @Test
    void createsMissingParentFolders() {
        Path nested = tempDir.resolve("a").resolve("b").resolve("data.db");
        Database database = new Database(nested);

        database.initialize();

        assertTrue(Files.exists(nested));
        assertTrue(Files.isDirectory(nested.getParent()));
    }

    @Test
    void foreignKeysAreEnabledOnEveryConnection() throws SQLException {
        Database database = newDatabase();
        database.initialize();

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO boards (name, description, theme, created_at) "
                    + "VALUES ('B', '', 'minimal-light', '2026-01-01')");
            statement.execute("INSERT INTO tasks (board_id, title, description, priority, status, position, created_at) "
                    + "VALUES (1, 'T', '', 'LOW', 'TODO', 0, '2026-01-01')");
            statement.execute("DELETE FROM boards WHERE id = 1");
        }

        assertEquals(0, countRows(database, "tasks"),
                "deleting a board must delete its tasks (ON DELETE CASCADE)");
    }

    @Test
    void reportsAUsefulErrorWhenTheFileCanNotBeCreated() {
        // A folder path is used as the database file, which SQLite can not open.
        Database database = new Database(tempDir);

        Exception error = assertThrows(IllegalStateException.class, database::initialize);
        assertTrue(error.getMessage().contains(tempDir.toString()), error.getMessage());
    }

    @Test
    void dataDirectoryCanBeOverridden() {
        String previous = System.getProperty("canvaskanban.data.dir");
        Path expected = tempDir.resolve("app-data");
        try {
            System.setProperty("canvaskanban.data.dir", expected.toString());

            assertEquals(expected.toAbsolutePath(), AppPaths.dataDirectory());
            assertEquals(expected.resolve("canvaskanban.db").toAbsolutePath(), AppPaths.databaseFile());
        } finally {
            if (previous == null) {
                System.clearProperty("canvaskanban.data.dir");
            } else {
                System.setProperty("canvaskanban.data.dir", previous);
            }
        }
    }

    @Test
    void defaultDatabaseLivesOutsideTheApplicationFolder() {
        // Guards the rule "never store user data next to the program".
        Path databaseFile = AppPaths.databaseFile();
        assertTrue(databaseFile.isAbsolute(), "the database path must be absolute");
        assertTrue(databaseFile.getFileName().toString().endsWith(".db"));
        assertTrue(databaseFile.toString().contains("anvaskanban"),
                "the data folder should be named after the application: " + databaseFile);
    }

    private List<String> tableNames(Database database) throws SQLException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
        }
        return names;
    }

    private int countRows(Database database, String table) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : -1;
        }
    }
}
