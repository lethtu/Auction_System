package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import javafx.scene.control.MenuItem;
import com.auction.client.util.ResizeHelper;
import com.auction.client.service.AppStyleManager;

public class SceneSwitcher {
    private static final Logger logger = LoggerFactory.getLogger(SceneSwitcher.class);

    public static final double DEFAULT_WIDTH = 1100;
    public static final double DEFAULT_HEIGHT = 700;
    public static final double MIN_WIDTH = 900;
    public static final double MIN_HEIGHT = 600;
    private static final double AUTH_WIDTH = 1000;
    private static final double AUTH_HEIGHT = 700;
    private static final double APP_WIDTH = 1200;
    private static final double APP_HEIGHT = 800;

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

        boolean wasMaximized = ResizeHelper.isMaximized(stage);
        boolean wasFullScreen = stage.isFullScreen();
        boolean isAuthScene = fxmlFile.equals("Login.fxml") || fxmlFile.equals("SignUp.fxml")
                || fxmlFile.equals("ForgotPassword.fxml");

        Parent currentRoot = stage.getScene() != null ? stage.getScene().getRoot() : null;
        boolean isCurrentRootAuth = false;
        if (currentRoot != null) {
            isCurrentRootAuth = currentRoot.getStyleClass().contains("auth-root");
        }

        boolean hideStageWhileLoading = stage.isShowing() && isCurrentRootAuth && !isAuthScene;
        double previousOpacity = stage.getOpacity();
        if (hideStageWhileLoading) {
            stage.setOpacity(0.0);
        }

        FXMLLoader loader = new FXMLLoader(xmlResource);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException | RuntimeException e) {
            if (hideStageWhileLoading) {
                stage.setOpacity(previousOpacity);
            }
            throw e;
        }

        if (!wasMaximized && !wasFullScreen) {
            applyTargetBounds(stage, isAuthScene, isCurrentRootAuth, targetWidth, targetHeight);
        }

        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, stage.getWidth(), stage.getHeight());
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        AppStyleManager.applyCurrentStyle(scene);
        ResizeHelper.install(stage, root);

        stage.setMinWidth(isAuthScene ? AUTH_WIDTH : MIN_WIDTH);
        stage.setMinHeight(isAuthScene ? AUTH_HEIGHT : MIN_HEIGHT);
        stage.setResizable(!isAuthScene);

        if (wasMaximized) {
            final Stage finalStage = stage;
            Platform.runLater(() -> ResizeHelper.reapplyMaximizedState(finalStage));
        }
        if (hideStageWhileLoading) {
            stage.setOpacity(previousOpacity);
        }

        logger.info("Switching to: {}", fxmlFile);
        return loader;
    }

    private static void applyTargetBounds(Stage stage, boolean isAuthScene, boolean isCurrentRootAuth,
            Integer targetWidth, Integer targetHeight) {
        double desiredWidth = stage.getWidth();
        double desiredHeight = stage.getHeight();

        if (isAuthScene) {
            desiredWidth = AUTH_WIDTH;
            desiredHeight = AUTH_HEIGHT;
        } else if (isCurrentRootAuth || desiredWidth < APP_WIDTH || desiredHeight < APP_HEIGHT) {
            desiredWidth = (targetWidth != null) ? targetWidth : APP_WIDTH;
            desiredHeight = (targetHeight != null) ? targetHeight : APP_HEIGHT;
        } else {
            return;
        }

        Rectangle2D bounds = screenBounds(stage);
        double centeredX = bounds.getMinX() + (bounds.getWidth() - desiredWidth) / 2.0;
        double centeredY = bounds.getMinY() + (bounds.getHeight() - desiredHeight) / 2.0;
        stage.setX(Math.max(bounds.getMinX(), centeredX));
        stage.setY(Math.max(bounds.getMinY(), centeredY));
        stage.setWidth(desiredWidth);
        stage.setHeight(desiredHeight);
    }

    private static Rectangle2D screenBounds(Stage stage) {
        var screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Screen target = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        return target.getVisualBounds();
    }

    public static FXMLLoader switchScene(Event event, String fxmlFile) throws IOException {
        return Switch(event, fxmlFile, null, null);
    }

    public static FXMLLoader switchScene(Event event, String fxmlFile, Integer width, Integer height)
            throws IOException {
        return Switch(event, fxmlFile, width, height);
    }
}
