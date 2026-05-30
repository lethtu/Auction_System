package com.auction.server.socket;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class WebSocketRoomRegistry {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketRoomRegistry.class);

    private static final ConcurrentHashMap<Integer, List<SocketClient>> rooms = new ConcurrentHashMap<>();
    private static final List<SocketClient> homeClients = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<Integer, List<SocketClient>> homeUserClients = new ConcurrentHashMap<>();

    public static void joinRoom(Integer sessionId, SocketClient client) {
        rooms.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(client);
        logger.info("SERVER: Client joined room ID: {}", sessionId);
        broadcastCounts(sessionId);
    }

    public static void broadcastToRoom(Integer sessionId, String message) {
        List<SocketClient> clients = rooms.get(sessionId);
        if (clients != null) {
            clients.removeIf(client -> !sendSafely(client, message));
        }
    }

    public static void joinHome(SocketClient client) {
        homeClients.add(client);
        logger.info("SERVER: Home Client connected for global events.");
    }

    public static void joinHome(Integer userId, SocketClient client) {
        homeUserClients.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(client);
        logger.info("SERVER: Home Client connected for user ID: {}", userId);
    }

    public static void sendToHomeUser(Integer userId, String message) {
        List<SocketClient> clients = homeUserClients.get(userId);
        if (clients != null) {
            clients.removeIf(client -> !sendSafely(client, message));
        }
    }

    public static void broadcastToAll(String message) {
        // Send to all Home clients
        homeClients.removeIf(client -> !sendSafely(client, message));
        // Send to all targeted Home clients
        homeUserClients.values().forEach(clients -> {
            clients.removeIf(client -> !sendSafely(client, message));
        });
        // Send to all clients in auction rooms
        rooms.values().forEach(clients -> {
            clients.removeIf(client -> !sendSafely(client, message));
        });
    }

    private static boolean sendSafely(SocketClient client, String message) {
        if (client == null) {
            return false;
        }
        try {
            client.sendMessage(message);
            return client.isOpen();
        } catch (Exception e) {
            logger.warn("Removing stale socket client: {}", e.getMessage());
            return false;
        }
    }

    public static void removeFromAllRooms(SocketClient client) {
        rooms.forEach((sessionId, clients) -> {
            if (clients.remove(client)) {
                broadcastCounts(sessionId);
            }
        });
        homeClients.remove(client);
        homeUserClients.values().forEach(clients -> clients.remove(client));
    }

    private static void broadcastCounts(Integer sessionId) {
        List<SocketClient> clients = rooms.get(sessionId);
        int count = clients == null ? 0 : clients.size();
        JSONObject notice = new JSONObject();
        notice.put("watchingCount", count);
        broadcastToRoom(sessionId, "WATCHING:" + notice.toString());
        broadcastToRoom(sessionId, "ROOM_COUNT:" + count);
    }
}
