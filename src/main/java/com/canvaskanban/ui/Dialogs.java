package com.canvaskanban.ui;

import com.canvaskanban.model.Board;
import com.canvaskanban.model.Priority;
import com.canvaskanban.model.Task;
import com.canvaskanban.model.TaskStatus;
import com.canvaskanban.model.Theme;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * The dialogs of CanvasKanban: add/edit a task, add/edit a board and the
 * "are you sure?" confirmation.
 *
 * <p>Everything is built in plain Java code (no FXML) so it can be read top to bottom.</p>
 */
public final class Dialogs {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private Dialogs() {
        // Static helpers only.
    }

    // ------------------------------------------------------------------
    // Task dialog
    // ------------------------------------------------------------------

    /**
     * Shows the "New task" / "Edit task" dialog.
     *
     * @param owner the window the dialog belongs to (may be {@code null} in tests)
     * @param task  the task to edit, or {@code null} when a new task is created
     * @param theme theme used to style the dialog
     * @return the edited task, or empty when the user cancelled
     */
    public static Optional<Task> showTaskDialog(Window owner, Task task, Theme theme) {
        boolean isNew = task == null || task.isNew();
        Task working = task == null ? new Task() : copyOf(task);
        if (!isNew) {
            // Editing keeps the original status so the caller can detect a column change.
            working.setStatus(task.getStatus());
        }

        TextField titleField = new TextField(working.getTitle());
        titleField.setPromptText("What has to be done?");

        TextArea descriptionArea = new TextArea(working.getDescription());
        descriptionArea.setPromptText("Optional details");
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setWrapText(true);

        ComboBox<TaskStatus> columnBox = new ComboBox<>(FXCollections.observableArrayList(TaskStatus.values()));
        columnBox.setValue(working.getStatus());
        columnBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Priority> priorityBox = new ComboBox<>(FXCollections.observableArrayList(Priority.values()));
        priorityBox.setValue(working.getPriority());
        priorityBox.setMaxWidth(Double.MAX_VALUE);

        DatePicker dueDatePicker = new DatePicker(working.getDueDate());
        dueDatePicker.setPromptText("No due date");
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);

        Button clearDueDateButton = new Button("Clear");
        clearDueDateButton.getStyleClass().add("flat");
        clearDueDateButton.setOnAction(event -> dueDatePicker.setValue(null));

        HBox dueDateRow = new HBox(6, dueDatePicker, clearDueDateButton);
        HBox.setHgrow(dueDatePicker, javafx.scene.layout.Priority.ALWAYS);

        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Title"), titleField);
        form.addRow(1, new Label("Description"), descriptionArea);
        form.addRow(2, new Label("Column"), columnBox);
        form.addRow(3, new Label("Priority"), priorityBox);
        form.addRow(4, new Label("Due date"), dueDateRow);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        showError(errorLabel, false, "");

