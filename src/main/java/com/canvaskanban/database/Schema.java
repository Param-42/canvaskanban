package com.canvaskanban.database;

/**
 * The SQL used to create the database. Keeping it in one place makes the schema
 * easy to read without knowledge of the rest of the application.
 */
final class Schema {

    static final String CREATE_BOARDS =
            "CREATE TABLE IF NOT EXISTS boards ("
                    + "  id          INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  name        TEXT    NOT NULL,"
                    + "  description TEXT    NOT NULL DEFAULT '',"
                    + "  theme       TEXT    NOT NULL DEFAULT 'minimal-light',"
                    + "  created_at  TEXT    NOT NULL"
                    + ")";

    static final String CREATE_TASKS =
            "CREATE TABLE IF NOT EXISTS tasks ("
                    + "  id          INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  board_id    INTEGER NOT NULL,"
                    + "  title       TEXT    NOT NULL,"
                    + "  description TEXT    NOT NULL DEFAULT '',"
                    + "  priority    TEXT    NOT NULL DEFAULT 'MEDIUM',"
                    + "  status      TEXT    NOT NULL DEFAULT 'TODO',"
                    + "  position    INTEGER NOT NULL DEFAULT 0,"
                    + "  created_at  TEXT    NOT NULL,"
                    + "  due_date    TEXT,"
                    + "  FOREIGN KEY (board_id) REFERENCES boards (id) ON DELETE CASCADE"
                    + ")";

    static final String CREATE_TASKS_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_tasks_board_status "
                    + "ON tasks (board_id, status, position)";

    static final String CREATE_SETTINGS =
            "CREATE TABLE IF NOT EXISTS settings ("
                    + "  key   TEXT PRIMARY KEY,"
                    + "  value TEXT NOT NULL"
                    + ")";

    static final String[] CREATE_STATEMENTS = {
            CREATE_BOARDS,
            CREATE_TASKS,
            CREATE_TASKS_INDEX,
            CREATE_SETTINGS
    };

    private Schema() {
        // Constants only.
    }
}
