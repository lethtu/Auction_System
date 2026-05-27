package com.auction.client.staticanalysis;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoSystemErrLoggingStaticTest {

    @Test
    void clientSource_shouldNotUseSystemErrPrint() throws Exception {
        Path sourceRoot = Paths.get("src/main/java");
        if (!Files.exists(sourceRoot)) {
            return;
        }

        List<String> offenders;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("System.err.print");
                        } catch (Exception e) {
                            return true;
                        }
                    })
                    .map(sourceRoot::relativize)
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.toList());
        }

        assertTrue(offenders.isEmpty(),
                "Do not use System.err.print in client source. Use logger instead: " + offenders);
    }
}
