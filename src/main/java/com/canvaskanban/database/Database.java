package com.canvaskanban.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the connection to the SQLite database file and creates the tables on start up.
 *
 * <p>There is no connection pool: the application is a single user desktop program,
 * so a fresh short lived connection per operation is simple and safe. Every caller
 * uses try-with-resources, which guarantees that connections and statements are closed.</p>
 *
 * <p>When no database is passed in, the file lives in the user specific application
 * directory returned by {@link AppPaths#databaseFile()}.</p>
 */
public class Database {

    /** Used by tests so they can point at a temporary file. */
    private static final String USER_HOME_DB_PROPERTY = "canvaskanban.db.file";

    private final String jdbcUrl;
    private final Path databaseFile;

    /** Uses the real application database (or the path set via system property). */
    public Database() {
        this(resolveDefaultPath());
    }

    /** Uses the given database file. Parent folders are created automatically. */
    public Database(Path databaseFile) {
        this.databaseFile = databaseFile.toAbsolutePath();
        this.jdbcUrl = "jdbc:sqlite:" + this.databaseFile;
    }

    private static Path resolveDefaultPath() {
        String override = System.getProperty(USER_HOME_DB_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return AppPaths.databaseFile();
    }

    /** The JDBC url that is used to open connections. */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /** The database file, or {@code null} for an in-memory database. */
    public Path getDatabaseFile() {
        return databaseFile;
    }

    /** Opens a new connection. The caller must close it (use try-with-resources). */
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            // SQLite disables foreign keys by default, so switch them on per connection.
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /**
     * Creates the application folder (if needed) and the database tables.
     * Calling this more than once is harmless.
     *
     * @throws IllegalStateException when the database can not be prepared
     */
    public void initialize() {
        createParentDirectory();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String ddl : Schema.CREATE_STATEMENTS) {
                statement.execute(ddl);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not initialise the database at " + describeLocation() + ": " + e.getMessage(), e);
        }
    }

    /** Human readable location, used in error messages and the status bar. */
    public String describeLocation() {
        return String.valueOf(databaseFile);
    }

    private void createParentDirectory() {
        if (databaseFile == null) {
            return;
        }
        Path parent = databaseFile.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the data folder " + parent + ": " + e.getMessage(), e);
        }
    }
}
