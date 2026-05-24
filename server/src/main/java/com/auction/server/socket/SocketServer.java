package com.auction.server.socket;

import org.json.JSONObject;
import com.auction.server.controller.BiddingController;
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
    private final ExecutorService threadPool = Executors.newFixedThreadPool(systemCores);
    private final BiddingController biddingController;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private static final ConcurrentHashMap<Integer, List<PrintWriter>> rooms = new ConcurrentHashMap<>();
    private static final List<PrintWriter> homeClients = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<Integer, List<PrintWriter>> homeUserClients = new ConcurrentHashMap<>();

    @Autowired
    public SocketServer(BiddingController biddingController) {
        this.biddingController = biddingController;
    }

    // Set port manually (used for testing)
    public void setPort(int port) {
        this.port = port;
    }

    @PostConstruct
    public void start() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new java.net.InetSocketAddress(this.port));
                logger.info("SERVER SOCKET: Listening on port {}...", this.port);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logger.info("SERVER SOCKET: Accepted new connection from {}", clientSocket.getRemoteSocketAddress());
                        threadPool.execute(new ClientHandler(clientSocket, biddingController));
                    } catch (SocketException e) {
                        if (running) logger.error("Accept error: {}", e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.error("Error starting ServerSocket on port {}: {}", this.port, e.getMessage());
            } finally {
                stop();
            }
        }).start();
    }

    public static void joinRoom(Integer sessionId, PrintWriter out) {
        rooms.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(out);
        logger.info("SERVER: Client joined room ID: {}", sessionId);
        broadcastCounts(sessionId);
    }

    public static void broadcastToRoom(Integer sessionId, String message) {
        List<PrintWriter> clients = rooms.get(sessionId);
        if (clients != null) {
            clients.forEach(out -> {
                out.println(message);
                out.flush();
            });
        }
    }

    public static void joinHome(PrintWriter out) {
        homeClients.add(out);
        logger.info("SERVER: Home Client connected for global events.");
    }

    public static void joinHome(Integer userId, PrintWriter out) {
        homeUserClients.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(out);
        logger.info("SERVER: Home Client connected for user ID: {}", userId);
    }

    public static void sendToHomeUser(Integer userId, String message) {
        List<PrintWriter> clients = homeUserClients.get(userId);
        if (clients != null) {
            clients.forEach(out -> {
                try {
                    out.println(message);
                    out.flush();
                } catch (Exception e) {
                    logger.error("Error sending message to user {}: {}", userId, e.getMessage());
                }
            });
        }
    }

    public static void broadcastToAll(String message) {
        // Send to all Home clients
        homeClients.forEach(out -> {
            out.println(message);
            out.flush();
        });
        // Send to all targeted Home clients
        homeUserClients.values().forEach(clients -> {
            clients.forEach(out -> {
                out.println(message);
                out.flush();
            });
        });
        // Send to all clients in auction rooms
        rooms.values().forEach(clients -> {
            clients.forEach(out -> {
                out.println(message);
                out.flush();
            });
        });
    }

    public static void removeFromAllRooms(PrintWriter out) {
        rooms.forEach((sessionId, clients) -> {
            if (clients.remove(out)) {
                broadcastCounts(sessionId);
            }
        });
        homeClients.remove(out);
        homeUserClients.values().forEach(clients -> clients.remove(out));
    }

    private static void broadcastCounts(Integer sessionId) {
        List<PrintWriter> clients = rooms.get(sessionId);
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