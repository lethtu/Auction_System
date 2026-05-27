package com.auction.client.service;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class AppStyleManagerStyleApplicationTest {
    private SettingsService settings;
    private String originalTheme;
    private String originalPrimaryColor;

    @BeforeEach
    public void setUp() {
        settings = SettingsService.getInstance();
        originalTheme = settings.getTheme();
        originalPrimaryColor = settings.getPrimaryColor();
    }

    @AfterEach
    public void tearDown() {
        settings.setTheme(originalTheme);
        settings.setPrimaryColor(originalPrimaryColor);
        settings.flush();
    }

    @Test
    public void applyCurrentStyle_sceneReplacesOldThemeAndAccentButKeepsCustomStylesheet() {
        settings.setTheme("Light");
        settings.setPrimaryColor("Rose Pink (Default)");

        StackPane root = new StackPane();
        root.getStyleClass().addAll("theme-dark", "accent-blue", "custom-root-class");
        Scene scene = new Scene(root);

        String customSceneCss = "file:/custom/scene-extra.css";
        String customRootCss = "file:/custom/root-extra.css";
        scene.getStylesheets().addAll(
                "file:/old/theme-dark.css",
                "file:/old/accent-blue.css",
                customSceneCss);
        root.getStylesheets().addAll(
                "file:/old/accent-orange.css",
                customRootCss);

        AppStyleManager.applyCurrentStyle(scene);

        assertStylesheetPresent(scene.getStylesheets(), "theme-light.css");
        assertStylesheetPresent(scene.getStylesheets(), "accent-pink.css");
        assertStylesheetAbsent(scene.getStylesheets(), "theme-dark.css");
        assertStylesheetAbsent(scene.getStylesheets(), "accent-blue.css");
        assertTrue(scene.getStylesheets().contains(customSceneCss));

        assertStylesheetPresent(root.getStylesheets(), "theme-light.css");
        assertStylesheetPresent(root.getStylesheets(), "accent-pink.css");
        assertStylesheetAbsent(root.getStylesheets(), "accent-orange.css");
        assertTrue(root.getStylesheets().contains(customRootCss));

        assertTrue(root.getStyleClass().contains("theme-light"));
        assertTrue(root.getStyleClass().contains("accent-pink"));
        assertTrue(root.getStyleClass().contains("custom-root-class"));
        assertFalse(root.getStyleClass().contains("theme-dark"));
        assertFalse(root.getStyleClass().contains("accent-blue"));
    }

    @Test
    public void applyCurrentStyle_parentWithSceneAppliesDarkThemeAndEmeraldAccent() {
        settings.setTheme("Dark Mode");
        settings.setPrimaryColor("Green Emerald");

        StackPane root = new StackPane();
        Scene scene = new Scene(root);

        AppStyleManager.applyCurrentStyle(root);

        assertStylesheetPresent(scene.getStylesheets(), "theme-dark.css");
        assertStylesheetPresent(scene.getStylesheets(), "accent-emerald.css");
        assertStylesheetPresent(root.getStylesheets(), "theme-dark.css");
        assertStylesheetPresent(root.getStylesheets(), "accent-emerald.css");
        assertTrue(root.getStyleClass().contains("theme-dark"));
        assertTrue(root.getStyleClass().contains("accent-emerald"));
        assertFalse(root.getStyleClass().contains("theme-light"));
    }

    @Test
    public void updateRootStyleClassSelectsExpectedAccentForConfiguredColor() {
        assertAccentClass("Purple", "accent-purple");
        assertAccentClass("emerald", "accent-emerald");
        assertAccentClass("green", "accent-emerald");
        assertAccentClass("blue", "accent-blue");
        assertAccentClass("orange", "accent-orange");
        assertAccentClass("unknown", "accent-pink");
    }

    @Test
    public void publicStyleMethodsIgnoreNullInputs() {
        assertDoesNotThrow(() -> AppStyleManager.applyCurrentStyle((Scene) null));
        assertDoesNotThrow(() -> AppStyleManager.applyCurrentStyle((javafx.scene.Parent) null));
        assertDoesNotThrow(() -> AppStyleManager.updateRootStyleClass((Scene) null));
        assertDoesNotThrow(() -> AppStyleManager.updateRootStyleClass((javafx.scene.Parent) null));
    }

    private void assertAccentClass(String colorName, String expectedClass) {
        settings.setTheme("Light");
        settings.setPrimaryColor(colorName);

        StackPane root = new StackPane();
        root.getStyleClass().addAll(
                "theme-dark",
                "accent-pink",
                "accent-purple",
                "accent-emerald",
                "accent-blue",
                "accent-orange",
                "keep-me");

        AppStyleManager.updateRootStyleClass(root);

        assertTrue(root.getStyleClass().contains("theme-light"));
        assertTrue(root.getStyleClass().contains(expectedClass));
        assertTrue(root.getStyleClass().contains("keep-me"));
        assertFalse(root.getStyleClass().contains("theme-dark"));

        for (String accentClass : new String[]{"accent-pink", "accent-purple", "accent-emerald", "accent-blue", "accent-orange"}) {
            if (!accentClass.equals(expectedClass)) {
                assertFalse(root.getStyleClass().contains(accentClass), "Unexpected accent class: " + accentClass);
            }
        }
    }

    private static void assertStylesheetPresent(ObservableList<String> stylesheets, String fileName) {
        assertTrue(stylesheets.stream().anyMatch(path -> path.contains(fileName)),
                "Expected stylesheet containing " + fileName + " in " + stylesheets);
    }

    private static void assertStylesheetAbsent(ObservableList<String> stylesheets, String fileName) {
        assertFalse(stylesheets.stream().anyMatch(path -> path.contains(fileName)),
                "Did not expect stylesheet containing " + fileName + " in " + stylesheets);
    }
}
