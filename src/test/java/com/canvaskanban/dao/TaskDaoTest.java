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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task operations: create, read, update, delete and the order inside a column.
 */
class TaskDaoTest {

    @TempDir
    Path tempDir;

    private Database database;
    private TaskDao taskDao;
    private long boardId;

    @BeforeEach
    void setUp() {
        database = new Database(tempDir.resolve("tasks.db"));
        database.initialize();
        taskDao = new TaskDao(database);
        boardId = new BoardDao(database).insert(new Board("Board", "", Theme.MINIMAL_LIGHT)).getId();
    }

    private Task newTask(String title, TaskStatus status) {
        return taskDao.insert(new Task(boardId, title, status));
    }

    @Test
    void insertStoresEveryField() {
        Task task = new Task(boardId, "Write the report", TaskStatus.IN_PROGRESS);
        task.setDescription("Chapter 1 and 2");
        task.setPriority(Priority.HIGH);
        task.setDueDate(LocalDate.of(2026, 12, 24));

        Task saved = taskDao.insert(task);

        assertTrue(saved.getId() > 0);
        Task reloaded = taskDao.findById(saved.getId()).orElseThrow();
        assertEquals("Write the report", reloaded.getTitle());
        assertEquals("Chapter 1 and 2", reloaded.getDescription());
        assertEquals(Priority.HIGH, reloaded.getPriority());
        assertEquals(TaskStatus.IN_PROGRESS, reloaded.getStatus());
        assertEquals(LocalDate.of(2026, 12, 24), reloaded.getDueDate());
        assertEquals(LocalDate.now(), reloaded.getCreatedAt());
        assertEquals(boardId, reloaded.getBoardId());
    }

    @Test
    void dueDateIsOptional() {
        Task saved = newTask("No deadline", TaskStatus.TODO);
        assertNull(taskDao.findById(saved.getId()).orElseThrow().getDueDate());
    }

