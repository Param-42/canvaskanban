package com.canvaskanban.dao;

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
 * Focused tests for changing the status (the column) of a task, and for the fact
 * that everything is still there after the application is started again.
 */
class TaskStatusUpdateTest {

    @TempDir
    Path tempDir;

    private Path databaseFile;
    private Database database;
    private TaskDao taskDao;
    private long boardId;

    @BeforeEach
    void setUp() {
        databaseFile = tempDir.resolve("status.db");
        database = new Database(databaseFile);
        database.initialize();
        taskDao = new TaskDao(database);
        boardId = new BoardDao(database).insert(new Board("Status board", "", Theme.MINIMAL_LIGHT)).getId();
    }

    @Test
    void updateStatusMovesTheTaskToTheEndOfTheNewColumn() {
        Task first = taskDao.insert(new Task(boardId, "First", TaskStatus.TODO));
        Task task = taskDao.insert(new Task(boardId, "Second", TaskStatus.TODO));
        taskDao.insert(new Task(boardId, "Already there", TaskStatus.DONE));

        assertTrue(taskDao.updateStatus(task.getId(), TaskStatus.DONE));

        Task reloaded = taskDao.findById(task.getId()).orElseThrow();
        assertEquals(TaskStatus.DONE, reloaded.getStatus());
        assertEquals(1, reloaded.getPosition(), "it should be placed after the task already in Done");
        assertEquals(List.of("Already there", "Second"),
                taskDao.findByBoardAndStatus(boardId, TaskStatus.DONE).stream().map(Task::getTitle).toList());
        assertEquals(List.of("First"),
                taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO).stream().map(Task::getTitle).toList());
    }

    @Test
    void updateStatusCanGoBackToToDo() {
        Task task = taskDao.insert(new Task(boardId, "Reopen me", TaskStatus.DONE));

        taskDao.updateStatus(task.getId(), TaskStatus.TODO);

        assertEquals(TaskStatus.TODO, taskDao.findById(task.getId()).orElseThrow().getStatus());
        assertEquals(1, taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO).size());
        assertEquals(0, taskDao.findByBoardAndStatus(boardId, TaskStatus.DONE).size());
    }

    @Test
    void updateStatusForAnUnknownTaskReturnsFalse() {
        assertFalse(taskDao.updateStatus(555L, TaskStatus.DONE));
    }

    @Test
    void everyStatusCanBeReached() {
        Task task = taskDao.insert(new Task(boardId, "Traveller", TaskStatus.TODO));

        for (TaskStatus status : TaskStatus.values()) {
            assertTrue(taskDao.updateStatus(task.getId(), status), "moving to " + status + " failed");
            assertEquals(status, taskDao.findById(task.getId()).orElseThrow().getStatus());
        }
    }

    @Test
    void doneTasksKeepTheirDueDateAndPriorityWhenMoved() {
        Task task = new Task(boardId, "Important", TaskStatus.TODO);
        task.setPriority(Priority.HIGH);
        task.setDueDate(LocalDate.of(2026, 3, 3));
        taskDao.insert(task);

        taskDao.updateStatus(task.getId(), TaskStatus.DONE);

        Task reloaded = taskDao.findById(task.getId()).orElseThrow();
        assertEquals(Priority.HIGH, reloaded.getPriority());
        assertEquals(LocalDate.of(2026, 3, 3), reloaded.getDueDate());
    }

    @Test
    void boardAndTasksSurviveARestartOfTheApplication() {
        Task task = taskDao.insert(new Task(boardId, "Survives", TaskStatus.TODO));
        taskDao.updateStatus(task.getId(), TaskStatus.IN_PROGRESS);

        // A new Database object on the same file is what a restart looks like.
        Database reopened = new Database(databaseFile);
        reopened.initialize();
        TaskDao reopenedTaskDao = new TaskDao(reopened);

        Task reloaded = reopenedTaskDao.findById(task.getId()).orElseThrow();
        assertEquals("Survives", reloaded.getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, reloaded.getStatus());
        assertEquals(1, reopenedTaskDao.countByBoard(boardId));
        assertEquals("Status board", new BoardDao(reopened).findById(boardId).orElseThrow().getName());
    }

    @Test
    void movingManyTasksKeepsTheOrderStable() {
        for (int i = 1; i <= 5; i++) {
            taskDao.insert(new Task(boardId, "Task " + i, TaskStatus.TODO));
        }
        List<Task> todo = taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO);

        // Move every task to Done in reverse order.
        for (int i = todo.size() - 1; i >= 0; i--) {
            taskDao.updateStatus(todo.get(i).getId(), TaskStatus.DONE);
        }

        assertEquals(List.of("Task 5", "Task 4", "Task 3", "Task 2", "Task 1"),
                taskDao.findByBoardAndStatus(boardId, TaskStatus.DONE).stream().map(Task::getTitle).toList());
        assertEquals(List.of(0, 1, 2, 3, 4),
                taskDao.findByBoardAndStatus(boardId, TaskStatus.DONE).stream().map(Task::getPosition).toList());
        assertTrue(taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO).isEmpty());
    }
}
