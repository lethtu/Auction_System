package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import javafx.scene.control.MenuItem;

public class SceneSwitcher {
    private static final Logger logger = LoggerFactory.getLogger(SceneSwitcher.class);

    public static final double DEFAULT_WIDTH = 1100;
    public static final double DEFAULT_HEIGHT = 700;
    public static final double MIN_WIDTH = 900;
    public static final double MIN_HEIGHT = 600;

    public static Stage getStage(javafx.event.Event event) {
        Object source = event.getSource();
        if (source instanceof Node node) {
            return (Stage) node.getScene().getWindow();
        } else if (source instanceof MenuItem menuItem) {
            return (Stage) menuItem.getParentPopup().getOwnerWindow();
        }
        return null;
    }

    public static void handleMinimize(javafx.event.Event event) {
        Stage stage = getStage(event);
        if (stage != null) {
            stage.setIconified(true);
        }
    }

    public static void handleMaximize(javafx.event.Event event) {
        Stage stage = getStage(event);
        if (stage != null) {
            com.auction.client.util.ResizeHelper.toggleMaximize(stage);
        }
    }

    public static void handleClose(javafx.event.Event event) {
        Stage stage = getStage(event);
        if (stage != null) {
            stage.close();
        }
    }

    public static FXMLLoader Switch(Event event, String fxmlFile, Integer targetWidth, Integer targetHeight)
            throws IOException {
        String path = "/com/auction/client/view/" + fxmlFile;
        URL xmlResource = SceneSwitcher.class.getResource(path);

        if (xmlResource == null) {
            throw new RuntimeException("FXML file not found: " + path);
        }

        FXMLLoader loader = new FXMLLoader(xmlResource);
        Parent root = loader.load();

        Stage stage = null;
        Object source = event.getSource();

        if (source instanceof Node) {
            stage = (Stage) ((Node) source).getScene().getWindow();
        } else if (source instanceof MenuItem) {
            MenuItem menuItem = (MenuItem) source;
            stage = (Stage) menuItem.getParentPopup().getOwnerWindow();
        }

        if (stage == null) {
            throw new RuntimeException("Cannot determine current Stage for scene switching!");
        }

        boolean wasMaximized = stage.isMaximized();
        boolean wasFullScreen = stage.isFullScreen();
        boolean isAuthScene = fxmlFile.equals("Login.fxml") || fxmlFile.equals("SignUp.fxml")
                || fxmlFile.equals("ForgotPassword.fxml");

        double currentWidth = stage.getWidth() > MIN_WIDTH ? stage.getWidth() : DEFAULT_WIDTH;
        double currentHeight = stage.getHeight() > MIN_HEIGHT ? stage.getHeight() : DEFAULT_HEIGHT;

        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        com.auction.client.util.ResizeHelper.install(stage, root);

        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setResizable(!isAuthScene);

        if (!wasMaximized && !wasFullScreen && !isAuthScene) {
            if (Double.isNaN(currentWidth) || currentWidth < MIN_WIDTH) {
                stage.setWidth(DEFAULT_WIDTH);
            } else {
                stage.setWidth(currentWidth);
            }

            if (Double.isNaN(currentHeight) || currentHeight < MIN_HEIGHT) {
                stage.setHeight(DEFAULT_HEIGHT);
            } else {
                stage.setHeight(currentHeight);
            }
        }

        final Stage finalStage = stage;
        Platform.runLater(() -> {
            finalStage.setMaximized(wasMaximized);
            finalStage.setFullScreen(wasFullScreen);
        });

        logger.info("Switching to: {}", fxmlFile);
        return loader;
    }

    public static FXMLLoader switchScene(Event event, String fxmlFile) throws IOException {
        return Switch(event, fxmlFile, null, null);
    }

    public static FXMLLoader switchScene(Event event, String fxmlFile, Integer width, Integer height)
            throws IOException {
        return Switch(event, fxmlFile, width, height);
    }
}