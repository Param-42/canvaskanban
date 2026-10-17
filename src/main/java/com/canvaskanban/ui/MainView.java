package com.canvaskanban.ui;

import com.canvaskanban.dao.DataAccessException;
import com.canvaskanban.model.Board;
import com.canvaskanban.model.Task;
import com.canvaskanban.model.TaskStatus;
import com.canvaskanban.model.Theme;
import com.canvaskanban.service.BoardService;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * The main window: toolbar, board header, the three columns and a status bar.
 *
 * <p>The class only talks to {@link BoardService}. That is why there is no SQL in the
 * user interface and why the board logic can be unit tested without a window.</p>
 */
public class MainView extends BorderPane {

    private final BoardService service;
    private final String databaseLocation;
    private final ThemeManager themeManager = new ThemeManager();

    private final ComboBox<Board> boardSelector = new ComboBox<>();
    private final ComboBox<Theme> themeSelector = new ComboBox<>();
    private final HBox columnsBox = new HBox(14);
    private final Label boardNameLabel = new Label();
    private final Label boardDescriptionLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label taskCountLabel = new Label();
    private final Button newTaskButton = new Button("New task");
    private final Button editBoardButton = new Button("Edit board");
    private final Button deleteBoardButton = new Button("Delete board");
    private final List<BoardColumnView> columnViews = new ArrayList<>();

    private Board currentBoard;
    /** Stops the combo boxes from firing their listeners while we update them. */
    private boolean updatingSelectors;

    public MainView(BoardService service, String databaseLocation) {
        this.service = service;
        this.databaseLocation = databaseLocation;

        getStyleClass().add("app-root");
        setTop(buildHeaderArea());
        setCenter(buildBoardArea());
        setBottom(buildStatusBar());
        wireActions();
    }

    /** Remembers the scene so themes can be applied. */
    public void attachScene(Scene scene) {
        themeManager.attach(scene);
    }

    /**
     * Loads the boards from the database and shows the board that was open when the
     * application was closed last time.
     */
    public void initialize() {
        long preferredBoardId = service.getLastBoardId().orElse(-1L);
        List<Board> boards = service.getBoards();

        if (boards.isEmpty()) {
            showEmptyState();
            return;
        }

        Board boardToShow = boards.stream()
                .filter(board -> board.getId() == preferredBoardId)
                .findFirst()
                .orElse(boards.get(0));

        refreshBoardList(boardToShow);
    }

    // ------------------------------------------------------------------
    // Building the screen
    // ------------------------------------------------------------------

    private VBox buildHeaderArea() {
        // --- toolbar -----------------------------------------------------
        Label boardCaption = new Label("Board");
        boardCaption.getStyleClass().add("toolbar-caption");

        boardSelector.setPrefWidth(210);
        boardSelector.setTooltip(new Tooltip("Choose the board you want to work on"));
        boardSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Board board) {
                return board == null ? "" : board.getName();
            }

