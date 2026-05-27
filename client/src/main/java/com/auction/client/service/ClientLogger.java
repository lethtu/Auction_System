package com.auction.client.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientLogger {
    private static final Logger logger = LoggerFactory.getLogger(ClientLogger.class);

    private static final Path LOG_DIR = Path.of("client_logs");
    private static final Path VIEW_HISTORY_FILE = LOG_DIR.resolve("view_history.log");
    private static final Path FAVORITES_FILE = LOG_DIR.resolve("favorites.log");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ClientLogger() {
    }

    private static boolean ensureLogDir() {
        try {
            Files.createDirectories(LOG_DIR);
            return true;
        } catch (IOException e) {
            logger.warn("Failed to create client log directory.", e);
            return false;
        }
    }

    public static void logViewHistory(String username, String itemName, int auctionId, BigDecimal currentPrice) {
        if (!ensureLogDir()) {
            return;
        }

        String time = LocalDateTime.now().format(formatter);
        String user = (username != null && !username.isEmpty()) ? username : "Guest";
        String logLine = String.format("[%s] User: %s | Viewed Auction ID: %d | Item: %s | Price: ₫ %s",
                time, user, auctionId, itemName, currentPrice != null ? currentPrice.toString() : "0");

        appendLine(VIEW_HISTORY_FILE, logLine, "view history");
    }

    public static void logFavorite(String username, String itemName, int auctionId, boolean isAdded) {
        if (!ensureLogDir()) {
            return;
        }

        String time = LocalDateTime.now().format(formatter);
        String user = (username != null && !username.isEmpty()) ? username : "Guest";
        String action = isAdded ? "ADDED to Favorites" : "REMOVED from Favorites";
        String logLine = String.format("[%s] User: %s | Action: %s | Auction ID: %d | Item: %s",
                time, user, action, auctionId, itemName);

        appendLine(FAVORITES_FILE, logLine, "favorites");
    }

    public static Set<Integer> loadUserFavorites(String username) {
        Set<Integer> favIds = new ConcurrentSkipListSet<>();
        if (!Files.exists(FAVORITES_FILE)) {
            return favIds;
        }

        String user = (username != null && !username.isEmpty()) ? username : "Guest";
        String targetUser = "User: " + user + " |";

        try (BufferedReader reader = Files.newBufferedReader(FAVORITES_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                applyFavoriteLogLine(favIds, targetUser, line);
            }
        } catch (IOException e) {
            logger.warn("Failed to read favorites log.", e);
        }
        return favIds;
    }

    private static void appendLine(Path file, String line, String logName) {
        try {
            Files.writeString(
                    file,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            logger.warn("Failed to write {} log.", logName, e);
        }
    }

    private static void applyFavoriteLogLine(Set<Integer> favIds, String targetUser, String line) {
        if (!line.contains(targetUser)) {
            return;
        }

        int idIndex = line.indexOf("Auction ID: ");
        if (idIndex == -1) {
            return;
        }

        int start = idIndex + "Auction ID: ".length();
        int end = line.indexOf(" |", start);
        if (end == -1) {
            end = line.length();
        }

        try {
            int auctionId = Integer.parseInt(line.substring(start, end).trim());
            if (line.contains("Action: ADDED to Favorites")) {
                favIds.add(auctionId);
            } else if (line.contains("Action: REMOVED from Favorites")) {
                favIds.remove(auctionId);
            }
        } catch (NumberFormatException e) {
            logger.debug("Ignoring malformed favorite log line: {}", line);
        }
    }
}
