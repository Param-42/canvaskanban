package com.canvaskanban.ui;

import com.canvaskanban.model.Theme;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the CSS resources without starting JavaFX: every theme must define every
 * colour variable that {@code base.css} uses. This catches a mistyped colour name
 * that would otherwise only show up as a warning in the console.
 */
class ThemeCssTest {

    private static final String BASE_CSS = "/css/base.css";

    @Test
    void baseStylesheetExists() throws IOException {
        String css = read(BASE_CSS);
        assertTrue(css.contains(".app-root"), "base.css should style the window");
        assertTrue(css.contains("-app-card"), "base.css should use the theme variables");
    }

    @Test
    void baseStylesheetDeclaresEveryVariableAsAFallback() throws IOException {
        String css = read(BASE_CSS);
        for (String variable : Theme.COLOR_VARIABLES) {
            assertTrue(declares(css, variable),
                    "base.css should have a fallback for " + variable);
        }
    }

    @Test
    void everyThemeHasItsOwnStylesheet() {
        for (Theme theme : Theme.values()) {
            assertNotNull(ThemeCssTest.class.getResource(theme.getCssResource()),
                    "missing stylesheet for theme " + theme.getDisplayName());
        }
    }

    @Test
    void everyThemeDefinesEveryColourVariable() throws IOException {
        for (Theme theme : Theme.values()) {
            String css = read(theme.getCssResource());
            for (String variable : Theme.COLOR_VARIABLES) {
                assertTrue(declares(css, variable),
                        "theme " + theme.getDisplayName() + " does not define " + variable);
            }
        }
    }

    @Test
    void themesActuallyUseDifferentColours() throws IOException {
        String light = read(Theme.MINIMAL_LIGHT.getCssResource());
        String dark = read(Theme.DARK_MODE.getCssResource());
        assertTrue(!light.equals(dark), "two themes should not be identical");
    }

    @Test
    void thereAreAtLeastTheSixDocumentedThemes() {
        assertEquals(6, Theme.values().length);
        assertEquals(Theme.MINIMAL_LIGHT, Theme.fromId("minimal-light"));
        assertEquals(Theme.DARK_MODE, Theme.fromId("dark-mode"));
        assertEquals(Theme.MIDNIGHT_BLUE, Theme.fromId("midnight-blue"));
        assertEquals(Theme.FOREST_GREEN, Theme.fromId("forest-green"));
        assertEquals(Theme.LAVENDER, Theme.fromId("lavender"));
        assertEquals(Theme.WARM_SAND, Theme.fromId("warm-sand"));
    }

    @Test
    void anUnknownThemeFallsBackToTheDefault() {
        assertEquals(Theme.MINIMAL_LIGHT, Theme.fromId("does-not-exist"));
        assertEquals(Theme.MINIMAL_LIGHT, Theme.fromId(null));
        assertEquals(Theme.DARK_MODE, Theme.fromId("DARK-MODE"));
    }

    /** A declaration is "-name:" somewhere in the file. */
    private boolean declares(String css, String variable) {
        return css.contains(variable + ":");
    }

    private String read(String resource) throws IOException {
        try (InputStream stream = ThemeCssTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "resource not found: " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
