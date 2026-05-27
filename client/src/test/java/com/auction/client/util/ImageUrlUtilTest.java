package com.auction.client.util;

import com.auction.client.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageUrlUtilTest {

    @BeforeEach
    void setUp() {
        Config.imageCacheBuster = 12345L;
    }

    @Test
    void buildImageUrl_blankInput_returnsEmptyString() {
        assertEquals("", ImageUrlUtil.buildImageUrl(null));
        assertEquals("", ImageUrlUtil.buildImageUrl("   "));
    }

    @Test
    void buildImageUrl_externalHttpUrl_keepsExternalUrlAndAddsCacheBuster() {
        assertEquals(
                "https://example.com/watch.png?t=12345",
                ImageUrlUtil.buildImageUrl("https://example.com/watch.png")
        );
    }

    @Test
    void buildImageUrl_localUploadPath_normalizesToApiImageEndpoint() {
        assertEquals(
                Config.API_URL + "/api/files/images/watch.png?t=12345",
                ImageUrlUtil.buildImageUrl("server/upload/images/watch.png")
        );
    }

    @Test
    void buildImageUrl_windowsStyleUploadPath_normalizesSlashesAndPrefix() {
        assertEquals(
                Config.API_URL + "/api/files/images/watch.png?t=12345",
                ImageUrlUtil.buildImageUrl("\\upload\\images\\watch.png")
        );
    }

    @Test
    void buildImageUrl_existingApiImageUrl_rebuildsAgainstConfiguredApiUrl() {
        assertEquals(
                Config.API_URL + "/api/files/images/watch.png?t=12345",
                ImageUrlUtil.buildImageUrl("http://localhost:8080/api/files/images/watch.png")
        );
    }
}