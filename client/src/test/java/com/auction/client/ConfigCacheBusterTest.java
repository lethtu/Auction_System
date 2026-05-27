package com.auction.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigCacheBusterTest {

    @BeforeEach
    void setUp() {
        Config.imageCacheBuster = 12345L;
    }

    @Test
    void applyCacheBuster_blankOrNull_returnsOriginalValue() {
        assertNull(Config.applyCacheBuster(null));
        assertEquals("", Config.applyCacheBuster(""));
        assertEquals("   ", Config.applyCacheBuster("   "));
    }

    @Test
    void applyCacheBuster_regularUrlWithoutQuery_addsTimestampQuery() {
        assertEquals(
                "https://example.com/image.png?t=12345",
                Config.applyCacheBuster("https://example.com/image.png")
        );
    }

    @Test
    void applyCacheBuster_regularUrlWithExistingQuery_appendsTimestampWithAmpersand() {
        assertEquals(
                "https://example.com/image.png?size=large&t=12345",
                Config.applyCacheBuster("https://example.com/image.png?size=large")
        );
    }

    @Test
    void applyCacheBuster_cloudinaryImageUrl_replacesExistingVersionSegment() {
        String input = "https://res.cloudinary.com/demo/image/upload/v999999/folder/watch.png";

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/v12345/folder/watch.png",
                Config.applyCacheBuster(input)
        );
    }

    @Test
    void applyCacheBuster_cloudinaryRawUrl_injectsVersionSegment() {
        String input = "https://res.cloudinary.com/demo/raw/upload/documents/file.glb";

        assertEquals(
                "https://res.cloudinary.com/demo/raw/upload/v12345/documents/file.glb",
                Config.applyCacheBuster(input)
        );
    }
}