# Using CanvasKanban

## Every action on the board

| What you want | What to do |
| --- | --- |
| Create a board | Click **New board** in the toolbar and type a name. |
| Rename a board | Pick it in the dropdown, then click **Edit board**. |
| Delete a board | Pick it in the dropdown, then click **Delete board**. |
| Add a task | Click **New task**, or the small **+** in the header of a column (the task is created in that column). |
| Edit a task | Double click its card. |
| Delete a task | Right click its card and choose **Delete task**. |
| Read a long description | Right click the card and choose **Show description**. |
| Change a task's column | Drag the card into another column. |
| Reorder tasks | Drag a card up or down inside its column; a blue line shows where it will land. |
| Change the theme | Choose one in the **Theme** box; it is stored for that board and used again after a restart. |

The little message in the bottom left corner always tells you what just happened (for
example "Task deleted"), and the bottom right corner shows where the database lives and
how many tasks the board has.

## Where your data is stored

The database is **not** stored next to the application, because the installation folder
may be read-only and an update must never overwrite your tasks.

| Operating system | Location |
| --- | --- |
| Linux | `$XDG_DATA_HOME/canvaskanban/canvaskanban.db`, usually `~/.local/share/canvaskanban/canvaskanban.db` |
| Windows | `%APPDATA%\CanvasKanban\canvaskanban.db` |
| macOS | `~/Library/Application Support/CanvasKanban/canvaskanban.db` |

The application shows the exact path in the bottom right corner, so you never have to
guess.

**Look inside it** - `canvaskanban.db` is a normal SQLite file, so the `sqlite3` command
line or any database client can open it, including over JDBC. The connection settings, the
queries worth running and the one rule about writing while the app is running are in
[docs/DATABASE.md](DATABASE.md).

**Backup** - close the app and copy that folder somewhere safe. That is the whole
database.

**Move the data somewhere else** - start the app with the system property
`canvaskanban.data.dir` (or the environment variable `CANVASKANBAN_DATA_DIR`):

```bash
java -Dcanvaskanban.data.dir=/tmp/my-board -cp "target/libs/*" com.canvaskanban.Launcher
```

**Start over** - close the app and delete `canvaskanban.db`. The next start creates a
fresh database with the example board again.

## Troubleshooting

**Where did my tasks go?**
They are in `canvaskanban.db` in the folder listed in the bottom right corner of the
window (see [Where your data is stored](#where-your-data-is-stored)). If the file is
missing, the app creates a new one - the example board is back, which is a good sign that
a fresh database was created.

**A "database is locked" error**
Two programs cannot write at the same time - that is true for two copies of the application
and also for a database client you have opened on the file. Close the other one and try
again; reads are always safe, and
[docs/DATABASE.md](DATABASE.md) shows how to work on a copy instead.

## Known limitations

- Boards, tasks and settings are local to your machine. There is no sharing, no sync and
  no export to CSV or JSON yet.
- The three columns are fixed (To Do / In Progress / Done). You cannot add a fourth.
- Tasks cannot be dragged between two different boards.
- There is no undo. Deleting a board asks for confirmation, deleting a task asks too, but
  after that it is gone.
- Search, filtering, attachments, sub-tasks, labels and reminders are not implemented.
- The window layout is aimed at a desktop screen; it is not a phone-sized interface.
- The database has no automatic older/newer-version migration yet: it creates the tables
  it needs when they are missing, which is fine for this version.
