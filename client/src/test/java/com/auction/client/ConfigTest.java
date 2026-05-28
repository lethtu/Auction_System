package com.auction.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTest {

    @Test
    void normalizeBaseUrl_trimsWhitespaceAndTrailingSlashes() {
        assertEquals(
                "http://localhost:8080",
                Config.normalizeBaseUrl("  http://localhost:8080///  ")
        );
    }

    @Test
    void normalizeBaseUrl_usesDefaultWhenBlank() {
        assertEquals(Config.DEFAULT_API_URL, Config.normalizeBaseUrl("   "));
    }

    @Test
    void readPositiveIntConfig_fallsBackWhenSystemPropertyIsInvalid() {
        String propertyName = "auction.test.invalid.port";
        System.setProperty(propertyName, "invalid");
        try {
            assertEquals(
                    8081,
                    Config.readPositiveIntConfig(propertyName, "AUCTION_TEST_INVALID_PORT", 8081)
            );
        } finally {
            System.clearProperty(propertyName);
        }
    }

    @Test
    void readPositiveIntConfig_readsPositiveSystemProperty() {
        String propertyName = "auction.test.valid.port";
        System.setProperty(propertyName, "9090");
        try {
            assertEquals(
                    9090,
                    Config.readPositiveIntConfig(propertyName, "AUCTION_TEST_VALID_PORT", 8081)
            );
        } finally {
            System.clearProperty(propertyName);
        }
    }
}
