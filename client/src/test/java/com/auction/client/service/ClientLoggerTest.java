package com.auction.client.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ClientLoggerTest {
    private static final String LOG_DIR = "client_logs";
    private static final String VIEW_HISTORY_FILE = LOG_DIR + File.separator + "view_history.log";
    private static final String FAVORITES_FILE = LOG_DIR + File.separator + "favorites.log";

    @BeforeEach
    public void setUp() throws IOException {
        cleanLogs();
    }

    @AfterEach
    public void tearDown() throws IOException {
        cleanLogs();
    }

    private void cleanLogs() throws IOException {
        Path favPath = Paths.get(FAVORITES_FILE);
        if (Files.exists(favPath)) {
            Files.delete(favPath);
        }
        Path viewPath = Paths.get(VIEW_HISTORY_FILE);
        if (Files.exists(viewPath)) {
            Files.delete(viewPath);
        }
        Path dirPath = Paths.get(LOG_DIR);
        if (Files.exists(dirPath)) {
            try {
                Files.delete(dirPath);
            } catch (IOException ignored) {}
        }
    }

    @Test
    public void testLogViewHistory_guest() throws IOException {
        ClientLogger.logViewHistory(null, "Laptop", 101, new BigDecimal("15000"));
        File file = new File(VIEW_HISTORY_FILE);
        assertTrue(file.exists());
        String content = Files.readString(file.toPath());
        assertTrue(content.contains("User: Guest"));
        assertTrue(content.contains("Viewed Auction ID: 101"));
        assertTrue(content.contains("Item: Laptop"));
        assertTrue(content.contains("Price: ₫ 15000"));
    }

    @Test
    public void testLogViewHistory_user() throws IOException {
        ClientLogger.logViewHistory("john_doe", "Phone", 202, new BigDecimal("500"));
        File file = new File(VIEW_HISTORY_FILE);
        assertTrue(file.exists());
        String content = Files.readString(file.toPath());
        assertTrue(content.contains("User: john_doe"));
        assertTrue(content.contains("Viewed Auction ID: 202"));
        assertTrue(content.contains("Item: Phone"));
        assertTrue(content.contains("Price: ₫ 500"));
    }

    @Test
    public void testLogFavorite_workflow() {
        // Initially empty
        Set<Integer> favs = ClientLogger.loadUserFavorites("alice");
        assertTrue(favs.isEmpty());

        // Add favorites
        ClientLogger.logFavorite("alice", "Car", 303, true);
        ClientLogger.logFavorite("alice", "Bike", 404, true);

        favs = ClientLogger.loadUserFavorites("alice");
        assertEquals(2, favs.size());
        assertTrue(favs.contains(303));
        assertTrue(favs.contains(404));

        // Remove favorite
        ClientLogger.logFavorite("alice", "Car", 303, false);

        favs = ClientLogger.loadUserFavorites("alice");
        assertEquals(1, favs.size());
        assertFalse(favs.contains(303));
        assertTrue(favs.contains(404));

        // Empty/null username
        ClientLogger.logFavorite(null, "Bike", 404, true);
        Set<Integer> guestFavs = ClientLogger.loadUserFavorites(null);
        assertTrue(guestFavs.contains(404));
    }
}
