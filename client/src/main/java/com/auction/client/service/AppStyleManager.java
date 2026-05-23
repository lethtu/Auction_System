package com.auction.client.service;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Centralized styling bridge for the app shell.
 * Auth screens keep their own design; main app screens can use persisted
 * light/dark theme and accent color safely through CSS files and root classes.
 */
public final class AppStyleManager {
    private static final Logger logger = LoggerFactory.getLogger(AppStyleManager.class);

    private static final String[] MANAGED_STYLESHEETS = {
            "global-tokens.css",
            "theme-light.css",
            "theme-dark.css",
            "accent-pink.css",
            "accent-purple.css",
            "accent-emerald.css",
            "accent-blue.css",
            "accent-orange.css"
    };

    private AppStyleManager() {
    }

    public static void applyCurrentStyle(Scene scene) {
        if (scene == null) return;
        applyStylesToCollection(scene.getStylesheets());
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
            return;
        }
        applyCurrentStyle(scene);
    }

    public static void applyCurrentStyleToOpenWindows() {
        for (Window window : Window.getWindows()) {
            if (window instanceof javafx.stage.Stage stage && stage.getScene() != null) {
                applyCurrentStyle(stage.getScene());
            }
        }
    }

    public static void clearCurrentStyle(Scene scene) {
        if (scene == null) return;
        removeManagedStylesheets(scene);
        if (scene.getRoot() != null) {
            removeThemeClasses(scene.getRoot());
        }
    }

    private static void applyStylesToCollection(javafx.collections.ObservableList<String> stylesheets) {
        removeManagedStylesheets(stylesheets);

        SettingsService settings = SettingsService.getInstance();
        String theme = safeLower(settings.getTheme());
        String colorName = safeLower(settings.getPrimaryColor());

        String themeFile = theme.contains("dark") ? "theme-dark.css" : "theme-light.css";
        String accentFile = resolveAccentFile(colorName);

        addStylesheet(stylesheets, themeFile);
        addStylesheet(stylesheets, accentFile);
        addStylesheet(stylesheets, "global-tokens.css");
    }

    public static void updateRootStyleClass(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        updateRootStyleClass(scene.getRoot());
    }

    public static void updateRootStyleClass(Parent root) {
        if (root == null) return;
        SettingsService settings = SettingsService.getInstance();
        String theme = safeLower(settings.getTheme());
        String colorName = safeLower(settings.getPrimaryColor());

        removeThemeClasses(root);
        root.getStyleClass().add(theme.contains("dark") ? "theme-dark" : "theme-light");
        root.getStyleClass().add(resolveAccentClass(colorName));

        logger.debug("Applied style classes: {}", root.getStyleClass());
    }

    private static void removeThemeClasses(Parent root) {
        root.getStyleClass().removeAll(
                "theme-light", "theme-dark",
                "accent-pink", "accent-purple", "accent-emerald", "accent-blue", "accent-orange"
        );
    }

    private static void removeManagedStylesheets(Scene scene) {
        removeManagedStylesheets(scene.getStylesheets());
    }

    private static void removeManagedStylesheets(javafx.collections.ObservableList<String> stylesheets) {
        stylesheets.removeIf(style -> style != null && isManagedStylesheet(style));
    }

    private static boolean isManagedStylesheet(String style) {
        for (String file : MANAGED_STYLESHEETS) {
            if (style.contains(file)) return true;
        }
        return false;
    }

    private static String resolveAccentFile(String colorName) {
        return resolveAccentClass(colorName).replace("accent-", "accent-") + ".css";
    }

    private static String resolveAccentClass(String colorName) {
        if (colorName.contains("purple")) return "accent-purple";
        if (colorName.contains("emerald") || colorName.contains("green")) return "accent-emerald";
        if (colorName.contains("blue")) return "accent-blue";
        if (colorName.contains("orange")) return "accent-orange";
        return "accent-pink";
    }

    private static void addStylesheet(javafx.collections.ObservableList<String> stylesheets, String fileName) {
        try {
            URL url = AppStyleManager.class.getResource("/com/auction/client/view/" + fileName);
            if (url == null) {
                logger.warn("Stylesheet missing: {}", fileName);
                return;
            }
            String externalForm = url.toExternalForm();
            if (!stylesheets.contains(externalForm)) {
                stylesheets.add(externalForm);
            }
        } catch (Exception e) {
            logger.warn("Error loading stylesheet {}: {}", fileName, e.getMessage());
        }
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
