package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

public class SceneSwitcher {
    private static final Logger logger = LoggerFactory.getLogger(SceneSwitcher.class);
    public static void switchScene(ActionEvent event, String fxmlFile, Integer width, Integer height) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(SceneSwitcher.class.getResource("/com/auction/client/view/" + fxmlFile)));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene;
        if (width == null || height == null) {
            scene = new Scene(root);
        } else {
            scene = new Scene(root, width, height);
        }

        stage.setScene(scene);

        if (width != null) {
            stage.setMinWidth(width);
            stage.setWidth(width);
        }
        if (height != null) {
            stage.setMinHeight(height);
            stage.setHeight(height);
        }
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
        logger.info("Đang chuyển sang: {}", fxmlFile);
    }
}