package com.auction.client.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class CacheManagerTest {

    @Test
    public void testGetMd5() {
        String input = "testString";
        String hash = CacheManager.getMd5(input);
        assertNotNull(hash);
        assertEquals(32, hash.length());

        // Same input should yield same output
        assertEquals(hash, CacheManager.getMd5(input));

        // Different input should yield different output
        assertNotEquals(hash, CacheManager.getMd5("otherString"));
    }

    @Test
    public void testGetMd5_nullOrEmpty() {
        // MD5 of null or empty shouldn't throw exception
        assertNotNull(CacheManager.getMd5(""));
    }

    @Test
    public void testStableCacheKeyUrl() throws Exception {
        // Test stableCacheKeyUrl method (using reflection if it is private or directly if package-private/public)
        Method method = CacheManager.class.getDeclaredMethod("stableCacheKeyUrl", String.class);
        method.setAccessible(true);

        // Test normal URL
        assertEquals("http://example.com/image.png", method.invoke(null, "http://example.com/image.png"));

        // Test URL with version
        assertEquals("http://example.com/image.png", method.invoke(null, "http://example.com/v12345/image.png"));

        // Test URL with query parameters
        assertEquals("http://example.com/image.png?id=5", method.invoke(null, "http://example.com/image.png?id=5&t=9999"));
        assertEquals("http://example.com/image.png?id=5", method.invoke(null, "http://example.com/image.png?t=9999&id=5"));

        // Test null/empty URL
        assertEquals("", method.invoke(null, (String) null));
        assertEquals("", method.invoke(null, "   "));
    }

    @Test
    public void testGetExtension() throws Exception {
        Method method = CacheManager.class.getDeclaredMethod("getExtension", String.class, String.class);
        method.setAccessible(true);

        assertEquals("png", method.invoke(null, "http://example.com/image.png", "jpg"));
        assertEquals("glb", method.invoke(null, "http://example.com/model.glb?token=xyz", "png"));
        assertEquals("png", method.invoke(null, "http://example.com/noextension", "png"));
        assertEquals("png", method.invoke(null, null, "png"));
    }
}
