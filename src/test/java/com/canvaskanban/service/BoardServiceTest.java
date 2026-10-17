package com.canvaskanban.service;

import com.canvaskanban.dao.SettingDao;
import com.canvaskanban.database.Database;
import com.canvaskanban.model.Board;
import com.canvaskanban.model.Priority;
import com.canvaskanban.model.Task;
import com.canvaskanban.model.TaskStatus;
import com.canvaskanban.model.Theme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the application logic that the user interface uses.
 */
class BoardServiceTest {

    @TempDir
    Path tempDir;

    private BoardService service;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDir.resolve("service.db"));
        database.initialize();
        service = new BoardService(database);
    }

    @Test
    void sampleDataIsOnlyCreatedOnce() {
        service.createSampleDataIfEmpty();
        int boardCount = service.getBoards().size();
        int taskCount = service.totalTaskCount();

        service.createSampleDataIfEmpty();

        assertEquals(1, boardCount);
        assertEquals(3, taskCount);
        assertEquals(boardCount, service.getBoards().size());
        assertEquals(taskCount, service.totalTaskCount());
    }

    @Test
    void createBoardStoresTheChosenTheme() {
        Board board = service.createBoard("Themed", "with a theme", Theme.MIDNIGHT_BLUE);

        assertEquals(Theme.MIDNIGHT_BLUE, service.findBoard(board.getId()).orElseThrow().getTheme());
    }

    @Test
    void theDefaultThemeIsRemembered() {
        assertEquals(Theme.MINIMAL_LIGHT, service.getDefaultTheme(), "Minimal Light is the starting theme");

        service.setDefaultTheme(Theme.LAVENDER);

        assertEquals(Theme.LAVENDER, service.getDefaultTheme());
    }

    @Test
    void theLastOpenedBoardIsRemembered() {
        assertTrue(service.getLastBoardId().isEmpty());

        Board board = service.createBoard("Board", "", Theme.MINIMAL_LIGHT);
        service.setLastBoardId(board.getId());

        assertEquals(board.getId(), service.getLastBoardId().orElseThrow());
    }

    @Test
    void aBrokenLastBoardIdSettingIsIgnored() {
        // A hand edited or damaged value must not break the start of the application.
        Database database = new Database(tempDir.resolve("service.db"));
        new SettingDao(database).save(BoardService.SETTING_LAST_BOARD_ID, "not-a-number");

        assertTrue(service.getLastBoardId().isEmpty());
    }

    @Test
    void createTaskUsesTheGivenValues() {
        Board board = service.createBoard("Board", "", Theme.MINIMAL_LIGHT);

        Task task = service.createTask(board.getId(), "Ship it", "before the deadline",
                Priority.HIGH, TaskStatus.IN_PROGRESS, LocalDate.of(2026, 6, 1));

        Task reloaded = service.getTasks(board.getId()).get(0);
        assertEquals(task.getId(), reloaded.getId());
        assertEquals("Ship it", reloaded.getTitle());
        assertEquals(Priority.HIGH, reloaded.getPriority());
        assertEquals(TaskStatus.IN_PROGRESS, reloaded.getStatus());
        assertEquals(LocalDate.of(2026, 6, 1), reloaded.getDueDate());
        assertEquals(1, service.countTasks(board.getId()));
    }

    @Test
    void saveTaskKeepsTheColumnWhenItDidNotChange() {
        Board board = service.createBoard("Board", "", Theme.MINIMAL_LIGHT);
        service.createTask(board.getId(), "First", "", Priority.LOW, TaskStatus.TODO, null);
        Task second = service.createTask(board.getId(), "Second", "", Priority.LOW, TaskStatus.TODO, null);

        second.setTitle("Second (edited)");
        service.saveTask(second, TaskStatus.TODO);

        List<Task> todo = service.getTasks(board.getId(), TaskStatus.TODO);
        assertEquals(List.of("First", "Second (edited)"), todo.stream().map(Task::getTitle).toList());
    }

    @Test
    void saveTaskMovesTheCardWhenTheColumnChangedInTheDialog() {
        Board board = service.createBoard("Board", "", Theme.MINIMAL_LIGHT);
        Task task = service.createTask(board.getId(), "Move me", "", Priority.LOW, TaskStatus.TODO, null);

        task.setStatus(TaskStatus.DONE);
        service.saveTask(task, TaskStatus.TODO);

        assertTrue(service.getTasks(board.getId(), TaskStatus.TODO).isEmpty());
        assertEquals(List.of("Move me"),
                service.getTasks(board.getId(), TaskStatus.DONE).stream().map(Task::getTitle).toList());
    }

    @Test
    void moveTaskChangesTheColumn() {
        Board board = service.createBoard("Board", "", Theme.MINIMAL_LIGHT);
        Task task = service.createTask(board.getId(), "Drag me", "", Priority.MEDIUM, TaskStatus.TODO, null);

        service.moveTask(task.getId(), TaskStatus.DONE, Integer.MAX_VALUE);

        assertEquals(TaskStatus.DONE, service.getTasks(board.getId(), TaskStatus.DONE).get(0).getStatus());
    }

    @Test
    void deleteBoardRemovesItsTasks() {
        Board board = service.createBoard("Board", "", Theme.MINIMAL_LIGHT);
        service.createTask(board.getId(), "Task", "", Priority.LOW, TaskStatus.TODO, null);

        service.deleteBoard(board.getId());

        assertEquals(0, service.getBoards().size());
        assertEquals(0, service.totalTaskCount());
    }

    @Test
    void deletingATaskLeavesTheOthersAlone() {
        Board board = service.createBoard("Board", "", Theme.MINIMAL_LIGHT);
        Task first = service.createTask(board.getId(), "One", "", Priority.LOW, TaskStatus.TODO, null);
        service.createTask(board.getId(), "Two", "", Priority.LOW, TaskStatus.TODO, null);

        service.deleteTask(first.getId());

        assertEquals(List.of("Two"), service.getTasks(board.getId()).stream().map(Task::getTitle).toList());
        assertFalse(service.getTasks(board.getId()).isEmpty());
    }

    @Test
    void sampleBoardUsesTheSelectedDefaultTheme() {
        service.setDefaultTheme(Theme.FOREST_GREEN);

        service.createSampleDataIfEmpty();

        assertEquals(Theme.FOREST_GREEN, service.getBoards().get(0).getTheme());
    }
}
