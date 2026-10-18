# Looking inside the database (SQL clients and JDBC)

CanvasKanban keeps everything - boards, tasks and settings - in one SQLite file. That file
*is* the database: there is no server to start and nothing to configure. The application
talks to it through the **SQLite JDBC driver**, so any SQL client that can load the same
driver can open the same file with the same URL.

This page shows where the file is, how to connect a client to it, and what to watch out for
while the application is running.

## Where the file is

| Operating system | Location |
| --- | --- |
| Linux | `$XDG_DATA_HOME/canvaskanban/canvaskanban.db`, usually `~/.local/share/canvaskanban/canvaskanban.db` |
| Windows | `%APPDATA%\CanvasKanban\canvaskanban.db` |
| macOS | `~/Library/Application Support/CanvasKanban/canvaskanban.db` |

The bottom right corner of the window shows the exact path for your machine, which is the
quickest way to copy it. If you started the app with `-Dcanvaskanban.data.dir=...` or
`CANVASKANBAN_DATA_DIR`, the file is in that folder instead (see
[Where your data is stored](USING.md#where-your-data-is-stored)).

## What is inside

| Table | Columns | Notes |
| --- | --- | --- |
| `boards` | `id`, `name`, `description`, `theme`, `created_at` | one row per board; `theme` holds the id of the board's theme |
| `tasks` | `id`, `board_id`, `title`, `description`, `priority`, `status`, `position`, `created_at`, `due_date` | `board_id` points at `boards.id`; deleting a board deletes its tasks (`ON DELETE CASCADE`) |
| `settings` | `key`, `value` | the application's own preferences, for example `last_board_id` and `default_theme` |

There is one index, `idx_tasks_board_status` on `tasks (board_id, status, position)`, which
is what makes loading a single column fast.

`status` is one of `TODO`, `IN_PROGRESS` or `DONE`, and `priority` is `LOW`, `MEDIUM` or
`HIGH`. Dates are stored as ISO text (`2026-10-18`), which sorts correctly and stays
readable in any client.

## Connecting a SQL client over JDBC

Every client needs the same five values:

| Setting | Value |
| --- | --- |
| Driver class | `org.sqlite.JDBC` |
| JDBC URL | `jdbc:sqlite:/home/you/.local/share/canvaskanban/canvaskanban.db` |
| User name | *(leave empty)* |
| Password | *(leave empty)* |
| Driver JAR | `sqlite-jdbc-3.53.4.0.jar` |

Use the **absolute** path and forward slashes, even on Windows:

```
jdbc:sqlite:C:/Users/you/AppData/Roaming/CanvasKanban/canvaskanban.db
```

Where to get the driver JAR:

| Source | Path |
| --- | --- |
| After `mvn clean package` (the build already copies every runtime dependency here) | `target/libs/sqlite-jdbc-3.53.4.0.jar` |
| Maven's local cache, after any build | `~/.m2/repository/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0.jar` |
| Maven Central | <https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0.jar> |

The driver reports itself as `SQLite JDBC 3.53.4.0` and the database as `SQLite 3.53.4`,
which is the version declared in `pom.xml`.

### DBeaver

Tested with DBeaver 26.2.1, installed as a normal package (not a Flatpak), so it can read
the data folder without any extra permission.

1. **Database > New Database Connection > SQLite > Next**
2. **Path**: pick `canvaskanban.db`. DBeaver downloads or already has its own SQLite driver.
3. **Test Connection > Finish**. Leave user name and password empty.
4. Open the connection, then `main > Tables > tasks`, and right click > **View Data**.

To browse without any risk while the application is running, use the read-only URL from the
table above (`?mode=ro`). SQLite itself enforces it, so it behaves the same in every client
and every DBeaver version. Recent DBeaver versions also offer a read-only switch of their own
in the connection settings, but that one is DBeaver's promise, not the driver's.

If you would rather use the driver from this project than DBeaver's own: create the
connection with **SQLite (JDBC)**, set the driver class to `org.sqlite.JDBC`, add the JAR
under **Driver files**, and paste the URL from the table above.

### SQL Workbench/J

SQL Workbench/J is a generic JDBC client and does **not** ship a SQLite driver, so the JAR
from the table above is required.

1. **File > Manage Drivers > New**: name `SQLite`, class `org.sqlite.JDBC`, add the JAR
   under **Library**.
2. **File > Connect window > New profile**: driver `SQLite`, paste the JDBC URL, leave the
   user name and password empty.
3. SQL Workbench/J wraps your statements in a transaction. **Commit or roll back when you
   are done reading**, otherwise the open transaction keeps the file locked for the running
   application (see the next section). Its SQL editor is also happier with plain SQL than
   with SQLite's `PRAGMA` statements; the other clients handle those better.

### IntelliJ IDEA and DataGrip

* **IntelliJ IDEA Ultimate** and **DataGrip**: **View > Tool Windows > Database**, then
  **+ > Data Source > SQLite > File** and pick the database. The driver is built in.
* **IntelliJ IDEA Community** - the edition used for this project - has **no** Database tool
  window. Use DBeaver, SQL Workbench/J or the `sqlite3` command line instead.

### DB Browser for SQLite, BeeKeeper and friends

These open the file directly and need no JDBC configuration at all; point them at the same
`canvaskanban.db`.

## While the application is running

SQLite allows many readers but only one writer. The application opens a connection per
operation and closes it again, so it never holds the file for long - but it does not wait
for anyone else either. Measured on this machine:

| Situation | Result |
| --- | --- |
| A client reads while the app runs | works |
| A client opens the file with `mode=ro` and tries to write | refused: `[SQLITE_READONLY] Attempt to write a readonly database` |
| A client writes while the app is saving | the second writer waits and then fails: `[SQLITE_BUSY] The database file is locked (database is locked)` |

So pick whichever of these fits what you are doing:

1. **Reading** (the usual case): connect read-only, where a mistake cannot corrupt anything.
   Add `?mode=ro` to the URL and wrap the path in `file:`:

   ```
   jdbc:sqlite:file:/home/you/.local/share/canvaskanban/canvaskanban.db?mode=ro
   ```

2. **Changing data by hand**: close the application first.

3. **Leaving the file completely alone**: work on a snapshot. `VACUUM INTO` writes a
   consistent copy, even while the application is running, and the real file is untouched:

   ```bash
   sqlite3 ~/.local/share/canvaskanban/canvaskanban.db "VACUUM INTO '/tmp/board-copy.db'"
   ```

   Open `/tmp/board-copy.db` in the client and delete it when you are done. A plain `cp` of
   the file can catch it in the middle of a write; `VACUUM INTO` cannot.

## Queries worth showing

The example board that a fresh installation creates, seen from a client:

```sql
-- every task with the name of its board
SELECT b.name AS board, t.status, t.title, t.priority, t.due_date
FROM boards b JOIN tasks t ON t.board_id = b.id
ORDER BY t.id;
```

```
board          | status      | title                | priority | due_date
My First Board | DONE        | Read the README      | LOW      |
My First Board | IN_PROGRESS | Add your first task  | MEDIUM   | 2026-10-21
My First Board | TODO        | Try the themes       | HIGH     |
```

```sql
-- how many tasks are in each column
SELECT status, count(*) FROM tasks GROUP BY status ORDER BY status;

-- what is late (the same rule the card colour uses)
SELECT title, due_date FROM tasks
WHERE due_date IS NOT NULL AND status <> 'DONE' AND due_date < date('now');

-- boards and their task counts
SELECT b.name, count(t.id) AS tasks FROM boards b
LEFT JOIN tasks t ON t.board_id = b.id GROUP BY b.id ORDER BY b.name;
```

The index is really used - worth showing if someone asks whether the database is designed
or just written:

```
sqlite> EXPLAIN QUERY PLAN
   ...> SELECT * FROM tasks WHERE board_id = 1 AND status = 'TODO' ORDER BY position;
QUERY PLAN
`--SEARCH tasks USING INDEX idx_tasks_board_status (board_id=? AND status=?)
```

## The command line, for a quick look

Nothing has to be installed for this on Linux and macOS; `sqlite3` reads the same file:

```bash
sqlite3 ~/.local/share/canvaskanban/canvaskanban.db
```

```sql
.headers on
.mode column
.tables
SELECT b.name, t.status, t.title FROM boards b JOIN tasks t ON t.board_id = b.id;
.quit
```

Or as a single command, without opening the shell:

```bash
sqlite3 ~/.local/share/canvaskanban/canvaskanban.db \
  "SELECT b.name, t.status, t.title FROM boards b JOIN tasks t ON t.board_id = b.id;"
```

## A sixty second demonstration

1. Start the application and add a task in **In Progress**, then close it.
2. Open the database in DBeaver (or run the join query with `sqlite3`).
3. The task you just created is a row in `tasks`, with `status = 'IN_PROGRESS'`.
4. Say: *"The window is only a view of this file. The application reaches the database
   through JDBC with the SQLite driver - the same driver this client is using - and the
   database itself is a single file, so there is no server to install or start."*
