package com.canvaskanban.ui;

import com.canvaskanban.model.Task;
import com.canvaskanban.model.TaskStatus;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * One column of the board ("To Do", "In Progress" or "Done").
 *
 * <p>The column is the drop target for drag and drop: it accepts cards dragged from
 * any column and reports where they were dropped through {@link DropListener}.</p>
 */
public class BoardColumnView extends VBox {

    /** Called when a card is dropped into this column. */
    public interface DropListener {
        void taskDropped(long taskId, TaskStatus targetStatus, int index);
    }

    private final TaskStatus status;
    private final DropListener dropListener;
    private final Consumer<Task> onEdit;
    private final Consumer<Task> onDelete;
    private final Consumer<Task> onShowDescription;

    private final VBox cardBox = new VBox(10);
    private final Label countLabel = new Label("0");

    public BoardColumnView(TaskStatus status,
                           DropListener dropListener,
                           Consumer<Task> onEdit,
                           Consumer<Task> onDelete,
                           Consumer<Task> onShowDescription,
                           Consumer<TaskStatus> onAddTask) {
        this.status = status;
        this.dropListener = dropListener;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onShowDescription = onShowDescription;

        getStyleClass().add("board-column");

        Label titleLabel = new Label(status.getLabel());
        titleLabel.getStyleClass().add("column-title");
        countLabel.getStyleClass().add("column-count");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("+");
        addButton.getStyleClass().addAll("flat", "column-add-button");
        addButton.setTooltip(new Tooltip("Add a task to " + status.getLabel()));
        addButton.setOnAction(event -> onAddTask.accept(status));

        HBox header = new HBox(8, titleLabel, countLabel, spacer, addButton);
        header.getStyleClass().add("column-header");

        cardBox.getStyleClass().add("task-list");
        cardBox.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(cardBox);
        scrollPane.getStyleClass().add("column-scroll");
        scrollPane.setFitToWidth(true);
        // Lets the card list fill the column, so it is a comfortable drop target.
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, scrollPane);
        installDragAndDrop();
    }

    /**
     * Replaces the cards of this column. {@code tasks} must already be in the order
     * they should be shown in.
     */
    public void setTasks(List<Task> tasks) {
        cardBox.getChildren().clear();
        for (Task task : tasks) {
            cardBox.getChildren().add(
                    new TaskCardView(task, onEdit, onDelete, () -> onShowDescription.accept(task)));
        }
        countLabel.setText(String.valueOf(tasks.size()));
    }

    /** The number of cards currently shown. */
    public int getCardCount() {
        return cardBox.getChildren().size();
    }

    public TaskStatus getStatus() {
        return status;
    }

    // ------------------------------------------------------------------
    // Drag and drop
    // ------------------------------------------------------------------

    private void installDragAndDrop() {
        setOnDragOver(event -> {
            if (isTaskDrag(event)) {
                event.acceptTransferModes(TransferMode.MOVE);
                if (!getStyleClass().contains("drop-active")) {
                    getStyleClass().add("drop-active");
                }
            }
            event.consume();
        });

        setOnDragExited(event -> {
            getStyleClass().remove("drop-active");
            event.consume();
        });

        setOnDragDropped(event -> {
            getStyleClass().remove("drop-active");
            long taskId = TaskCardView.draggedTaskId(event.getDragboard());
            boolean moved = false;
            if (taskId > 0 && dropListener != null) {
                dropListener.taskDropped(taskId, status, computeDropIndex(event, taskId));
                moved = true;
            }
            event.setDropCompleted(moved);
            event.consume();
        });
    }

    private boolean isTaskDrag(DragEvent event) {
        return event.getDragboard() != null
                && event.getDragboard().hasContent(TaskCardView.TASK_DATA_FORMAT);
    }

    /**
     * Works out where the card should be inserted: above the first card whose middle is
     * below the mouse pointer, otherwise at the end. The dragged card itself is skipped,
     * because it is still part of the list while it is being dragged.
     */
    private int computeDropIndex(DragEvent event, long draggedTaskId) {
        Point2D pointer = cardBox.sceneToLocal(event.getSceneX(), event.getSceneY());
        List<Node> cards = cardBox.getChildren();

        int index = 0;
        for (Node node : cards) {
            if (node instanceof TaskCardView card && card.getTask().getId() == draggedTaskId) {
                continue;
            }
            Bounds bounds = node.getBoundsInParent();
            if (pointer.getY() < bounds.getMinY() + bounds.getHeight() / 2) {
                return index;
            }
            index++;
        }
        return index;
    }
}
