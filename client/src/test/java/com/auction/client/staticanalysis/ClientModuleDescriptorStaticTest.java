package com.auction.client.staticanalysis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientModuleDescriptorStaticTest {

    private static final Path MODULE_INFO = Path.of("src/main/java/module-info.java");

    @Test
    void moduleInfo_opensPackagesNeededByJavaFxReflection() throws Exception {
        String moduleInfo = compactModuleInfo();

        assertContains(moduleInfo, "opens com.auction.client.controller to javafx.fxml;",
                "FXML controllers must stay open to javafx.fxml, otherwise FXMLLoader can fail at runtime.");
        assertContains(moduleInfo, "opens com.auction.client.model to javafx.base;",
                "JavaFX property/table reflection needs model package access.");
    }

    @Test
    void moduleInfo_requiresJavaFxModulesUsedByTheClient() throws Exception {
        String moduleInfo = compactModuleInfo();

        assertContains(moduleInfo, "requires transitive javafx.controls;",
                "Client UI depends on JavaFX controls.");
        assertContains(moduleInfo, "requires transitive javafx.fxml;",
                "FXML loading depends on javafx.fxml.");
        assertContains(moduleInfo, "requires transitive javafx.graphics;",
                "Scene, Stage, Node, and image APIs depend on javafx.graphics.");
        assertContains(moduleInfo, "requires transitive javafx.media;",
                "SoundManager depends on javafx.media.");
    }

    @Test
    void moduleInfo_exportsClientPackagesUsedAcrossTheApp() throws Exception {
        String moduleInfo = compactModuleInfo();

        assertContains(moduleInfo, "exports com.auction.client;",
                "Main client package should remain exported.");
        assertContains(moduleInfo, "exports com.auction.client.controller;",
                "Controller package should remain exported for app navigation/tests.");
        assertContains(moduleInfo, "exports com.auction.client.model;",
                "Model package should remain exported for shared client models.");
        assertContains(moduleInfo, "exports com.auction.client.dto;",
                "DTO package should remain exported for API contract objects.");
    }

    private static String compactModuleInfo() throws IOException {
        String text = Files.readString(MODULE_INFO, StandardCharsets.UTF_8);
        return text.replaceAll("//.*", " ")
                .replaceAll("/\\*.*?\\*/", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void assertContains(String moduleInfo, String expectedDirective, String message) {
        assertTrue(moduleInfo.contains(expectedDirective), message + " Missing directive: " + expectedDirective);
    }
}