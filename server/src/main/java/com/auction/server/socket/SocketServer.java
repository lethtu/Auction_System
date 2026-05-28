package com.auction.server.socket;

import org.json.JSONObject;
import com.auction.server.controller.BiddingController;
import com.auction.server.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SocketServer {
    private int port = 8081; // Non-final to allow changes during testing
    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);
    private final int systemCores = Runtime.getRuntime().availableProcessors();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(systemCores, runnable -> {
        Thread thread = new Thread(runnable, "socket-client-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final BiddingController biddingController;
    private final SessionManager sessionManager;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private static final ConcurrentHashMap<Integer, List<SocketClient>> rooms = new ConcurrentHashMap<>();
    private static final List<SocketClient> homeClients = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<Integer, List<SocketClient>> homeUserClients = new ConcurrentHashMap<>();

    @Autowired
    public SocketServer(BiddingController biddingController, SessionManager sessionManager) {
        this.biddingController = biddingController;
        this.sessionManager = sessionManager;
    }

    // Set port manually (used for testing)
    public void setPort(int port) {
        this.port = port;
    }

    @PostConstruct
    public void start() {
        Thread acceptThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new java.net.InetSocketAddress(this.port));
                logger.info("SERVER SOCKET: Listening on port {}...", this.port);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logger.info("SERVER SOCKET: Accepted new connection from {}", clientSocket.getRemoteSocketAddress());
                        threadPool.execute(new ClientHandler(clientSocket, biddingController, sessionManager));
                    } catch (SocketException e) {
                        if (running) logger.error("Accept error: {}", e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.error("Error starting ServerSocket on port {}: {}", this.port, e.getMessage());
            } finally {
                stop();
            }
        }, "socket-accept-loop");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    // Overloads for PrintWriter to support legacy TCP client handler
    public static void joinRoom(Integer sessionId, PrintWriter out) {
        joinRoom(sessionId, new TcpSocketClient(out));
    }

    public static void joinHome(PrintWriter out) {
        joinHome(new TcpSocketClient(out));
    }

    public static void joinHome(Integer userId, PrintWriter out) {
        joinHome(userId, new TcpSocketClient(out));
    }

    public static void removeFromAllRooms(PrintWriter out) {
        removeFromAllRooms(new TcpSocketClient(out));
    }

    // New API using SocketClient interface
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

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            logger.error("Error closing ServerSocket: {}", e.getMessage());
        }
    }
}
