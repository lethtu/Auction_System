package com.auction.client.service;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary safe style manager.
 *
 * Theme/accent switching is intentionally disabled for now because the current UI
 * mixes CSS theming with hard-coded Java inline colors. Keeping theme switching
 * partially enabled makes screens inconsistent. This class keeps the app on the
 * stable base stylesheet while preserving the API used by Main, SceneSwitcher and
 * SettingsController.
 */
public final class AppStyleManager {
    private static final Logger logger = LoggerFactory.getLogger(AppStyleManager.class);

    private AppStyleManager() {
    }

    public static void applyCurrentStyle(Scene scene) {
        if (scene == null) return;
        removeExperimentalThemeSheets(scene);
        updateRootStyleClass(scene);
    }

    public static void applyCurrentStyle(Parent root) {
        if (root == null) return;
        Scene scene = root.getScene();
        if (scene == null) {
            Platform.runLater(() -> {
                if (root.getScene() != null) {
                    applyCurrentStyle(root.getScene());
                }
            });
        } else {
            applyCurrentStyle(scene);
        }
    }

    public static void applyCurrentStyleToOpenWindows() {
        for (Window window : Window.getWindows()) {
            if (window instanceof javafx.stage.Stage stage && stage.getScene() != null) {
                applyCurrentStyle(stage.getScene());
            }
        }
    }

    public static void updateRootStyleClass(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        updateRootStyleClass(scene.getRoot());
    }

    public static void updateRootStyleClass(Parent root) {
        if (root == null) return;
        root.getStyleClass().removeAll(
                "theme-light", "theme-dark",
                "accent-pink", "accent-purple", "accent-emerald", "accent-blue", "accent-orange"
        );
        logger.debug("Experimental theme classes disabled. Root classes now: {}", root.getStyleClass());
    }

    private static void removeExperimentalThemeSheets(Scene scene) {
        scene.getStylesheets().removeIf(s -> s.contains("theme-dark.css")
                || s.contains("theme-light.css")
                || s.contains("accent-pink.css")
                || s.contains("accent-purple.css")
                || s.contains("accent-emerald.css")
                || s.contains("accent-blue.css")
                || s.contains("accent-orange.css"));
    }

    public static void clearCurrentStyle(javafx.scene.Scene scene) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().removeIf(style -> style != null && (
                style.contains("theme-dark.css")
                        || style.contains("theme-light.css")
                        || style.contains("accent-")
        ));
        if (scene.getRoot() != null) {
            scene.getRoot().getStyleClass().removeAll(
                    "theme-dark",
                    "theme-light",
                    "accent-pink",
                    "accent-purple",
                    "accent-blue",
                    "accent-emerald",
                    "accent-orange"
            );
        }
    }
}
