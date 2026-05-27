package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoPrintStackTraceStaticTest {

    @Test
    void clientSource_shouldNotUsePrintStackTrace() throws IOException {
        Path sourceRoot = Path.of("src/main/java");

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<String> offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsPrintStackTrace(path))
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.toList());

            assertTrue(
                    offenders.isEmpty(),
                    "Do not use printStackTrace() in client source. Use logger instead: " + offenders
            );
        }
    }

    private boolean containsPrintStackTrace(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).contains("printStackTrace(");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read source file: " + path, e);
        }
    }
}
