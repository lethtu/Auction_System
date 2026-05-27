package com.auction.client.util;

import com.auction.client.model.User;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestUtilGuardAndMultipartTest {

    @AfterEach
    void clearSessionToken() {
        User.setSessionToken(null);
    }

    @Test
    void publicMethodsRejectNullInputsBeforeSendingNetworkRequests() {
        assertThrows(NullPointerException.class, () -> HttpRequestUtil.get(null, "/api/test"));
        assertThrows(NullPointerException.class, () -> HttpRequestUtil.get("http://localhost", null));
        assertThrows(NullPointerException.class, () -> HttpRequestUtil.delete(null, "/api/test"));
        assertThrows(NullPointerException.class, () -> HttpRequestUtil.postNoBody(null, "/api/test"));

        assertThrows(NullPointerException.class,
                () -> HttpRequestUtil.sendJson(null, "http://localhost", "/api/test", new JSONObject()));
        assertThrows(NullPointerException.class,
                () -> HttpRequestUtil.sendJson("POST", "http://localhost", "/api/test", null));

        assertThrows(NullPointerException.class,
                () -> HttpRequestUtil.uploadImage("http://localhost", "/api/upload", null));
        assertThrows(NullPointerException.class,
                () -> HttpRequestUtil.uploadImageWithProgress("http://localhost", "/api/upload", null, ignored -> { }));
    }

    @Test
    void requestBuilderAddsAuthTokenOnlyWhenUserSessionTokenExists() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod("requestBuilder", String.class, String.class);
        method.setAccessible(true);

        User.setSessionToken(null);
        HttpRequest noTokenRequest = ((HttpRequest.Builder) method.invoke(null, "http://localhost:8080", "/api/test"))
                .GET()
                .build();
        assertTrue(noTokenRequest.headers().firstValue("X-Auth-Token").isEmpty());

        User.setSessionToken("");
        HttpRequest emptyTokenRequest = ((HttpRequest.Builder) method.invoke(null, "http://localhost:8080", "/api/test"))
                .GET()
                .build();
        assertTrue(emptyTokenRequest.headers().firstValue("X-Auth-Token").isEmpty());

        User.setSessionToken("token-123");
        HttpRequest tokenRequest = ((HttpRequest.Builder) method.invoke(null, "http://localhost:8080", "/api/test"))
                .GET()
                .build();
        assertEquals("token-123", tokenRequest.headers().firstValue("X-Auth-Token").orElseThrow());
        assertEquals("http://localhost:8080/api/test", tokenRequest.uri().toString());
    }

    @Test
    void createBoundaryUsesAuctionPrefixAndProducesDifferentValues() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod("createBoundary");
        method.setAccessible(true);

        String first = (String) method.invoke(null);
        String second = (String) method.invoke(null);

        assertTrue(first.startsWith("----AuctionBoundary"));
        assertTrue(second.startsWith("----AuctionBoundary"));
        assertNotEquals(first, second);
    }

    @Test
    void buildImageMultipartBodyIncludesSanitizedFileNameContentTypeAndFileBytes() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod(
                "buildImageMultipartBody",
                String.class,
                File.class
        );
        method.setAccessible(true);

        Path tempFile = Files.createTempFile("auction-http-upload-", ".unknown");
        try {
            Files.write(tempFile, List.of("binary-like-content"), StandardCharsets.UTF_8);

            byte[] body = (byte[]) method.invoke(null, "boundary-test", tempFile.toFile());
            String text = new String(body, StandardCharsets.UTF_8);

            assertTrue(text.startsWith("--boundary-test\r\n"));
            assertTrue(text.contains("Content-Disposition: form-data; name=\"file\"; filename=\"" + tempFile.getFileName() + "\""));
            assertTrue(text.contains("Content-Type: application/octet-stream")
                    || text.contains("Content-Type: text/plain"));
            assertTrue(text.contains("binary-like-content"));
            assertTrue(text.endsWith("\r\n--boundary-test--\r\n"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void buildImageMultipartBodyUsesImageContentTypeForPngFileName() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod(
                "buildImageMultipartBody",
                String.class,
                File.class
        );
        method.setAccessible(true);

        Path tempFile = Files.createTempFile("auction-http-upload-", ".png");
        try {
            Files.write(tempFile, new byte[] {1, 2, 3, 4});

            byte[] body = (byte[]) method.invoke(null, "boundary-png", tempFile.toFile());
            String text = new String(body, StandardCharsets.ISO_8859_1);

            assertTrue(text.contains("Content-Type: image/png"));
            assertTrue(text.contains("filename=\"" + tempFile.getFileName() + "\""));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void encode_handlesBlankAndReservedCharacters() {
        assertEquals("", HttpRequestUtil.encode(""));
        assertEquals("a%2Bb%26c%3Dd", HttpRequestUtil.encode("a+b&c=d"));
    }
}
