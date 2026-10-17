package com.canvaskanban;

import com.canvaskanban.database.Database;
import com.canvaskanban.service.BoardService;
import com.canvaskanban.ui.MainView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/**
 * The JavaFX application. It prepares the database, builds the main window and
 * applies the theme that was saved with the board.
 *
 * <p>Start the application with {@code mvn clean javafx:run} while developing, or
 * through {@link Launcher} when it is packaged.</p>
 */
public class MainApp extends Application {

    private static final double WINDOW_WIDTH = 1180;
    private static final double WINDOW_HEIGHT = 740;

    private Database database;
    private BoardService service;
    private MainView mainView;

    /** Filled in {@link #init()} when the database could not be prepared. */
    private Exception startupError;

    /**
     * Runs before the window is shown, so the database is ready when the first
     * screen is built. Exceptions are remembered and shown to the user afterwards.
     */
    @Override
    public void init() {
        try {
            database = new Database();
            database.initialize();
            service = new BoardService(database);
            service.createSampleDataIfEmpty();
        } catch (Exception e) {
            startupError = e;
        }
    }

    @Override
    public void start(Stage stage) {
        if (startupError != null) {
            showStartupError(startupError);
            return;
        }

        mainView = new MainView(service, database.describeLocation());

        Scene scene = new Scene(mainView, WINDOW_WIDTH, WINDOW_HEIGHT);
        mainView.attachScene(scene);

        stage.setTitle("CanvasKanban");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);

        // Loads the boards and restores the last one, then shows the window.
        mainView.initialize();
        stage.show();
    }

    @Override
    public void stop() {
        // Every database operation closes its own connection, so there is nothing to
        // release here. The method is kept as a place for future clean up.
    }

    private void showStartupError(Exception error) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("CanvasKanban");
        alert.setHeaderText("CanvasKanban could not start");
        alert.setContentText("The database could not be prepared.\n\n" + error.getMessage()
                + "\n\nYou can change the data folder with the system property "
                + "\"-Dcanvaskanban.data.dir=...\".");
        alert.showAndWait();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
