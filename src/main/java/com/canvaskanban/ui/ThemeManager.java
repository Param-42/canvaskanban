package com.canvaskanban.ui;

import com.canvaskanban.model.Theme;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;

/**
 * Applies theme CSS files to the application window (and to dialogs).
 *
 * <p>{@code base.css} contains all the layout rules and only uses colour variables.
 * A theme file such as {@code dark-mode.css} defines those variables. Both files are
 * added to the scene, the theme last, so the theme wins.</p>
 */
public class ThemeManager {

    /** Layout and appearance rules, shared by every theme. */
    private static final String BASE_STYLESHEET = "/css/base.css";

    private Theme currentTheme = Theme.MINIMAL_LIGHT;
    private Scene scene;

    /** Remembers the scene so it can be re-styled later. */
    public void attach(Scene scene) {
        this.scene = scene;
        applyTheme(currentTheme);
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /** Switches the whole window to another theme. */
    public void applyTheme(Theme theme) {
        currentTheme = theme == null ? Theme.MINIMAL_LIGHT : theme;
        if (scene != null) {
            scene.getStylesheets().setAll(stylesheetsFor(currentTheme));
        }
    }

    /**
     * The stylesheet list for a theme. Dialogs use this as well, so they look the
     * same as the main window.
     */
    public static List<String> stylesheetsFor(Theme theme) {
        List<String> stylesheets = new ArrayList<>(2);
        addIfPresent(stylesheets, BASE_STYLESHEET);
        addIfPresent(stylesheets, theme.getCssResource());
        return stylesheets;
    }

    private static void addIfPresent(List<String> target, String resource) {
        if (ThemeManager.class.getResource(resource) != null) {
            target.add(resource);
        }
    }
}
