package com.auction.server.socket;

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
    private final int PORT = 8081;
    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);
    private final int systemCores = Runtime.getRuntime().availableProcessors();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(systemCores);
    private final BiddingController biddingController;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private static final ConcurrentHashMap<Integer, List<PrintWriter>> rooms = new ConcurrentHashMap<>();

    @Autowired
    public SocketServer(BiddingController biddingController) {
        this.biddingController = biddingController;
    }

    @PostConstruct
    public void start() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new java.net.InetSocketAddress(PORT));
                logger.info("SERVER SOCKET: Đang lắng nghe tại cổng {}...", PORT);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logger.info("SERVER: Có kết nối mới từ {}", clientSocket.getInetAddress());
                        threadPool.execute(new ClientHandler(clientSocket, biddingController));
                    } catch (SocketException e) {
                        if (running) {
                            logger.error("SERVER SOCKET: Lỗi accept: {}", e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("SERVER SOCKET: Lỗi khởi động ServerSocket: {}", e.getMessage());
            } finally {
                stop();
            }
        }).start();
    }

    public static void joinRoom(Integer sessionId, PrintWriter out) {
        rooms.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(out);
        logger.info("SERVER: Client đã tham gia vào phòng đấu giá ID: {}", sessionId);
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

    // Xóa theo PrintWriter
    public static void removeFromAllRooms(PrintWriter out) {
        rooms.values().forEach(list -> list.remove(out));
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("SERVER SOCKET: Lỗi khi đóng ServerSocket: {}", e.getMessage());
        }
    }
}