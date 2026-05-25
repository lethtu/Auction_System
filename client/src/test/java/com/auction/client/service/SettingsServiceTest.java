package com.auction.client.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsServiceTest {
    private SettingsService settingsService;

    // Backup variables to avoid dirtying local preferences
    private boolean originalNotifications;
    private boolean originalOutbid;
    private boolean originalEndingSoon;
    private boolean originalAuctionResult;
    private boolean originalSound;
    private double originalSoundVolume;
    private String originalTheme;
    private String originalPrimaryColor;
    private boolean originalConfirmBeforeBid;
    private long originalHighBidWarningThreshold;
    private boolean originalShowEnded;
    private boolean originalSortActive;
    private boolean originalShowCountdown;

    @BeforeEach
    public void setUp() {
        settingsService = SettingsService.getInstance();
        assertNotNull(settingsService.getPrefs());

        // Backup
        originalNotifications = settingsService.isNotificationsEnabled();
        originalOutbid = settingsService.isOutbidNotificationEnabled();
        originalEndingSoon = settingsService.isEndingSoonNotificationEnabled();
        originalAuctionResult = settingsService.isAuctionResultNotificationEnabled();
        originalSound = settingsService.isSoundEnabled();
        originalSoundVolume = settingsService.getSoundVolume();
        originalTheme = settingsService.getTheme();
        originalPrimaryColor = settingsService.getPrimaryColor();
        originalConfirmBeforeBid = settingsService.isConfirmBeforeBid();
        originalHighBidWarningThreshold = settingsService.getHighBidWarningThreshold();
        originalShowEnded = settingsService.isShowEndedAuctions();
        originalSortActive = settingsService.isSortActiveFirst();
        originalShowCountdown = settingsService.isShowCountdownTimer();
    }

    @AfterEach
    public void tearDown() {
        // Restore
        settingsService.setNotificationsEnabled(originalNotifications);
        settingsService.setOutbidNotificationEnabled(originalOutbid);
        settingsService.setEndingSoonNotificationEnabled(originalEndingSoon);
        settingsService.setAuctionResultNotificationEnabled(originalAuctionResult);
        settingsService.setSoundEnabled(originalSound);
        settingsService.setSoundVolume(originalSoundVolume);
        settingsService.setTheme(originalTheme);
        settingsService.setPrimaryColor(originalPrimaryColor);
        settingsService.setConfirmBeforeBid(originalConfirmBeforeBid);
        settingsService.setHighBidWarningThreshold(originalHighBidWarningThreshold);
        settingsService.setShowEndedAuctions(originalShowEnded);
        settingsService.setSortActiveFirst(originalSortActive);
        settingsService.setShowCountdownTimer(originalShowCountdown);
        settingsService.flush();
    }

    @Test
    public void testNotificationsPreferences() {
        settingsService.setNotificationsEnabled(false);
        assertFalse(settingsService.isNotificationsEnabled());
        settingsService.setNotificationsEnabled(true);
        assertTrue(settingsService.isNotificationsEnabled());

        settingsService.setOutbidNotificationEnabled(false);
        assertFalse(settingsService.isOutbidNotificationEnabled());
        settingsService.setOutbidNotificationEnabled(true);
        assertTrue(settingsService.isOutbidNotificationEnabled());

        settingsService.setEndingSoonNotificationEnabled(false);
        assertFalse(settingsService.isEndingSoonNotificationEnabled());
        settingsService.setEndingSoonNotificationEnabled(true);
        assertTrue(settingsService.isEndingSoonNotificationEnabled());

        settingsService.setAuctionResultNotificationEnabled(false);
        assertFalse(settingsService.isAuctionResultNotificationEnabled());
        settingsService.setAuctionResultNotificationEnabled(true);
        assertTrue(settingsService.isAuctionResultNotificationEnabled());
    }

    @Test
    public void testSoundPreferences() {
        settingsService.setSoundEnabled(false);
        assertFalse(settingsService.isSoundEnabled());
        settingsService.setSoundEnabled(true);
        assertTrue(settingsService.isSoundEnabled());

        settingsService.setSoundVolume(0.25);
        assertEquals(0.25, settingsService.getSoundVolume(), 0.01);

        // Test volume clamping
        settingsService.setSoundVolume(-1.5);
        assertEquals(0.0, settingsService.getSoundVolume(), 0.01);
        settingsService.setSoundVolume(2.5);
        assertEquals(1.0, settingsService.getSoundVolume(), 0.01);
    }

    @Test
    public void testAppearancePreferences() {
        settingsService.setTheme("Dark");
        assertEquals("Dark", settingsService.getTheme());

        settingsService.setPrimaryColor("Teal");
        assertEquals("Teal", settingsService.getPrimaryColor());
    }

    @Test
    public void testAuctionPreferences() {
        settingsService.setConfirmBeforeBid(false);
        assertFalse(settingsService.isConfirmBeforeBid());
        settingsService.setConfirmBeforeBid(true);
        assertTrue(settingsService.isConfirmBeforeBid());

        settingsService.setHighBidWarningThreshold(500000L);
        assertEquals(500000L, settingsService.getHighBidWarningThreshold());

        settingsService.setHighBidWarningThreshold(-100L);
        assertEquals(0L, settingsService.getHighBidWarningThreshold());
    }

    @Test
    public void testDashboardDisplayPreferences() {
        settingsService.setShowEndedAuctions(true);
        assertTrue(settingsService.isShowEndedAuctions());

        settingsService.setSortActiveFirst(false);
        assertFalse(settingsService.isSortActiveFirst());

        settingsService.setShowCountdownTimer(false);
        assertFalse(settingsService.isShowCountdownTimer());

        settingsService.resetDashboardDisplayDefaults();
        assertFalse(settingsService.isShowEndedAuctions());
        assertTrue(settingsService.isSortActiveFirst());
        assertTrue(settingsService.isShowCountdownTimer());
    }
}
