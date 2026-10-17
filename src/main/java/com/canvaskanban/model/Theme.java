package com.canvaskanban.model;

import java.util.List;

/**
 * The built-in colour schemes ("themes") of CanvasKanban.
 *
 * <p>Every theme is a plain CSS file in {@code src/main/resources/css}. Each file only
 * declares the colour variables listed in {@link #COLOR_VARIABLES}; the layout itself
 * lives in {@code base.css}. That way a new theme is just a new small CSS file.</p>
 */
public enum Theme {

    MINIMAL_LIGHT("Minimal Light", "minimal-light", "#2563eb"),
    DARK_MODE("Dark Mode", "dark-mode", "#4f9cf9"),
    MIDNIGHT_BLUE("Midnight Blue", "midnight-blue", "#2f6fed"),
    FOREST_GREEN("Forest Green", "forest-green", "#2f7d4f"),
    LAVENDER("Lavender", "lavender", "#7c5cd6"),
    WARM_SAND("Warm Sand", "warm-sand", "#b4713a");

    /**
     * The colour variables that {@code base.css} uses. Every theme CSS file must
     * define all of them, otherwise parts of the board would look wrong.
     * {@code ThemeCssTest} checks this automatically.
     */
    public static final List<String> COLOR_VARIABLES = List.of(
            "-app-bg",
            "-app-toolbar",
            "-app-toolbar-text",
            "-app-surface",
            "-app-column-header",
            "-app-list-bg",
            "-app-card",
            "-app-card-hover",
            "-app-border",
            "-app-text",
            "-app-text-muted",
            "-app-accent",
            "-app-accent-text",
            "-app-danger",
            "-app-chip-text",
            "-app-priority-high",
            "-app-priority-medium",
            "-app-priority-low",
            "-app-drop-highlight",
            // JavaFX (Modena) variables, so that dialogs, text fields and popup menus
            // follow the theme as well.
            "-fx-base",
            "-fx-control-inner-background",
            "-fx-text-background-color",
            "-fx-accent",
            "-fx-focus-color"
    );

    private final String displayName;
    private final String id;
    private final String previewColor;

    Theme(String displayName, String id, String previewColor) {
        this.displayName = displayName;
        this.id = id;
        this.previewColor = previewColor;
    }

    /** Text shown in the theme selector. */
    public String getDisplayName() {
        return displayName;
    }

    /** Short identifier stored in the database (for example "dark-mode"). */
    public String getId() {
        return id;
    }

    /** Colour used for the little dot next to the theme name in the selector. */
    public String getPreviewColor() {
        return previewColor;
    }

    /** Classpath location of the theme CSS file. */
    public String getCssResource() {
        return "/css/" + id + ".css";
    }

    /**
     * Finds a theme by its id. Unknown ids (for example from a hand edited
     * database) fall back to {@link #MINIMAL_LIGHT} instead of failing.
     */
    public static Theme fromId(String id) {
        if (id != null) {
            String wanted = id.trim();
            for (Theme theme : values()) {
                if (theme.id.equalsIgnoreCase(wanted) || theme.name().equalsIgnoreCase(wanted)) {
                    return theme;
                }
            }
        }
        return MINIMAL_LIGHT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
