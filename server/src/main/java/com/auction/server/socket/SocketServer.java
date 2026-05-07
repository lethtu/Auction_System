package com.auction.server.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;

@Component
public class SocketServer {
    @Value("${socket.port}")
    private int PORT;
    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);
    private final int systemCores = Runtime.getRuntime().availableProcessors();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(systemCores);

    @PostConstruct // Tự động chạy ngay khi Spring Boot khởi động xong
    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT))
            {
                logger.info("SERVER SOCKET: Đang lắng nghe tại cổng {}...", PORT);

                while (true)
                {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("SERVER: Có kết nối mới từ {}", clientSocket.getInetAddress());

                    threadPool.execute(new ClientHandler(clientSocket));
                }
            }
            catch (IOException e) {
                logger.error("SERVER SOCKET: Lỗi khởi động ServerSocket: {}", e.getMessage());
            }
        }).start();
    }
}