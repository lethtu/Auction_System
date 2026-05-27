package com.auction.client.staticanalysis;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientNavigationResourceStaticTest {

    private static final Path JAVA_ROOT = Paths.get("src/main/java");
    private static final Path RESOURCES_ROOT = Paths.get("src/main/resources");
    private static final Path VIEW_ROOT = JAVA_ROOT.resolve("com/auction/client/view");

    private static final Pattern SCENE_SWITCHER_FXML_TARGET = Pattern.compile(
            "SceneSwitcher\\.(?:switchScene|Switch)\\s*\\([^;]*?\\\"([^\\\"]+\\.fxml)\\\"",
            Pattern.DOTALL);

    private static final Pattern ABSOLUTE_CLIENT_RESOURCE = Pattern.compile(
            "\\\"(/com/auction/client/[^\\\"]+\\.(?:css|fxml|ttf|png|jpg|jpeg|webp|wav))\\\"",
            Pattern.CASE_INSENSITIVE);

    @Test
    void sceneSwitcherTargets_shouldPointToExistingFxmlFiles() throws Exception {
        List<String> missing = new ArrayList<>();
        Set<String> discoveredTargets = new LinkedHashSet<>();

        for (Path sourceFile : javaSourceFiles()) {
            String source = Files.readString(sourceFile);
            Matcher matcher = SCENE_SWITCHER_FXML_TARGET.matcher(source);
            while (matcher.find()) {
                String fxmlFile = matcher.group(1).trim();
                if (fxmlFile.contains(" ") || fxmlFile.contains("/") || fxmlFile.contains("\\")) {
                    missing.add(relative(sourceFile) + " has suspicious SceneSwitcher target: " + fxmlFile);
                    continue;
                }

                discoveredTargets.add(fxmlFile);
                Path expectedFile = VIEW_ROOT.resolve(fxmlFile);
                if (!Files.isRegularFile(expectedFile)) {
                    missing.add(relative(sourceFile) + " -> missing FXML: " + expectedFile);
                }
            }
        }

        assertFalse(discoveredTargets.isEmpty(),
                "Expected to discover at least one SceneSwitcher FXML target in client source.");
        assertTrue(missing.isEmpty(),
                "SceneSwitcher FXML targets must exist under src/main/java/com/auction/client/view: " + missing);
    }

    @Test
    void absoluteClientResourceStrings_shouldPointToExistingFiles() throws Exception {
        List<String> missing = new ArrayList<>();
        Set<String> discoveredResources = new LinkedHashSet<>();

        for (Path sourceFile : javaSourceFiles()) {
            String source = Files.readString(sourceFile);
            Matcher matcher = ABSOLUTE_CLIENT_RESOURCE.matcher(source);
            while (matcher.find()) {
                String resourcePath = matcher.group(1);
                discoveredResources.add(resourcePath);

                Path relativeResource = Paths.get(resourcePath.substring(1));
                Path javaResource = JAVA_ROOT.resolve(relativeResource);
                Path mainResource = RESOURCES_ROOT.resolve(relativeResource);

                if (!Files.isRegularFile(javaResource) && !Files.isRegularFile(mainResource)) {
                    missing.add(relative(sourceFile) + " -> missing resource: " + resourcePath);
                }
            }
        }

        assertFalse(discoveredResources.isEmpty(),
                "Expected to discover absolute /com/auction/client/... resource strings in client source.");
        assertTrue(missing.isEmpty(),
                "Absolute client resource strings must resolve from src/main/java or src/main/resources: " + missing);
    }

    private static List<Path> javaSourceFiles() throws Exception {
        try (Stream<Path> paths = Files.walk(JAVA_ROOT)) {
            return paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toList());
        }
    }

    private static String relative(Path sourceFile) {
        return JAVA_ROOT.relativize(sourceFile).toString().replace('\\', '/');
    }
}