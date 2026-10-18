# CanvasKanban architecture

## Project structure

```
.
├── pom.xml                                  Maven build: Java 21, JavaFX, SQLite, JUnit
├── package.sh                               builds a Linux package with jpackage
├── package.bat                              builds a Windows package with jpackage
├── install.sh                               one-command installer for Linux
├── install.ps1                              one-command installer for Windows
├── .github/workflows/build.yml              continuous integration
├── docs/screenshots/                        the screenshots used in the README
└── src
    ├── main
    │   ├── java/com/canvaskanban
    │   │   ├── MainApp.java                 the JavaFX application: init, start, stop
    │   │   ├── Launcher.java                the entry point used by the packaged app
    │   │   ├── database
    │   │   │   ├── AppPaths.java            where the database lives on each OS
    │   │   │   ├── Database.java             opens/closes connections, one per call
    │   │   │   └── Schema.java              creates the tables and indexes
    │   │   ├── model
    │   │   │   ├── Board.java
    │   │   │   ├── Task.java
    │   │   │   ├── Priority.java            LOW, MEDIUM, HIGH
    │   │   │   ├── TaskStatus.java          TODO, IN_PROGRESS, DONE (the three columns)
    │   │   │   └── Theme.java               the six built-in themes
    │   │   ├── dao                            plain SQL, no framework
    │   │   │   ├── BoardDao.java
    │   │   │   ├── TaskDao.java             includes moving and reordering tasks
    │   │   │   ├── SettingDao.java          key/value settings (last board, default theme)
    │   │   │   ├── SqlDates.java            date <-> ISO text
    │   │   │   └── DataAccessException.java
    │   │   ├── service
    │   │   │   └── BoardService.java        what the user interface is allowed to do
    │   │   └── ui
    │   │       ├── MainView.java            toolbar, board, status bar
    │   │       ├── BoardColumnView.java     one column, drag and drop targets
    │   │       ├── TaskCardView.java        one card
    │   │       ├── Dialogs.java             the task/board dialogs and alerts
    │   │       ├── ThemeManager.java        which CSS files belong to a theme
    │   │       └── ThemeListCell.java       the coloured dots in the theme selector
    │   └── resources/css
    │       ├── base.css                     all layout, uses -app-* variables
    │       ├── minimal-light.css            one small file per theme, colours only
    │       ├── dark-mode.css
    │       ├── midnight-blue.css
    │       ├── forest-green.css
    │       ├── lavender.css
    │       └── warm-sand.css
    └── test/java/com/canvaskanban
        ├── database/DatabaseTest.java
        ├── dao/BoardDaoTest.java
        ├── dao/TaskDaoTest.java
        ├── dao/TaskStatusUpdateTest.java
        ├── service/BoardServiceTest.java
        ├── ui/ThemeCssTest.java
        └── ui/MainViewTest.java
```

## How the code fits together

```
MainApp  ->  MainView  ->  BoardService  ->  BoardDao / TaskDao / SettingDao  ->  Database  ->  SQLite file
 (window)     (layout)      (rules)          (SQL)                               (JDBC)
```

- **`Database`** opens a new JDBC connection for every call and closes it again. SQLite
  is happy with that, it keeps the code simple, and it means no connection is ever left
  open behind a dialog.
- **The DAOs** contain plain SQL. Nothing is hidden behind an ORM.
- **`BoardService`** is the only thing the user interface talks to. It knows the rules:
  a board always gets the three columns, deleting a board deletes its tasks, moving a
  task rewrites the positions of both columns in one transaction.
- **`MainView`** builds the screen by hand (no FXML), wires the buttons and rebuilds the
  board whenever the service says something changed.
- **`Launcher`** is a plain class whose only job is to call `MainApp.main`. Java refuses
  to start a class that extends `javafx.application.Application` from the classpath with
  *"JavaFX runtime components are missing"*, so the packaged application, the JAR and the
  tests all go through `Launcher` or `MainApp.main` instead.
