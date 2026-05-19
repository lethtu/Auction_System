package com.auction.client.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientLogger {
    private static final String LOG_DIR = "client_logs";
    private static final String VIEW_HISTORY_FILE = LOG_DIR + File.separator + "view_history.log";
    private static final String FAVORITES_FILE = LOG_DIR + File.separator + "favorites.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void ensureLogDir() {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void logViewHistory(String username, String itemName, int auctionId, BigDecimal currentPrice) {
        ensureLogDir();
        String time = LocalDateTime.now().format(formatter);
        String user = (username != null && !username.isEmpty()) ? username : "Guest";
        String logLine = String.format("[%s] User: %s | Viewed Auction ID: %d | Item: %s | Price: ₫ %s",
                time, user, auctionId, itemName, currentPrice != null ? currentPrice.toString() : "0");
        
        try (PrintWriter out = new PrintWriter(new FileWriter(VIEW_HISTORY_FILE, true))) {
            out.println(logLine);
        } catch (IOException e) {
            System.err.println("Failed to write view history log: " + e.getMessage());
        }
    }

    public static void logFavorite(String username, String itemName, int auctionId, boolean isAdded) {
        ensureLogDir();
        String time = LocalDateTime.now().format(formatter);
        String user = (username != null && !username.isEmpty()) ? username : "Guest";
        String action = isAdded ? "ADDED to Favorites" : "REMOVED from Favorites";
        String logLine = String.format("[%s] User: %s | Action: %s | Auction ID: %d | Item: %s",
                time, user, action, auctionId, itemName);

        try (PrintWriter out = new PrintWriter(new FileWriter(FAVORITES_FILE, true))) {
            out.println(logLine);
        } catch (IOException e) {
            System.err.println("Failed to write favorites log: " + e.getMessage());
        }
    }

    public static java.util.Set<Integer> loadUserFavorites(String username) {
        java.util.Set<Integer> favIds = new java.util.concurrent.ConcurrentSkipListSet<>();
        File file = new File(FAVORITES_FILE);
        if (!file.exists()) return favIds;

        String targetUser = "User: " + (username != null && !username.isEmpty() ? username : "Guest") + " |";
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(targetUser)) {
                    int idIndex = line.indexOf("Auction ID: ");
                    if (idIndex != -1) {
                        int start = idIndex + "Auction ID: ".length();
                        int end = line.indexOf(" |", start);
                        if (end == -1) end = line.length();
                        try {
                            int auctionId = Integer.parseInt(line.substring(start, end).trim());
                            if (line.contains("Action: ADDED to Favorites")) {
                                favIds.add(auctionId);
                            } else if (line.contains("Action: REMOVED from Favorites")) {
                                favIds.remove(auctionId);
                            }
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read favorites log: " + e.getMessage());
        }
        return favIds;
    }
}
