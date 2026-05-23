package com.auction.client.service;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class AppStyleManager {
    private static final Logger logger = LoggerFactory.getLogger(AppStyleManager.class);
    
    public static void applyCurrentStyle(Scene scene) {
        if (scene == null) return;
        applyStylesToCollection(scene.getStylesheets());
        if (scene.getRoot() != null) {
            applyStylesToCollection(scene.getRoot().getStylesheets());
        }
        updateRootStyleClass(scene);
    }

    public static void applyCurrentStyle(Parent root) {
        if (root == null) return;
        applyStylesToCollection(root.getStylesheets());
        Scene scene = root.getScene();
        if (scene == null) {
            Platform.runLater(() -> {
                if (root.getScene() != null) {
                    applyStylesToCollection(root.getScene().getStylesheets());
                    applyStylesToCollection(root.getScene().getRoot().getStylesheets());
                    updateRootStyleClass(root.getScene());
                }
            });
        } else {
            applyStylesToCollection(scene.getStylesheets());
            if (scene.getRoot() != null) {
                applyStylesToCollection(scene.getRoot().getStylesheets());
            }
            updateRootStyleClass(scene);
        }
    }

    private static void applyStylesToCollection(javafx.collections.ObservableList<String> stylesheets) {
        // 1. Remove old theme/accent stylesheets
        stylesheets.removeIf(s -> s.contains("theme-dark.css") || s.contains("theme-light.css")
                || s.contains("accent-pink.css") || s.contains("accent-purple.css")
                || s.contains("accent-emerald.css") || s.contains("accent-blue.css")
                || s.contains("accent-orange.css"));

        SettingsService settings = SettingsService.getInstance();
        String theme = settings.getTheme().toLowerCase();
        String colorName = settings.getPrimaryColor().toLowerCase();

        // 2. Add Theme and set Root Class
        String themeFile = "theme-light.css";
        String themeClass = "theme-light";
        if (theme.contains("dark")) {
            themeFile = "theme-dark.css";
            themeClass = "theme-dark";
        }
        addStylesheet(stylesheets, themeFile);

        // 3. Add Accent
        String accentFile = "accent-pink.css";
        if (colorName.contains("purple")) {
            accentFile = "accent-purple.css";
        } else if (colorName.contains("emerald") || colorName.contains("green")) {
            accentFile = "accent-emerald.css";
        } else if (colorName.contains("blue")) {
            accentFile = "accent-blue.css";
        } else if (colorName.contains("orange")) {
            accentFile = "accent-orange.css";
        }
        addStylesheet(stylesheets, accentFile);
        
        logger.debug("Current stylesheets: {}", stylesheets);
    }

    public static void updateRootStyleClass(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        updateRootStyleClass(scene.getRoot());
    }

        public static void updateRootStyleClass(Parent root) {
        if (root == null) return;
        SettingsService settings = SettingsService.getInstance();
        String theme = settings.getTheme().toLowerCase();
        String colorName = settings.getPrimaryColor().toLowerCase();
        
        root.getStyleClass().remove("theme-light");
        root.getStyleClass().remove("theme-dark");
        root.getStyleClass().remove("accent-pink");
        root.getStyleClass().remove("accent-purple");
        root.getStyleClass().remove("accent-emerald");
        root.getStyleClass().remove("accent-blue");
        root.getStyleClass().remove("accent-orange");
        
        if (theme.contains("dark")) {
            root.getStyleClass().add("theme-dark");
        } else {
            root.getStyleClass().add("theme-light");
        }
        
        String accentClass = "accent-pink";
        if (colorName.contains("purple")) {
            accentClass = "accent-purple";
        } else if (colorName.contains("emerald") || colorName.contains("green")) {
            accentClass = "accent-emerald";
        } else if (colorName.contains("blue")) {
            accentClass = "accent-blue";
        } else if (colorName.contains("orange")) {
            accentClass = "accent-orange";
        }
        root.getStyleClass().add(accentClass);
        
        logger.debug("Current Root Style Classes: {}", root.getStyleClass());
    }

    private static void addStylesheet(javafx.collections.ObservableList<String> stylesheets, String fileName) {
        try {
            URL url = AppStyleManager.class.getResource("/com/auction/client/view/" + fileName);
            if (url != null) {
                String externalForm = url.toExternalForm();
                if (!stylesheets.contains(externalForm)) {
                    stylesheets.add(externalForm);
                }
            } else {
                logger.warn("Stylesheet missing: {}", fileName);
            }
        } catch (Exception e) {
            logger.warn("Error loading stylesheet {}: {}", fileName, e.getMessage());
        }
    }

    public static String getAccentColorHex() {
        SettingsService settings = SettingsService.getInstance();
        String colorName = settings.getPrimaryColor().toLowerCase();
        if (colorName.contains("purple")) {
            return "#8b5cf6";
        } else if (colorName.contains("emerald") || colorName.contains("green")) {
            return "#10b981";
        } else if (colorName.contains("blue")) {
            return "#3b82f6";
        } else if (colorName.contains("orange")) {
            return "#f97316";
        }
        return "#e040a0"; // Default Pink
    }
}
