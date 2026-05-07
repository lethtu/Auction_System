package com.auction.server.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                // Khởi tạo luồng vào/ra để gửi nhận Object (Task 2)
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            logger.info("Đã kết nối với Client: {}", clientSocket.getInetAddress());

            while (true) {
                // Đọc dữ liệu từ Client gửi lên
                Object input = in.readObject();
                logger.info("Nhận dữ liệu từ Client: {}", input);

                // Logic xử lý sẽ được thêm vào các Task sau
            }
        } catch (EOFException | SocketException e) {
            logger.info("Client {} đã ngắt kết nối.", clientSocket.getInetAddress());
        } catch (Exception e) {
            logger.error("Lỗi xử lý Client {}: {}", clientSocket.getInetAddress(), e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Không thể đóng socket của client.");
            }
        }
    }
}