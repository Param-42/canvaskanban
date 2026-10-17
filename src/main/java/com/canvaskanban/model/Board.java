package com.canvaskanban.model;

/**
 * A Kanban board. A board owns a set of tasks and remembers which theme
 * (colour scheme) the user selected for it.
 */
public class Board {

    private long id;
    private String name;
    private String description;
    private Theme theme;
    private String createdAt;

    public Board() {
        this(0L, "", "", Theme.MINIMAL_LIGHT, "");
    }

    /** Used when creating a brand new board (no database id yet). */
    public Board(String name, String description, Theme theme) {
        this(0L, name, description, theme, "");
    }

    public Board(long id, String name, String description, Theme theme, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.theme = theme;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    /** ISO date (yyyy-MM-dd) on which the board was created. */
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /** A board is "new" while it has no database id. */
    public boolean isNew() {
        return id <= 0L;
    }

    @Override
    public String toString() {
        return name;
    }
}
