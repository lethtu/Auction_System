package com.auction.client.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GltfImporterErrorLoggingStaticTest {

    @Test
    void gltfImporter_shouldLogDebugFileWriteFailureInsteadOfIgnoringIt() throws Exception {
        Path source = Path.of(
                "src", "main", "java", "com", "auction", "client", "util", "GltfImporterJFX.java"
        );
        String text = Files.readString(source, StandardCharsets.UTF_8);

        assertFalse(text.contains("catch (Exception ignored) {}"),
                "GltfImporterJFX should not silently ignore failures while writing GLB debug errors.");
        assertTrue(text.contains("Could not write GLB debug error file"),
                "GltfImporterJFX should log when writing the GLB debug error file fails.");
    }
}
