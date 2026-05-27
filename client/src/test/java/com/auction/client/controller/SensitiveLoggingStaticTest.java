package com.auction.client.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SensitiveLoggingStaticTest {

    @Test
    void signupControllerDoesNotLogRawRequestBody() throws Exception {
        Path source = Path.of("src/main/java/com/auction/client/controller/SignUpController.java");
        String content = Files.readString(source);

        assertFalse(content.contains("logger.info(jsonBody);"),
                "SignUpController must not log the raw signup JSON body because it contains the password.");
        assertFalse(content.contains("logger.debug(jsonBody);"),
                "SignUpController must not log the raw signup JSON body because it contains the password.");
        assertFalse(content.contains("logger.warn(jsonBody);"),
                "SignUpController must not log the raw signup JSON body because it contains the password.");
    }
}
