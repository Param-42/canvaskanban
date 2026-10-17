package com.canvaskanban.database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Works out where CanvasKanban stores its data.
 *
 * <p>The database is <strong>not</strong> stored next to the application, because the
 * installation folder may be read-only (for example {@code C:\Program Files}) and
 * because an application update must never overwrite the user's data.</p>
 *
 * <ul>
 *   <li>Linux: {@code $XDG_DATA_HOME/canvaskanban} or {@code ~/.local/share/canvaskanban}</li>
 *   <li>Windows: {@code %APPDATA%\CanvasKanban}</li>
 *   <li>macOS: {@code ~/Library/Application Support/CanvasKanban}</li>
 * </ul>
 *
 * <p>For tests and for advanced users the location can be overridden with the
 * system property {@code canvaskanban.data.dir} or the environment variable
 * {@code CANVASKANBAN_DATA_DIR}.</p>
 */
public final class AppPaths {

    private static final String APP_NAME = "CanvasKanban";
    private static final String APP_NAME_LOWER = "canvaskanban";
    private static final String DATABASE_FILE_NAME = "canvaskanban.db";

    private AppPaths() {
        // Utility class: no instances.
    }

    /** The folder that holds the database (created on demand). */
    public static Path dataDirectory() {
        String override = firstNonBlank(
                System.getProperty("canvaskanban.data.dir"),
                System.getenv("CANVASKANBAN_DATA_DIR"));
        if (override != null) {
            return Paths.get(override).toAbsolutePath();
        }

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String userHome = System.getProperty("user.home", ".");

        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, APP_NAME).toAbsolutePath();
            }
            return Paths.get(userHome, "AppData", "Roaming", APP_NAME).toAbsolutePath();
        }

        if (osName.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", APP_NAME).toAbsolutePath();
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Paths.get(xdgDataHome, APP_NAME_LOWER).toAbsolutePath();
        }
        return Paths.get(userHome, ".local", "share", APP_NAME_LOWER).toAbsolutePath();
    }

    /** Full path of the SQLite database file. */
    public static Path databaseFile() {
        return dataDirectory().resolve(DATABASE_FILE_NAME);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
