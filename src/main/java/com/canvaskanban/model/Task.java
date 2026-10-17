package com.canvaskanban.model;

import java.time.LocalDate;

/**
 * A single card on the board.
 */
public class Task {

    private long id;
    private long boardId;
    private String title;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private LocalDate createdAt;
    private LocalDate dueDate;
    private int position;

    public Task() {
        this(0L, 0L, "", "", Priority.MEDIUM, TaskStatus.TODO, LocalDate.now(), null, 0);
    }

    /** Used when creating a new task in a column. */
    public Task(long boardId, String title, TaskStatus status) {
        this(0L, boardId, title, "", Priority.MEDIUM, status, LocalDate.now(), null, 0);
    }

    public Task(long id, long boardId, String title, String description, Priority priority,
                TaskStatus status, LocalDate createdAt, LocalDate dueDate, int position) {
        this.id = id;
        this.boardId = boardId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.dueDate = dueDate;
        this.position = position;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBoardId() {
        return boardId;
    }

    public void setBoardId(long boardId) {
        this.boardId = boardId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    /** May be {@code null} when the task has no deadline. */
    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /** Sort order inside a column. Lower numbers are shown first. */
    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    /** True when the due date is in the past and the task is not finished yet. */
    public boolean isOverdue() {
        return dueDate != null && status != TaskStatus.DONE && dueDate.isBefore(LocalDate.now());
    }

    /** A task is "new" while it has no database id. */
    public boolean isNew() {
        return id <= 0L;
    }

    @Override
    public String toString() {
        return title;
    }
}
