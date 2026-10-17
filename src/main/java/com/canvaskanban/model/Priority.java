package com.canvaskanban.model;

/**
 * How urgent a task is. Stored in the database as the enum name (for example "HIGH").
 */
public enum Priority {

    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    /** The text that is shown in the user interface. */
    public String getLabel() {
        return label;
    }

    /**
     * Looks up a priority by name. Never returns null, so old or unexpected
     * values in the database can not crash the application.
     */
    public static Priority fromName(String name) {
        if (name != null) {
            for (Priority priority : values()) {
                if (priority.name().equalsIgnoreCase(name.trim())) {
                    return priority;
                }
            }
        }
        return MEDIUM;
    }

    @Override
    public String toString() {
        return label;
    }
}
