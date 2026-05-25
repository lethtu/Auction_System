package com.auction.client.service;

import com.auction.client.util.SettingsDialog;
import java.util.prefs.Preferences;

public class SettingsService {
    private static SettingsService instance;
    private final Preferences prefs;

    // Default Values
    private static final boolean DEFAULT_NOTIFICATIONS = true;
    private static final boolean DEFAULT_OUTBID = true;
    private static final boolean DEFAULT_ENDING_SOON = true;
    private static final boolean DEFAULT_AUCTION_RESULT = true;
    private static final boolean DEFAULT_SOUND = true;
    private static final double DEFAULT_SOUND_VOLUME = 0.6;
    private static final String DEFAULT_THEME = "Light";
    private static final String DEFAULT_ACCENT_COLOR = "Rose Pink (Default)";
    
    // Auction defaults
    private static final boolean DEFAULT_CONFIRM_BID = true;
    private static final long DEFAULT_HIGH_BID_WARNING = 1000000L;

    // Dashboard defaults
    private static final boolean DEFAULT_SHOW_ENDED = false;
    private static final boolean DEFAULT_SORT_ACTIVE_FIRST = true;
    private static final boolean DEFAULT_SHOW_COUNTDOWN = true;

    private SettingsService() {
        // Use the old node for backward compatibility
        this.prefs = Preferences.userNodeForPackage(SettingsDialog.class);
    }

    public static synchronized SettingsService getInstance() {
        if (instance == null) {
            instance = new SettingsService();
        }
        return instance;
    }

    public Preferences getPrefs() {
        return prefs;
    }

    // --- Notifications ---
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean("notifications_enabled", DEFAULT_NOTIFICATIONS);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.putBoolean("notifications_enabled", enabled);
    }

    public boolean isOutbidNotificationEnabled() {
        // use old key
        return prefs.getBoolean(SettingsDialog.KEY_OUTBID, DEFAULT_OUTBID);
    }

    public void setOutbidNotificationEnabled(boolean enabled) {
        prefs.putBoolean(SettingsDialog.KEY_OUTBID, enabled);
    }

    public boolean isEndingSoonNotificationEnabled() {
        return prefs.getBoolean("ending_soon_notification", DEFAULT_ENDING_SOON);
    }

    public void setEndingSoonNotificationEnabled(boolean enabled) {
        prefs.putBoolean("ending_soon_notification", enabled);
    }

    public boolean isAuctionResultNotificationEnabled() {
        return prefs.getBoolean("auction_result_notification", DEFAULT_AUCTION_RESULT);
    }

    public void setAuctionResultNotificationEnabled(boolean enabled) {
        prefs.putBoolean("auction_result_notification", enabled);
    }

    // --- Sounds ---
    public boolean isSoundEnabled() {
        // use old key
        return prefs.getBoolean(SettingsDialog.KEY_SOUND, DEFAULT_SOUND);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.putBoolean(SettingsDialog.KEY_SOUND, enabled);
    }

    public double getSoundVolume() {
        return prefs.getDouble("sound_volume", DEFAULT_SOUND_VOLUME);
    }

    public void setSoundVolume(double volume) {
        prefs.putDouble("sound_volume", Math.max(0.0, Math.min(1.0, volume)));
    }

    // --- Appearance ---
    public String getTheme() {
        return prefs.get("app_theme", DEFAULT_THEME);
    }

    public void setTheme(String theme) {
        prefs.put("app_theme", theme);
    }

    public String getPrimaryColor() {
        return prefs.get(SettingsDialog.KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR);
    }

    public void setPrimaryColor(String color) {
        prefs.put(SettingsDialog.KEY_ACCENT_COLOR, color);
    }

    // --- Auction Preferences ---
    public boolean isConfirmBeforeBid() {
        return prefs.getBoolean("confirm_before_bid", DEFAULT_CONFIRM_BID);
    }

    public void setConfirmBeforeBid(boolean enabled) {
        prefs.putBoolean("confirm_before_bid", enabled);
    }

    public long getHighBidWarningThreshold() {
        return prefs.getLong("high_bid_warning_threshold", DEFAULT_HIGH_BID_WARNING);
    }

    public void setHighBidWarningThreshold(long threshold) {
        prefs.putLong("high_bid_warning_threshold", Math.max(0L, threshold));
    }

    // --- Dashboard Display ---
    public boolean isShowEndedAuctions() {
        return prefs.getBoolean("show_ended_auctions", DEFAULT_SHOW_ENDED);
    }

    public void setShowEndedAuctions(boolean enabled) {
        prefs.putBoolean("show_ended_auctions", enabled);
    }

    public boolean isSortActiveFirst() {
        return prefs.getBoolean("sort_active_first", DEFAULT_SORT_ACTIVE_FIRST);
    }

    public void setSortActiveFirst(boolean enabled) {
        prefs.putBoolean("sort_active_first", enabled);
    }

    public boolean isShowCountdownTimer() {
        return prefs.getBoolean("show_countdown_timer", DEFAULT_SHOW_COUNTDOWN);
    }

    public void setShowCountdownTimer(boolean enabled) {
        prefs.putBoolean("show_countdown_timer", enabled);
    }

    public void resetDashboardDisplayDefaults() {
        setShowEndedAuctions(DEFAULT_SHOW_ENDED);
        setSortActiveFirst(DEFAULT_SORT_ACTIVE_FIRST);
        setShowCountdownTimer(DEFAULT_SHOW_COUNTDOWN);
    }

    public void flush() {
        try {
            prefs.flush();
        } catch (Exception ignored) {
            // Preferences are best-effort; Java will retry later for the user node.
        }
    }
}
