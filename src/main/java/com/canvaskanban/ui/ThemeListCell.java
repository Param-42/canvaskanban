package com.canvaskanban.ui;

import com.canvaskanban.model.Theme;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * Shows a theme in a combo box: a small colour dot plus the theme name.
 */
public class ThemeListCell extends ListCell<Theme> {

    @Override
    protected void updateItem(Theme theme, boolean empty) {
        super.updateItem(theme, empty);

        if (empty || theme == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        Region dot = new Region();
        dot.getStyleClass().add("theme-dot");
        dot.setStyle("-fx-background-color: " + theme.getPreviewColor() + ";");

        setText(null);
        setGraphic(new HBox(8, dot, new Label(theme.getDisplayName())));
    }
}
