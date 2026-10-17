package com.canvaskanban.service;

import com.canvaskanban.dao.BoardDao;
import com.canvaskanban.dao.SettingDao;
import com.canvaskanban.dao.TaskDao;
import com.canvaskanban.database.Database;
import com.canvaskanban.model.Board;
import com.canvaskanban.model.Priority;
import com.canvaskanban.model.Task;
import com.canvaskanban.model.TaskStatus;
import com.canvaskanban.model.Theme;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The single place where the user interface asks for data.
 *
 * <p>The JavaFX classes never write SQL themselves; they call this service, which
 * uses the DAOs. That keeps the screens free of database details and makes the
 * logic testable.</p>
 */
public class BoardService {

    public static final String SETTING_LAST_BOARD_ID = "last_board_id";
    public static final String SETTING_DEFAULT_THEME = "default_theme";

    private final BoardDao boardDao;
    private final TaskDao taskDao;
    private final SettingDao settingDao;

    public BoardService(Database database) {
        this.boardDao = new BoardDao(database);
        this.taskDao = new TaskDao(database);
        this.settingDao = new SettingDao(database);
    }

    // ------------------------------------------------------------------
    // Boards
    // ------------------------------------------------------------------

    public List<Board> getBoards() {
        return boardDao.findAll();
    }

    /** Creates a board and stores it in the database. */
    public Board createBoard(String name, String description, Theme theme) {
        Board board = new Board(name, description, theme);
        return boardDao.insert(board);
    }

    /** Stores name, description and theme of an existing board. */
    public void saveBoard(Board board) {
        boardDao.update(board);
    }

    /** Deletes a board together with all of its tasks. */
    public void deleteBoard(long boardId) {
        boardDao.delete(boardId);
    }

    public Optional<Board> findBoard(long boardId) {
        return boardDao.findById(boardId);
    }

    // ------------------------------------------------------------------
    // Tasks
    // ------------------------------------------------------------------

    public List<Task> getTasks(long boardId) {
        return taskDao.findByBoard(boardId);
    }

    public List<Task> getTasks(long boardId, TaskStatus status) {
        return taskDao.findByBoardAndStatus(boardId, status);
    }

    public Task createTask(long boardId, String title, String description,
                           Priority priority, TaskStatus status, LocalDate dueDate) {
        Task task = new Task(0L, boardId, title, description, priority, status,
                LocalDate.now(), dueDate, 0);
        return taskDao.insert(task);
    }

    /**
     * Saves the content of a task. When the column changed in the edit dialog the
     * card is moved to the end of its new column.
     *
     * @param task           the task with its new values
     * @param previousStatus the column the task had before the edit
     */
    public void saveTask(Task task, TaskStatus previousStatus) {
        taskDao.updateDetails(task);
        if (previousStatus != task.getStatus()) {
            taskDao.moveTask(task.getId(), task.getStatus(), Integer.MAX_VALUE);
        }
    }

    public void deleteTask(long taskId) {
        taskDao.delete(taskId);
    }

    /**
     * Moves a task, for example when the user drags a card to another column.
     *
     * @param targetIndex place inside the target column, {@code Integer.MAX_VALUE} for the end
     */
    public void moveTask(long taskId, TaskStatus targetStatus, int targetIndex) {
        taskDao.moveTask(taskId, targetStatus, targetIndex);
    }

    public int countTasks(long boardId) {
        return taskDao.countByBoard(boardId);
    }

    public int totalTaskCount() {
        return boardDao.findAll().stream().mapToInt(board -> taskDao.countByBoard(board.getId())).sum();
    }

    // ------------------------------------------------------------------
    // Settings (kept small on purpose, just two keys)
    // ------------------------------------------------------------------

    /** The theme new boards start with. The last theme the user picked. */
    public Theme getDefaultTheme() {
        return Theme.fromId(settingDao.findOrDefault(SETTING_DEFAULT_THEME, Theme.MINIMAL_LIGHT.getId()));
    }

    public void setDefaultTheme(Theme theme) {
        settingDao.save(SETTING_DEFAULT_THEME, theme.getId());
    }

    /** The board that should be opened when the application starts. */
    public Optional<Long> getLastBoardId() {
        String value = settingDao.find(SETTING_LAST_BOARD_ID).orElse(null);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public void setLastBoardId(long boardId) {
        settingDao.save(SETTING_LAST_BOARD_ID, String.valueOf(boardId));
    }

    /**
     * Creates a small example board the first time the application runs, so a new
     * user immediately sees how CanvasKanban works. Does nothing when boards exist.
     */
    public void createSampleDataIfEmpty() {
        if (!getBoards().isEmpty()) {
            return;
        }
        Board sample = createBoard("My First Board",
                "Example board created for you. Change it however you like.",
                getDefaultTheme());
        createTask(sample.getId(), "Read the README",
                "Java 21 and Maven are enough - no server and no internet connection needed.",
                Priority.LOW, TaskStatus.DONE, null);
        createTask(sample.getId(), "Add your first task",
                "Use the \"New task\" button, or drag a card between the columns.",
                Priority.MEDIUM, TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(3));
        createTask(sample.getId(), "Try the themes",
                "Pick a theme in the toolbar - it is stored per board and remembered after a restart.",
                Priority.HIGH, TaskStatus.TODO, null);
    }
}
