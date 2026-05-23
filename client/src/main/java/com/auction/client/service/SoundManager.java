package com.auction.client.service;

import com.auction.client.model.audio.SoundEvent;
import javafx.scene.media.AudioClip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SoundManager {
    private static final Logger logger = LoggerFactory.getLogger(SoundManager.class);
    private static SoundManager instance;

    // Deduplication for ending soon notifications (one per session)
    private final Set<Integer> endingSoonNotifiedSessions = ConcurrentHashMap.newKeySet();
    
    // Rate limiting
    private final Map<SoundEvent, Long> lastPlayedTime = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MS = 2000;

    // AudioClip Cache
    private final Map<SoundEvent, AudioClip> clipCache = new ConcurrentHashMap<>();

    private SoundManager() {}

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    public boolean playSound(SoundEvent event) {
        return playSound(event, null, null, false);
    }
    
    public boolean playSound(SoundEvent event, Integer auctionId) {
        return playSound(event, auctionId, null, false);
    }

    public boolean playSound(SoundEvent event, Double volumeOverride, boolean ignoreRateLimit) {
        return playSound(event, null, volumeOverride, ignoreRateLimit);
    }

    public boolean playSoundWithVolume(SoundEvent event, double volume) {
        return playSound(event, null, volume, true);
    }

    private boolean playSound(SoundEvent event, Integer auctionId, Double volumeOverride, boolean ignoreRateLimit) {
        if (!ignoreRateLimit && !SettingsService.getInstance().isSoundEnabled()) {
            return false;
        }

        // Dedup for ENDING_SOON
        if (event == SoundEvent.AUCTION_ENDING_SOON && auctionId != null) {
            if (!endingSoonNotifiedSessions.add(auctionId)) {
                return false; // Already played for this auction
            }
        }

        // Rate limit for specific events
        if (!ignoreRateLimit && (event == SoundEvent.OUTBID || event == SoundEvent.NOTIFICATION || event == SoundEvent.AUCTION_ENDING_SOON || event == SoundEvent.BID_ERROR)) {
            long now = System.currentTimeMillis();
            long lastTime = lastPlayedTime.getOrDefault(event, 0L);
            if (now - lastTime < RATE_LIMIT_MS) {
                return false;
            }
            lastPlayedTime.put(event, now);
        }

        // Determine and clamp volume
        double volume = (volumeOverride != null) ? volumeOverride : SettingsService.getInstance().getSoundVolume();
        volume = Math.max(0.0, Math.min(1.0, volume));

        String filename = getFilenameForEvent(event);
        try {
            AudioClip clip = clipCache.get(event);
            if (clip == null) {
                URL resource = getClass().getResource("/sounds/" + filename);
                if (resource == null) {
                    logger.warn("Audio file missing, fallback applied: /sounds/{}", filename);
                    return false;
                }
                clip = new AudioClip(resource.toExternalForm());
                clipCache.put(event, clip);
            }
            clip.setVolume(volume);
            clip.play();
            return true;
        } catch (Exception e) {
            logger.warn("Failed to play audio {}: {}", filename, e.getMessage());
            return false;
        }
    }

    private String getFilenameForEvent(SoundEvent event) {
        switch (event) {
            case BID_SUCCESS: return "success.wav";
            case BID_ERROR: return "error.wav";
            case OUTBID: return "outbid.wav";
            case AUCTION_ENDING_SOON: return "ending_soon.wav";
            case AUCTION_WON: return "win.wav";
            case AUCTION_LOST: return "lost.wav";
            case NOTIFICATION:
            default: return "notification.wav";
        }
    }
}
