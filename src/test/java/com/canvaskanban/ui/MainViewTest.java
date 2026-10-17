package com.canvaskanban.ui;

import com.canvaskanban.database.Database;
import com.canvaskanban.model.Board;
import com.canvaskanban.model.Priority;
import com.canvaskanban.model.Task;
import com.canvaskanban.model.TaskStatus;
import com.canvaskanban.model.Theme;
import com.canvaskanban.service.BoardService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builds the real user interface (without opening a window) and checks that the board
 * is filled, that themes can be switched and that moving a card works.
 *
 * <p>JavaFX needs a graphics environment. When there is none - for example on a build
 * server - these tests are skipped instead of failing.</p>
 */
class MainViewTest {

    @TempDir
    static Path tempDir;

    private static boolean toolkitStarted;

    private BoardService service;
    private MainView view;
    private Scene scene;

    @BeforeAll
    static void startJavaFxToolkit() throws Exception {
        try {
            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(started::countDown);
            toolkitStarted = started.await(30, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyRunning) {
            toolkitStarted = true;
        } catch (Throwable problem) {
            toolkitStarted = false;
            System.out.println("JavaFX could not start, UI tests are skipped: " + problem);
        }
        Assumptions.assumeTrue(toolkitStarted, "no graphics environment available");
    }

    @AfterAll
    static void stopJavaFxToolkit() {
        if (toolkitStarted) {
            Platform.exit();
        }
        toolkitStarted = false;
    }

    @BeforeEach
    void setUp() throws Exception {
        Database database = new Database(tempDir.resolve("ui-" + System.nanoTime() + ".db"));
        database.initialize();
        service = new BoardService(database);

        onFxThread(() -> {
            view = new MainView(service, "test database");
            scene = new Scene(view, 1100, 700);
            view.attachScene(scene);
            return null;
        });
    }

    @Test
    void everyThemeCanBeAppliedToTheWindow() throws Exception {
        Board board = createBoardWithThreeTasks(Theme.MINIMAL_LIGHT);
        onFxThread(() -> {
            view.initialize();
            return null;
        });

        for (Theme theme : Theme.values()) {
            Paint background = onFxThread(() -> {
                view.selectTheme(theme);
                scene.getRoot().applyCss();
                assertTrue(scene.getStylesheets().contains(theme.getCssResource()),
                        "the stylesheet of " + theme + " should be active");
                return view.getBackground().getFills().get(0).getFill();
            });
            assertInstanceOf(Color.class, background, "theme " + theme + " should set a colour");
        }

        // The background really changes between a light and a dark theme.
        Color light = applyAndReadBackground(Theme.MINIMAL_LIGHT);
        Color dark = applyAndReadBackground(Theme.DARK_MODE);
        assertEquals(Color.web("#f4f5f7"), light);
        assertEquals(Color.web("#1b1d21"), dark);
        assertNotEquals(light, dark);
        assertNotEquals(0L, board.getId());
    }

    @Test
    void switchingTheThemeIsStoredForTheBoardAndAsTheDefault() throws Exception {
        Board board = createBoardWithThreeTasks(Theme.MINIMAL_LIGHT);
        onFxThread(() -> {
            view.initialize();
            view.selectTheme(Theme.WARM_SAND);
            return null;
        });

        Board reloaded = service.findBoard(board.getId()).orElseThrow();
        assertEquals(Theme.WARM_SAND, reloaded.getTheme(), "the theme belongs to the board");
        assertEquals(Theme.WARM_SAND, service.getDefaultTheme(), "and it is remembered for new boards");
    }

    @Test
    void initializeShowsTheCardsOfTheBoard() throws Exception {
        createBoardWithThreeTasks(Theme.DARK_MODE);

        onFxThread(() -> {
            view.initialize();
            return null;
        });

        List<BoardColumnView> columns = findColumns();
        assertEquals(3, columns.size(), "a board has three columns");
        assertEquals(1, cardCount(columns, TaskStatus.TODO));
        assertEquals(1, cardCount(columns, TaskStatus.IN_PROGRESS));
        assertEquals(1, cardCount(columns, TaskStatus.DONE));
        assertEquals(3, findCards().size());
    }

    @Test
    void movingACardUpdatesTheColumnsAndTheDatabase() throws Exception {
        Board board = createBoardWithThreeTasks(Theme.MINIMAL_LIGHT);
        onFxThread(() -> {
            view.initialize();
            return null;
        });

        Task toMove = service.getTasks(board.getId(), TaskStatus.TODO).get(0);

        onFxThread(() -> {
            view.moveTaskToColumn(toMove.getId(), TaskStatus.DONE, Integer.MAX_VALUE);
            return null;
        });

        List<BoardColumnView> columns = findColumns();
        assertEquals(0, cardCount(columns, TaskStatus.TODO));
        assertEquals(1, cardCount(columns, TaskStatus.IN_PROGRESS));
        assertEquals(2, cardCount(columns, TaskStatus.DONE));

        assertEquals(0, service.getTasks(board.getId(), TaskStatus.TODO).size());
        assertEquals(1, service.getTasks(board.getId(), TaskStatus.IN_PROGRESS).size());
        assertEquals(2, service.getTasks(board.getId(), TaskStatus.DONE).size());
        assertTrue(service.getTasks(board.getId(), TaskStatus.DONE).stream()
                        .anyMatch(task -> task.getId() == toMove.getId()),
                "the moved task must be stored in the Done column");
    }

    @Test
    void theLastOpenedBoardIsShownAgain() throws Exception {
        createBoardWithThreeTasks(Theme.MINIMAL_LIGHT);
        Board second = service.createBoard("Second board", "", Theme.LAVENDER);
        service.createTask(second.getId(), "Only on the second board", "", Priority.HIGH, TaskStatus.TODO, null);
        service.setLastBoardId(second.getId());

        onFxThread(() -> {
            view.initialize();
            return null;
        });

        List<TaskCardView> cards = findCards();
        assertEquals(1, cards.size(), "only the tasks of the remembered board should be shown");
        assertEquals("Only on the second board", cards.get(0).getTask().getTitle());
    }

    @Test
    void anEmptyDatabaseShowsAnEmptyBoard() throws Exception {
        onFxThread(() -> {
            view.initialize();
            return null;
        });

        assertEquals(0, findCards().size());
        assertEquals(0, cardCount(findColumns(), TaskStatus.TODO));
    }

    @Test
    void columnsAndCardsAreSetUpForDragAndDrop() throws Exception {
        createBoardWithThreeTasks(Theme.MINIMAL_LIGHT);
        onFxThread(() -> {
            view.initialize();
            return null;
        });

        for (BoardColumnView column : findColumns()) {
            assertNotNull(column.getOnDragOver(), "the column must accept dragged cards");
            assertNotNull(column.getOnDragDropped(), "the column must handle a drop");
        }
        List<TaskCardView> cards = findCards();
        assertTrue(!cards.isEmpty(), "the board should show its cards");
        assertNotNull(cards.get(0).getOnDragDetected(), "a card must start a drag");
    }

    @Test
    void theSameCardIsNotShownTwiceAfterMovingIt() throws Exception {
        Board board = createBoardWithThreeTasks(Theme.MINIMAL_LIGHT);
        onFxThread(() -> {
            view.initialize();
            return null;
        });
        Task toMove = service.getTasks(board.getId(), TaskStatus.IN_PROGRESS).get(0);

        onFxThread(() -> {
            view.moveTaskToColumn(toMove.getId(), TaskStatus.DONE, 0);
            return null;
        });

        List<Long> ids = findCards().stream().map(card -> card.getTask().getId()).toList();
        assertEquals(ids.size(), ids.stream().distinct().count(), "ids should be unique: " + ids);
        assertEquals(3, ids.size());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Creates a board with one task in each column and returns it. */
    private Board createBoardWithThreeTasks(Theme theme) {
        Board board = service.createBoard("UI test board", "created by MainViewTest", theme);
        service.createTask(board.getId(), "To do task", "not started", Priority.LOW, TaskStatus.TODO, null);
        service.createTask(board.getId(), "Running task", "in progress", Priority.MEDIUM,
                TaskStatus.IN_PROGRESS, null);
        service.createTask(board.getId(), "Finished task", "done", Priority.HIGH, TaskStatus.DONE, null);
        return board;
    }

    private Color applyAndReadBackground(Theme theme) throws Exception {
        return onFxThread(() -> {
            view.selectTheme(theme);
            scene.getRoot().applyCss();
            return (Color) view.getBackground().getFills().get(0).getFill();
        });
    }

    private List<BoardColumnView> findColumns() throws Exception {
        List<Node> nodes = onFxThread(() -> collect(scene.getRoot()));
        return nodes.stream()
                .filter(BoardColumnView.class::isInstance)
                .map(BoardColumnView.class::cast)
                .toList();
    }

    private List<TaskCardView> findCards() throws Exception {
        List<Node> nodes = onFxThread(() -> collect(scene.getRoot()));
        return nodes.stream()
                .filter(TaskCardView.class::isInstance)
                .map(TaskCardView.class::cast)
                .toList();
    }

    private int cardCount(List<BoardColumnView> columns, TaskStatus status) {
        return columns.stream()
                .filter(column -> column.getStatus() == status)
                .mapToInt(BoardColumnView::getCardCount)
                .sum();
    }

    /** Walks the scene graph by hand; no CSS selector lookup needed. */
    private static List<Node> collect(Node node) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(node);
        // A ScrollPane only shows its content through its skin, so follow the content
        // reference directly. This works even before the first layout pass.
        if (node instanceof javafx.scene.control.ScrollPane scrollPane && scrollPane.getContent() != null) {
            nodes.addAll(collect(scrollPane.getContent()));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                nodes.addAll(collect(child));
            }
        }
        return nodes;
    }

    /** Runs the action on the JavaFX thread and waits for the result. */
    private static <T> T onFxThread(java.util.concurrent.Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        try {
            return task.get(30, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            // Unwrap, so a failing assertion inside the FX block is easy to read.
            if (e.getCause() instanceof AssertionError assertionError) {
                throw assertionError;
            }
            throw e;
        }
    }
}
