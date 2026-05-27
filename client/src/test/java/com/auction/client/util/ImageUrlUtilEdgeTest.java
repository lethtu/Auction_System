package com.auction.client.util;

import com.auction.client.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageUrlUtilEdgeTest {

    @BeforeEach
    void setUp() {
        Config.imageCacheBuster = 24680L;
    }

    @Test
    void buildImageUrl_plainFileName_buildsApiImageUrl() {
        assertEquals(
                Config.API_URL + "/api/files/images/watch.png?t=24680",
                ImageUrlUtil.buildImageUrl("watch.png")
        );
    }

    @Test
    void buildImageUrl_leadingSlashesAndImagesPrefix_areNormalized() {
        assertEquals(
                Config.API_URL + "/api/files/images/products/watch.png?t=24680",
                ImageUrlUtil.buildImageUrl("///images/products/watch.png")
        );
    }

    @Test
    void buildImageUrl_existingApiPathWithoutHost_rebuildsAgainstConfiguredApiUrl() {
        assertEquals(
                Config.API_URL + "/api/files/images/products/watch.png?t=24680",
                ImageUrlUtil.buildImageUrl("/api/files/images/products/watch.png")
        );
    }

    @Test
    void buildImageUrl_externalUrlWithQuery_keepsQueryAndAppendsCacheBuster() {
        assertEquals(
                "https://cdn.example.com/watch.png?quality=80&t=24680",
                ImageUrlUtil.buildImageUrl("https://cdn.example.com/watch.png?quality=80")
        );
    }

    @Test
    void buildImageUrl_windowsServerUploadPath_normalizesNestedPath() {
        assertEquals(
                Config.API_URL + "/api/files/images/products/watch.png?t=24680",
                ImageUrlUtil.buildImageUrl("server\\upload\\images\\products\\watch.png")
        );
    }
}