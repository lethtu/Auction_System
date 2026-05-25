package com.auction.client.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppStyleManagerTest {
    private String originalColor;

    @BeforeEach
    public void setUp() {
        originalColor = SettingsService.getInstance().getPrimaryColor();
    }

    @AfterEach
    public void tearDown() {
        SettingsService.getInstance().setPrimaryColor(originalColor);
    }

    @Test
    public void testGetAccentColorHex() {
        SettingsService settings = SettingsService.getInstance();

        settings.setPrimaryColor("Purple");
        assertEquals("#8b5cf6", AppStyleManager.getAccentColorHex());

        settings.setPrimaryColor("emerald");
        assertEquals("#10b981", AppStyleManager.getAccentColorHex());

        settings.setPrimaryColor("blue");
        assertEquals("#3b82f6", AppStyleManager.getAccentColorHex());

        settings.setPrimaryColor("orange");
        assertEquals("#f97316", AppStyleManager.getAccentColorHex());

        settings.setPrimaryColor("pink");
        assertEquals("#e040a0", AppStyleManager.getAccentColorHex());
    }
}