        VBox content = new VBox(10, form, errorLabel);
        content.getStyleClass().add("dialog-content");

        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "New task" : "Edit task");
        dialog.setHeaderText(isNew ? "Add a card to the board" : "Change this card");
        initOwner(dialog, owner);
        prepareDialogPane(dialog, content, theme, 460);

        ButtonType saveButtonType = new ButtonType(isNew ? "Add task" : "Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.getStyleClass().add("primary");
        saveButton.setDisable(working.getTitle().isBlank());

        titleField.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean blank = newValue == null || newValue.isBlank();
            saveButton.setDisable(blank);
            showError(errorLabel, blank, "Please enter a title.");
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            working.setTitle(titleField.getText().trim());
            working.setDescription(trimmed(descriptionArea.getText()));
            working.setStatus(columnBox.getValue() == null ? TaskStatus.TODO : columnBox.getValue());
            working.setPriority(priorityBox.getValue() == null ? Priority.MEDIUM : priorityBox.getValue());
            working.setDueDate(dueDatePicker.getValue());
            return working;
        });

        Platform.runLater(titleField::requestFocus);
        return dialog.showAndWait();
    }

    // ------------------------------------------------------------------
    // Board dialog
    // ------------------------------------------------------------------

    /**
     * Shows the "New board" / "Edit board" dialog.
     *
     * @param board       the board to edit, or {@code null} for a new board
     * @param defaultTheme theme selected when a new board is created
     */
    public static Optional<Board> showBoardDialog(Window owner, Board board, Theme defaultTheme) {
        boolean isNew = board == null || board.isNew();
        Board working = board == null ? new Board("", "", defaultTheme) : copyOf(board);

        TextField nameField = new TextField(working.getName());
        nameField.setPromptText("For example: College project");

        TextArea descriptionArea = new TextArea(working.getDescription());
        descriptionArea.setPromptText("Optional description");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);

        ComboBox<Theme> themeBox = new ComboBox<>(FXCollections.observableArrayList(Theme.values()));
        themeBox.setValue(working.getTheme());
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setCellFactory(list -> new ThemeListCell());
        themeBox.setButtonCell(new ThemeListCell());

        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.addRow(0, new Label("Name"), nameField);
        form.addRow(1, new Label("Description"), descriptionArea);
        form.addRow(2, new Label("Theme"), themeBox);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        showError(errorLabel, false, "");

        VBox content = new VBox(10, form, errorLabel);
        content.getStyleClass().add("dialog-content");

        Dialog<Board> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "New board" : "Edit board");
        dialog.setHeaderText(isNew ? "Create a board" : "Change this board");
        initOwner(dialog, owner);
        prepareDialogPane(dialog, content, defaultTheme, 460);

        ButtonType saveButtonType = new ButtonType(isNew ? "Create" : "Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.getStyleClass().add("primary");
        saveButton.setDisable(working.getName().isBlank());

        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean blank = newValue == null || newValue.isBlank();
            saveButton.setDisable(blank);
            showError(errorLabel, blank, "Please enter a name.");
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            working.setName(nameField.getText().trim());
            working.setDescription(trimmed(descriptionArea.getText()));
            working.setTheme(themeBox.getValue() == null ? defaultTheme : themeBox.getValue());
            return working;
        });

        Platform.runLater(nameField::requestFocus);
        return dialog.showAndWait();
    }

    // ------------------------------------------------------------------
    // Confirmations and messages
    // ------------------------------------------------------------------

    /** Returns {@code true} when the user confirms the action. */
    public static boolean confirm(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("CanvasKanban");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        initOwner(alert, owner);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static void error(Window owner, String title, String message) {
        show(owner, Alert.AlertType.ERROR, title, message);
    }

    public static void info(Window owner, String title, String message) {
        show(owner, Alert.AlertType.INFORMATION, title, message);
    }

    private static void show(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("CanvasKanban");
        alert.setHeaderText(title);
        alert.setContentText(message);
        initOwner(alert, owner);
        alert.showAndWait();
    }

    /** Formats a date the way it is shown on the cards. */
    public static String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMAT.format(date);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void prepareDialogPane(Dialog<?> dialog, Node content, Theme theme, double width) {
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().addAll(ThemeManager.stylesheetsFor(theme));
        dialog.getDialogPane().getStyleClass().add("app-dialog");
        dialog.getDialogPane().setPrefWidth(width);
    }

    /** Only sets an owner when there is one; tests build dialogs without a window. */
    private static void initOwner(Dialog<?> dialog, Window owner) {
        if (owner != null) {
            dialog.initOwner(owner);
        }
    }

    private static void initOwner(Alert alert, Window owner) {
        if (owner != null) {
            alert.initOwner(owner);
        }
    }

    private static void showError(Label errorLabel, boolean visible, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(visible);
        errorLabel.setManaged(visible);
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private static Task copyOf(Task task) {
        return new Task(task.getId(), task.getBoardId(), task.getTitle(), task.getDescription(),
                task.getPriority(), task.getStatus(), task.getCreatedAt(), task.getDueDate(), task.getPosition());
    }

    private static Board copyOf(Board board) {
        return new Board(board.getId(), board.getName(), board.getDescription(),
                board.getTheme(), board.getCreatedAt());
    }
}
