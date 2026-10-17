package com.canvaskanban;

/**
 * Plain Java entry point of the application.
 *
 * <p>The packaged application (created with jpackage) starts this class. It must
 * <strong>not</strong> extend {@code javafx.application.Application}, because the Java
 * launcher refuses to start an {@code Application} subclass when JavaFX is not on the
 * module path. From here JavaFX can be started normally.</p>
 *
 * <p>Run it directly with:</p>
 * <pre>java -cp "canvaskanban-1.0.0.jar:libs/*" com.canvaskanban.Launcher</pre>
 */
public final class Launcher {

    private Launcher() {
        // Entry point only.
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
