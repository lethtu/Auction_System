package com.auction.client.service;

import com.auction.client.model.audio.SoundEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class SoundManagerCoverageTest {

    @Test
    void getInstance_returnsSingleton() {
        SoundManager first = SoundManager.getInstance();
        SoundManager second = SoundManager.getInstance();

        assertSame(first, second);
    }

    @Test
    void getFilenameForEvent_mapsEverySoundEvent() throws Exception {
        SoundManager manager = SoundManager.getInstance();
        Method method = SoundManager.class.getDeclaredMethod("getFilenameForEvent", SoundEvent.class);
        method.setAccessible(true);

        assertEquals("success.wav", method.invoke(manager, SoundEvent.BID_SUCCESS));
        assertEquals("error.wav", method.invoke(manager, SoundEvent.BID_ERROR));
        assertEquals("outbid.wav", method.invoke(manager, SoundEvent.OUTBID));
        assertEquals("ending_soon.wav", method.invoke(manager, SoundEvent.AUCTION_ENDING_SOON));
        assertEquals("win.wav", method.invoke(manager, SoundEvent.AUCTION_WON));
        assertEquals("lost.wav", method.invoke(manager, SoundEvent.AUCTION_LOST));
        assertEquals("notification.wav", method.invoke(manager, SoundEvent.NOTIFICATION));
    }

    @Test
    void playSound_returnsFalseWithoutTouchingAudioWhenSoundDisabled() {
        SettingsService settings = SettingsService.getInstance();
        boolean previousSoundEnabled = settings.isSoundEnabled();
        settings.setSoundEnabled(false);
        try {
            SoundManager manager = SoundManager.getInstance();

            assertFalse(manager.playSound(SoundEvent.BID_SUCCESS));
            assertFalse(manager.playSound(SoundEvent.AUCTION_ENDING_SOON, 123));
            assertFalse(manager.playSound(SoundEvent.OUTBID, 0.5, false));
        } finally {
            settings.setSoundEnabled(previousSoundEnabled);
            settings.flush();
        }
    }
}