    @Test
    void newTasksAreAppendedToTheirColumn() {
        Task first = newTask("First", TaskStatus.TODO);
        Task second = newTask("Second", TaskStatus.TODO);
        Task third = newTask("Third", TaskStatus.TODO);

        assertEquals(0, first.getPosition());
        assertEquals(1, second.getPosition());
        assertEquals(2, third.getPosition());
        assertEquals(List.of("First", "Second", "Third"),
                taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO).stream().map(Task::getTitle).toList());
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertTrue(taskDao.findById(999L).isEmpty());
    }

    @Test
    void findByBoardReturnsTasksOfEveryColumn() {
        newTask("A", TaskStatus.TODO);
        newTask("B", TaskStatus.IN_PROGRESS);
        newTask("C", TaskStatus.DONE);

        List<Task> tasks = taskDao.findByBoard(boardId);

        assertEquals(3, tasks.size());
        assertEquals(3, taskDao.countByBoard(boardId));
    }

    @Test
    void findByBoardOnlyReturnsTheGivenBoard() {
        long otherBoard = new BoardDao(database).insert(new Board("Other", "", Theme.DARK_MODE)).getId();
        newTask("Mine", TaskStatus.TODO);
        taskDao.insert(new Task(otherBoard, "Theirs", TaskStatus.TODO));

        assertEquals(List.of("Mine"), taskDao.findByBoard(boardId).stream().map(Task::getTitle).toList());
        assertEquals(0, taskDao.countByBoard(999L));
    }

    @Test
    void updateDetailsChangesTheContentButNotTheColumn() {
        Task task = newTask("Draft", TaskStatus.TODO);

        task.setTitle("Final");
        task.setDescription("ready");
        task.setPriority(Priority.LOW);
        task.setDueDate(LocalDate.of(2026, 5, 5));
        task.setStatus(TaskStatus.DONE); // must be ignored by updateDetails
        assertTrue(taskDao.updateDetails(task));

        Task reloaded = taskDao.findById(task.getId()).orElseThrow();
        assertEquals("Final", reloaded.getTitle());
        assertEquals("ready", reloaded.getDescription());
        assertEquals(Priority.LOW, reloaded.getPriority());
        assertEquals(LocalDate.of(2026, 5, 5), reloaded.getDueDate());
        assertEquals(TaskStatus.TODO, reloaded.getStatus(), "updateDetails must not move the card");
    }

    @Test
    void updateDetailsCanRemoveTheDueDate() {
        Task task = newTask("Has a date", TaskStatus.TODO);
        task.setDueDate(LocalDate.of(2026, 1, 1));
        taskDao.updateDetails(task);

        task.setDueDate(null);
        taskDao.updateDetails(task);

        assertNull(taskDao.findById(task.getId()).orElseThrow().getDueDate());
    }

    @Test
    void deleteRemovesOnlyThatTask() {
        Task keep = newTask("Keep", TaskStatus.TODO);
        Task remove = newTask("Remove", TaskStatus.TODO);

        assertTrue(taskDao.delete(remove.getId()));

        assertTrue(taskDao.findById(remove.getId()).isEmpty());
        assertEquals(1, taskDao.findByBoard(boardId).size());
        assertEquals("Keep", taskDao.findById(keep.getId()).orElseThrow().getTitle());
        assertFalse(taskDao.delete(remove.getId()), "deleting twice should report false");
    }

    @Test
    void nextPositionFollowsTheColumn() {
        assertEquals(0, taskDao.nextPosition(boardId, TaskStatus.TODO));
        newTask("A", TaskStatus.TODO);
        newTask("B", TaskStatus.IN_PROGRESS);

        assertEquals(1, taskDao.nextPosition(boardId, TaskStatus.TODO));
        assertEquals(1, taskDao.nextPosition(boardId, TaskStatus.IN_PROGRESS));
        assertEquals(0, taskDao.nextPosition(boardId, TaskStatus.DONE));
    }

    @Test
    void moveTaskAppendsToTheEndOfTheTargetColumn() {
        Task task = newTask("Move me", TaskStatus.TODO);
        newTask("Existing", TaskStatus.IN_PROGRESS);

        assertTrue(taskDao.moveTask(task.getId(), TaskStatus.IN_PROGRESS, Integer.MAX_VALUE));

        List<Task> inProgress = taskDao.findByBoardAndStatus(boardId, TaskStatus.IN_PROGRESS);
        assertEquals(List.of("Existing", "Move me"), inProgress.stream().map(Task::getTitle).toList());
        assertEquals(List.of(0, 1), inProgress.stream().map(Task::getPosition).toList());
        assertTrue(taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO).isEmpty());
    }

    @Test
    void moveTaskInsertsAtTheGivenIndex() {
        Task moved = newTask("Moved", TaskStatus.TODO);
        newTask("One", TaskStatus.DONE);
        newTask("Two", TaskStatus.DONE);
        newTask("Three", TaskStatus.DONE);

        assertTrue(taskDao.moveTask(moved.getId(), TaskStatus.DONE, 1));

        assertEquals(List.of("One", "Moved", "Two", "Three"),
                taskDao.findByBoardAndStatus(boardId, TaskStatus.DONE).stream().map(Task::getTitle).toList());
    }

    @Test
    void moveTaskReordersInsideTheSameColumn() {
        Task a = newTask("A", TaskStatus.TODO);
        newTask("B", TaskStatus.TODO);
        newTask("C", TaskStatus.TODO);

        // A is at index 0; drop it between B and C, which is index 1 in the list without A.
        assertTrue(taskDao.moveTask(a.getId(), TaskStatus.TODO, 1));

        assertEquals(List.of("B", "A", "C"),
                taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO).stream().map(Task::getTitle).toList());
        assertEquals(List.of(0, 1, 2),
                taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO).stream().map(Task::getPosition).toList());
    }

    @Test
    void moveTaskClampsAnIndexThatIsTooLarge() {
        Task task = newTask("Last", TaskStatus.TODO);
        assertTrue(taskDao.moveTask(task.getId(), TaskStatus.DONE, 99));
        assertEquals(0, taskDao.findById(task.getId()).orElseThrow().getPosition());
    }

    @Test
    void moveTaskReturnsFalseForAnUnknownTask() {
        assertFalse(taskDao.moveTask(777L, TaskStatus.DONE, 0));
    }

    @Test
    void moveTaskClosesTheGapInTheSourceColumn() {
        Task a = newTask("A", TaskStatus.TODO);
        Task b = newTask("B", TaskStatus.TODO);
        Task c = newTask("C", TaskStatus.TODO);

        taskDao.moveTask(b.getId(), TaskStatus.DONE, Integer.MAX_VALUE);

        List<Task> todo = taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO);
        assertEquals(List.of("A", "C"), todo.stream().map(Task::getTitle).toList());
        assertEquals(List.of(0, 1), todo.stream().map(Task::getPosition).toList());
        assertEquals(TaskStatus.DONE, taskDao.findById(b.getId()).orElseThrow().getStatus());
        assertTrue(a.getId() > 0 && c.getId() > 0);
    }

    @Test
    void tasksAreSeparatePerBoard() {
        long otherBoard = new BoardDao(database).insert(new Board("Other", "", Theme.LAVENDER)).getId();
        newTask("Mine 1", TaskStatus.TODO);
        newTask("Mine 2", TaskStatus.TODO);
        Task other = taskDao.insert(new Task(otherBoard, "Theirs", TaskStatus.TODO));

        taskDao.moveTask(other.getId(), TaskStatus.DONE, 0);

        assertEquals(TaskStatus.DONE, taskDao.findById(other.getId()).orElseThrow().getStatus());
        assertEquals(2, taskDao.findByBoardAndStatus(boardId, TaskStatus.TODO).size());
    }
}