            @Override
            public Board fromString(String text) {
                return null;
            }
        });

        Button newBoardButton = new Button("New board");
        newBoardButton.getStyleClass().add("primary");
        newBoardButton.setOnAction(event -> createBoard());

        editBoardButton.setOnAction(event -> editBoard());
        deleteBoardButton.getStyleClass().add("danger");
        deleteBoardButton.setOnAction(event -> deleteBoard());

        Label themeCaption = new Label("Theme");
        themeCaption.getStyleClass().add("toolbar-caption");

        themeSelector.setPrefWidth(170);
        themeSelector.setItems(FXCollections.observableArrayList(Theme.values()));
        themeSelector.setCellFactory(list -> new ThemeListCell());
        themeSelector.setButtonCell(new ThemeListCell());
        themeSelector.setTooltip(new Tooltip("Colours of the board - remembered for this board"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        newTaskButton.getStyleClass().add("primary");
        newTaskButton.setOnAction(event -> addTask(TaskStatus.TODO));

        HBox toolbar = new HBox(8, boardCaption, boardSelector, newBoardButton, editBoardButton,
                deleteBoardButton, new Separator(javafx.geometry.Orientation.VERTICAL),
                themeCaption, themeSelector, spacer, newTaskButton);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // --- board title -------------------------------------------------
        boardNameLabel.getStyleClass().add("board-title");
        boardDescriptionLabel.getStyleClass().add("board-subtitle");
        boardDescriptionLabel.setWrapText(true);

        VBox titleBox = new VBox(2, boardNameLabel, boardDescriptionLabel);
        titleBox.getStyleClass().add("board-header");

        return new VBox(toolbar, titleBox);
    }

    private HBox buildBoardArea() {
        columnsBox.getStyleClass().add("board-area");
        columnsBox.setPadding(new Insets(16));
        columnsBox.setAlignment(Pos.TOP_LEFT);

        for (TaskStatus status : TaskStatus.values()) {
            BoardColumnView columnView = new BoardColumnView(
                    status,
                    this::moveTaskToColumn,
                    this::editTask,
                    this::deleteTask,
                    this::showTaskDescription,
                    this::addTask);
            HBox.setHgrow(columnView, Priority.ALWAYS);
            columnView.setMaxWidth(Double.MAX_VALUE);
            columnView.setMinWidth(240);
            columnViews.add(columnView);
            columnsBox.getChildren().add(columnView);
        }
        return columnsBox;
    }

    private HBox buildStatusBar() {
        statusLabel.getStyleClass().add("status-text");
        taskCountLabel.getStyleClass().add("status-text-muted");

        Label databaseLabel = new Label(databaseLocation);
        databaseLabel.getStyleClass().add("status-text-muted");
        databaseLabel.setTooltip(new Tooltip("Your data. Copy this file to back it up."));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(8, statusLabel, spacer, taskCountLabel, new Separator(
                javafx.geometry.Orientation.VERTICAL), databaseLabel);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        return statusBar;
    }

    private void wireActions() {
        boardSelector.setOnAction(event -> {
            if (updatingSelectors || boardSelector.getValue() == null) {
                return;
            }
            showBoard(boardSelector.getValue());
        });

        themeSelector.setOnAction(event -> {
            if (updatingSelectors || themeSelector.getValue() == null) {
                return;
            }
            applyTheme(themeSelector.getValue());
        });
    }

    // ------------------------------------------------------------------
    // Boards
    // ------------------------------------------------------------------

    private void refreshBoardList(Board boardToSelect) {
        updatingSelectors = true;
        boardSelector.setItems(FXCollections.observableArrayList(service.getBoards()));
        boardSelector.setValue(boardToSelect);
        updatingSelectors = false;

        if (boardToSelect == null) {
            showEmptyState();
        } else {
            showBoard(boardToSelect);
        }
    }

    private void showBoard(Board board) {
        currentBoard = board;
        editBoardButton.setDisable(false);
        deleteBoardButton.setDisable(false);
        newTaskButton.setDisable(false);

        updatingSelectors = true;
        boardSelector.setValue(board);
        themeSelector.setValue(board.getTheme());
        updatingSelectors = false;

        themeManager.applyTheme(board.getTheme());
        refreshBoard();

        // Remembered so the same board opens the next time the app starts.
        service.setLastBoardId(board.getId());
    }

    private void showEmptyState() {
        currentBoard = null;
        editBoardButton.setDisable(true);
        deleteBoardButton.setDisable(true);
        newTaskButton.setDisable(true);

        boardNameLabel.setText("No board yet");
        boardDescriptionLabel.setText("Click \"New board\" to create your first board.");

        updatingSelectors = true;
        themeSelector.setValue(service.getDefaultTheme());
        updatingSelectors = false;
        themeManager.applyTheme(service.getDefaultTheme());

        for (BoardColumnView columnView : columnViews) {
            columnView.setTasks(List.of());
        }
        taskCountLabel.setText("0 tasks");
        setStatus("Ready");
    }

    private void createBoard() {
        Dialogs.showBoardDialog(owner(), null, service.getDefaultTheme()).ifPresent(newBoard -> {
            try {
                Board created = service.createBoard(newBoard.getName(), newBoard.getDescription(), newBoard.getTheme());
                refreshBoardList(created);
                setStatus("Board \"" + created.getName() + "\" created");
            } catch (DataAccessException e) {
                Dialogs.error(owner(), "Could not create the board", e.getMessage());
            }
        });
    }

    private void editBoard() {
        if (currentBoard == null) {
            return;
        }
        Dialogs.showBoardDialog(owner(), currentBoard, service.getDefaultTheme()).ifPresent(edited -> {
            try {
                currentBoard.setName(edited.getName());
                currentBoard.setDescription(edited.getDescription());
                currentBoard.setTheme(edited.getTheme());
                service.saveBoard(currentBoard);
                refreshBoardList(currentBoard);
                setStatus("Board saved");
            } catch (DataAccessException e) {
                Dialogs.error(owner(), "Could not save the board", e.getMessage());
            }
        });
    }

    private void deleteBoard() {
        if (currentBoard == null) {
            return;
        }
        String name = currentBoard.getName();
        boolean confirmed = Dialogs.confirm(owner(), "Delete board \"" + name + "\"?",
                "All tasks of this board will be deleted as well. This can not be undone.");
        if (!confirmed) {
            return;
        }
        try {
            service.deleteBoard(currentBoard.getId());
            currentBoard = null;
            List<Board> remaining = service.getBoards();
            if (remaining.isEmpty()) {
                refreshBoardList(null);
            } else {
                refreshBoardList(remaining.get(0));
            }
            setStatus("Board \"" + name + "\" deleted");
        } catch (DataAccessException e) {
            Dialogs.error(owner(), "Could not delete the board", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Tasks
    // ------------------------------------------------------------------

    private void addTask(TaskStatus status) {
        if (currentBoard == null) {
            return;
        }
        Task draft = new Task(currentBoard.getId(), "", status);
        Dialogs.showTaskDialog(owner(), draft, currentBoard.getTheme()).ifPresent(edited -> {
            try {
                service.createTask(currentBoard.getId(), edited.getTitle(), edited.getDescription(),
                        edited.getPriority(), edited.getStatus(), edited.getDueDate());
                refreshBoard();
                setStatus("Task added");
            } catch (DataAccessException e) {
                Dialogs.error(owner(), "Could not add the task", e.getMessage());
            }
        });
    }

    private void editTask(Task task) {
        if (currentBoard == null) {
            return;
        }
        Dialogs.showTaskDialog(owner(), task, currentBoard.getTheme()).ifPresent(edited -> {
            try {
                service.saveTask(edited, task.getStatus());
                refreshBoard();
                setStatus("Task saved");
            } catch (DataAccessException e) {
                Dialogs.error(owner(), "Could not save the task", e.getMessage());
            }
        });
    }

    private void deleteTask(Task task) {
        boolean confirmed = Dialogs.confirm(owner(), "Delete task \"" + task.getTitle() + "\"?",
                "The task will be removed from the board. This can not be undone.");
        if (!confirmed) {
            return;
        }
        try {
            service.deleteTask(task.getId());
            refreshBoard();
            setStatus("Task deleted");
        } catch (DataAccessException e) {
            Dialogs.error(owner(), "Could not delete the task", e.getMessage());
        }
    }

    private void showTaskDescription(Task task) {
        Dialogs.info(owner(), task.getTitle(),
                task.getDescription().isBlank() ? "This task has no description." : task.getDescription());
    }

    /**
     * Moves a card. This is what the drag and drop handlers call when a card is
     * dropped into a column.
     *
     * @param index place inside the target column, {@code Integer.MAX_VALUE} for the end
     */
    public void moveTaskToColumn(long taskId, TaskStatus targetStatus, int index) {
        if (currentBoard == null) {
            return;
        }
        try {
            service.moveTask(taskId, targetStatus, index);
            refreshBoard();
            setStatus("Task moved to \"" + targetStatus.getLabel() + "\"");
        } catch (DataAccessException e) {
            Dialogs.error(owner(), "Could not move the task", e.getMessage());
        }
    }

    private void refreshBoard() {
        if (currentBoard == null) {
            return;
        }
        for (BoardColumnView columnView : columnViews) {
            columnView.setTasks(service.getTasks(currentBoard.getId(), columnView.getStatus()));
        }
        boardNameLabel.setText(currentBoard.getName());
        String description = currentBoard.getDescription();
        boardDescriptionLabel.setText(description == null || description.isBlank()
                ? "No description" : description);

        int total = service.countTasks(currentBoard.getId());
        taskCountLabel.setText(total + (total == 1 ? " task" : " tasks") + " on this board");
    }

    // ------------------------------------------------------------------
    // Theme
    // ------------------------------------------------------------------

    /**
     * The theme of the current board, used by the theme selector in the toolbar.
     * It also lets other code (and the tests) switch the theme without clicking.
     */
    public void selectTheme(Theme theme) {
        updatingSelectors = true;
        themeSelector.setValue(theme);
        updatingSelectors = false;
        applyTheme(theme);
    }

    private void applyTheme(Theme theme) {
        service.setDefaultTheme(theme);
        themeManager.applyTheme(theme);
        if (currentBoard != null && theme != currentBoard.getTheme()) {
            try {
                currentBoard.setTheme(theme);
                service.saveBoard(currentBoard);
                setStatus("Theme changed to " + theme.getDisplayName());
            } catch (DataAccessException e) {
                Dialogs.error(owner(), "Could not save the theme", e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    private Window owner() {
        Scene scene = getScene();
        return scene == null ? null : scene.getWindow();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
