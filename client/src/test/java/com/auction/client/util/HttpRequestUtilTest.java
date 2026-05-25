package com.auction.client.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestUtilTest {

    @Test
    void encode_null_returnsEmptyString() {
        assertEquals("", HttpRequestUtil.encode(null));
    }

    @Test
    void encode_vietnameseAndSpaces_returnsUrlEncodedText() {
        assertEquals("Sai+th%C3%B4ng+tin+%C4%91%E1%BA%A5u+gi%C3%A1", HttpRequestUtil.encode("Sai thông tin đấu giá"));
    }

    @Test
    void testDetectContentTypeFromFileName() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod("detectContentTypeFromFileName", String.class);
        method.setAccessible(true);

        assertEquals("image/png", method.invoke(null, "photo.png"));
        assertEquals("image/jpeg", method.invoke(null, "photo.jpg"));
        assertEquals("image/jpeg", method.invoke(null, "photo.jpeg"));
        assertEquals("image/gif", method.invoke(null, "photo.gif"));
        assertEquals("image/webp", method.invoke(null, "photo.webp"));
        assertEquals("image/bmp", method.invoke(null, "photo.bmp"));
        assertEquals("application/octet-stream", method.invoke(null, "document.pdf"));
        assertEquals("application/octet-stream", method.invoke(null, (String) null));
    }

    @Test
    void testSafeFileName() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod("safeFileName", String.class);
        method.setAccessible(true);

        assertEquals("image.png", method.invoke(null, (String) null));
        assertEquals("image.png", method.invoke(null, "   "));
        assertEquals("my_cool_image.png", method.invoke(null, "my/cool\\image.png"));
        assertEquals("my_image_.png", method.invoke(null, "my\"image\".png"));
    }

    @Test
    void testBuildMultipartHeader() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod("buildMultipartHeader", String.class, String.class, String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "boundary123", "file", "avatar.png", "image/png");
        assertTrue(result.contains("--boundary123"));
        assertTrue(result.contains("name=\"file\""));
        assertTrue(result.contains("filename=\"avatar.png\""));
        assertTrue(result.contains("Content-Type: image/png"));
    }

    @Test
    void testBuildMultipartEnding() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod("buildMultipartEnding", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "boundary123");
        assertEquals("\r\n--boundary123--\r\n", result);
    }

    @Test
    void testBuildMultipartBody() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod("buildMultipartBody", String.class, String.class, String.class, String.class, byte[].class);
        method.setAccessible(true);

        byte[] fileBytes = "hello file content".getBytes(StandardCharsets.UTF_8);
        byte[] body = (byte[]) method.invoke(null, "boundary123", "file", "test.txt", "text/plain", fileBytes);

        assertNotNull(body);
        String bodyString = new String(body, StandardCharsets.UTF_8);
        assertTrue(bodyString.contains("boundary123"));
        assertTrue(bodyString.contains("test.txt"));
        assertTrue(bodyString.contains("hello file content"));
    }

    @Test
    void testHasText() throws Exception {
        Method method = HttpRequestUtil.class.getDeclaredMethod("hasText", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(null, "text"));
        assertFalse((Boolean) method.invoke(null, (String) null));
        assertFalse((Boolean) method.invoke(null, "   "));
    }
}
