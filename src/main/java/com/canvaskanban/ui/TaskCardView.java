package com.canvaskanban.ui;

import com.canvaskanban.model.Task;

import java.util.Locale;
import java.util.function.Consumer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * One card on the board.
 *
 * <p>A card shows the title, a shortened description, the priority and the due date.
 * Double clicking it (or using the right click menu) opens the edit dialog, and the
 * card can be dragged to another column.</p>
 */
public class TaskCardView extends VBox {

    /** Marks the data that is put on the dragboard while a card is dragged. */
    public static final DataFormat TASK_DATA_FORMAT = new DataFormat("application/x-canvaskanban-task");

    private final Task task;

    public TaskCardView(Task task, Consumer<Task> onEdit, Consumer<Task> onDelete, Runnable onShowDescription) {
        this.task = task;
        getStyleClass().add("task-card");

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("task-card-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        getChildren().add(titleLabel);

        if (!task.getDescription().isBlank()) {
            Label descriptionLabel = new Label(task.getDescription());
            descriptionLabel.getStyleClass().add("task-card-description");
            descriptionLabel.setWrapText(true);
            descriptionLabel.setMaxHeight(44);
            descriptionLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            descriptionLabel.setMaxWidth(Double.MAX_VALUE);
            getChildren().add(descriptionLabel);
        }

        getChildren().add(createFooter());

        // --- interaction ------------------------------------------------
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onEdit.accept(task);
            }
        });

        MenuItem editItem = new MenuItem("Edit task");
        editItem.setOnAction(event -> onEdit.accept(task));

        MenuItem descriptionItem = new MenuItem("Show description");
        descriptionItem.setOnAction(event -> onShowDescription.run());

        MenuItem deleteItem = new MenuItem("Delete task");
        deleteItem.setOnAction(event -> onDelete.accept(task));

        ContextMenu contextMenu = new ContextMenu(editItem, descriptionItem, new SeparatorMenuItem(), deleteItem);
        setOnContextMenuRequested(event -> contextMenu.show(this, event.getScreenX(), event.getScreenY()));

        setOnDragDetected(event -> {
            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(TASK_DATA_FORMAT, task.getId());
            dragboard.setContent(content);
            getStyleClass().add("task-card-dragging");
            event.consume();
        });

        setOnDragDone(event -> {
            getStyleClass().remove("task-card-dragging");
            event.consume();
        });
    }

    public Task getTask() {
        return task;
    }

    /** Reads the task id from a drag, or -1 when the drag is not a task card. */
    public static long draggedTaskId(Dragboard dragboard) {
        if (dragboard == null) {
            return -1L;
        }
        Object content = dragboard.getContent(TASK_DATA_FORMAT);
        return content instanceof Long id ? id : -1L;
    }

    private HBox createFooter() {
        Label priorityChip = new Label(task.getPriority().getLabel());
        priorityChip.getStyleClass().addAll("chip", "priority-" + task.getPriority().name().toLowerCase(Locale.ROOT));

        Label createdLabel = new Label("Created " + Dialogs.formatDate(task.getCreatedAt()));
        createdLabel.getStyleClass().add("task-card-created");

        HBox footer = new HBox(8, priorityChip, createdLabel);
        footer.getStyleClass().add("task-card-footer");

        if (task.getDueDate() != null) {
            Label dueLabel = new Label("Due " + Dialogs.formatDate(task.getDueDate()));
            dueLabel.getStyleClass().add("task-card-due");
            if (task.isOverdue()) {
                dueLabel.getStyleClass().add("overdue");
            }
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            footer.getChildren().addAll(spacer, dueLabel);
        }
        return footer;
    }
}
