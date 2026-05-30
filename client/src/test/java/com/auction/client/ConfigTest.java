package com.auction.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTest {

    @Test
    void config_usesApiUrlOnlyForServerLocation() {
        assertEquals("https://server-bai-tap-lon.shin25112007.workers.dev", Config.API_URL);
    }

    @Test
    void normalizeBaseUrl_trimsWhitespaceAndTrailingSlashes() {
        assertEquals(
                "http://localhost:8080",
                Config.normalizeBaseUrl("  http://localhost:8080///  ")
        );
    }

    @Test
    void normalizeBaseUrl_usesDefaultWhenBlank() {
        assertEquals(Config.API_URL, Config.normalizeBaseUrl("   "));
    }
}
