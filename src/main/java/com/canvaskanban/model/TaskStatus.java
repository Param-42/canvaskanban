package com.canvaskanban.model;

/**
 * The columns of a board. CanvasKanban always uses these three default columns,
 * which keeps the application easy to understand.
 */
public enum TaskStatus {

    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    /** The column title that is shown in the user interface. */
    public String getLabel() {
        return label;
    }

    /**
     * Looks up a column by name. Falls back to {@link #TODO} for unknown values.
     */
    public static TaskStatus fromName(String name) {
        if (name != null) {
            for (TaskStatus status : values()) {
                if (status.name().equalsIgnoreCase(name.trim())) {
                    return status;
                }
            }
        }
        return TODO;
    }

    @Override
    public String toString() {
        return label;
    }
}
