package com.canvaskanban.dao;

import com.canvaskanban.database.Database;
import com.canvaskanban.model.Board;
import com.canvaskanban.model.Task;
import com.canvaskanban.model.TaskStatus;
import com.canvaskanban.model.Theme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Board operations against a temporary database file.
 */
class BoardDaoTest {

    @TempDir
    Path tempDir;

    private Database database;
    private BoardDao boardDao;
    private TaskDao taskDao;

    @BeforeEach
    void setUp() {
        database = new Database(tempDir.resolve("boards.db"));
        database.initialize();
        boardDao = new BoardDao(database);
        taskDao = new TaskDao(database);
    }

    @Test
    void insertStoresTheBoardAndAssignsAnId() {
        Board board = new Board("College project", "Semester 5 work", Theme.FOREST_GREEN);

        Board saved = boardDao.insert(board);

        assertTrue(saved.getId() > 0, "the database should assign an id");
        assertEquals("College project", saved.getName());
        assertFalse(saved.getCreatedAt() == null || saved.getCreatedAt().isBlank(),
                "a creation date should be filled in");
    }

    @Test
    void findByIdReturnsAllFields() {
        Board saved = boardDao.insert(new Board("Work", "Office stuff", Theme.MIDNIGHT_BLUE));

        Optional<Board> found = boardDao.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Work", found.get().getName());
        assertEquals("Office stuff", found.get().getDescription());
        assertEquals(Theme.MIDNIGHT_BLUE, found.get().getTheme());
        assertEquals(saved.getCreatedAt(), found.get().getCreatedAt());
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertTrue(boardDao.findById(4242L).isEmpty());
    }

    @Test
    void findAllReturnsEveryBoardInCreationOrder() {
        boardDao.insert(new Board("First", "", Theme.MINIMAL_LIGHT));
        boardDao.insert(new Board("Second", "", Theme.DARK_MODE));
        boardDao.insert(new Board("Third", "", Theme.LAVENDER));

        List<Board> boards = boardDao.findAll();

        assertEquals(3, boards.size());
        assertEquals(List.of("First", "Second", "Third"),
                boards.stream().map(Board::getName).toList());
        assertEquals(3, boardDao.count());
    }

    @Test
    void updateChangesNameDescriptionAndTheme() {
        Board board = boardDao.insert(new Board("Old name", "old", Theme.MINIMAL_LIGHT));

        board.setName("New name");
        board.setDescription("new description");
        board.setTheme(Theme.WARM_SAND);
        assertTrue(boardDao.update(board));

        Board reloaded = boardDao.findById(board.getId()).orElseThrow();
        assertEquals("New name", reloaded.getName());
        assertEquals("new description", reloaded.getDescription());
        assertEquals(Theme.WARM_SAND, reloaded.getTheme());
    }

    @Test
    void updateOfAnUnknownBoardReturnsFalse() {
        assertFalse(boardDao.update(new Board(9999L, "Ghost", "", Theme.DARK_MODE, "2026-01-01")));
    }

    @Test
    void deleteRemovesTheBoardAndItsTasks() {
        Board board = boardDao.insert(new Board("Temporary", "", Theme.MINIMAL_LIGHT));
        taskDao.insert(new Task(board.getId(), "Task A", TaskStatus.TODO));
        taskDao.insert(new Task(board.getId(), "Task B", TaskStatus.DONE));
        assertEquals(2, taskDao.countByBoard(board.getId()));

        assertTrue(boardDao.delete(board.getId()));

        assertTrue(boardDao.findById(board.getId()).isEmpty());
        assertEquals(0, taskDao.countByBoard(board.getId()), "the tasks must be deleted as well");
    }

    @Test
    void deleteOfAnUnknownBoardReturnsFalse() {
        assertFalse(boardDao.delete(1234L));
    }

    @Test
    void deletingOneBoardKeepsTheTasksOfTheOther() {
        Board keep = boardDao.insert(new Board("Keep", "", Theme.MINIMAL_LIGHT));
        Board remove = boardDao.insert(new Board("Remove", "", Theme.DARK_MODE));
        taskDao.insert(new Task(keep.getId(), "Still here", TaskStatus.TODO));
        taskDao.insert(new Task(remove.getId(), "Goes away", TaskStatus.TODO));

        boardDao.delete(remove.getId());

        assertEquals(1, taskDao.countByBoard(keep.getId()));
        assertEquals("Still here", taskDao.findByBoard(keep.getId()).get(0).getTitle());
    }
}
