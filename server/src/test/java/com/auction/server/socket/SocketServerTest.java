package com.auction.server.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SocketServerTest {
    private final int TEST_PORT = 8081;
    private SocketServer socketServer;

    @BeforeEach
    public void setUp() throws Exception {
        socketServer = new SocketServer();
        socketServer.start();
        Thread.sleep(200);
    }

    @Test
    public void testConnectionSocket() {
        assertDoesNotThrow(() -> {
            System.out.println("TEST: Đang thử kết nối tới Server cổng " + TEST_PORT);
            Socket clientSocket = null;
            ObjectOutputStream out = null;

            try {
                // 1. Bắt đầu khởi tạo kết nối (Mở máy tính, gọi đến số cổng)
                System.out.println("TEST: Đang thử kết nối tới Server cổng " + TEST_PORT);
                clientSocket = new Socket("localhost", TEST_PORT);

                out = new ObjectOutputStream(clientSocket.getOutputStream());

                Assertions.assertTrue(clientSocket.isConnected(), "Client phải kết nối được tới Server!");
                System.out.println("TEST: Đã kết nối thành công!");

                String message = "Xin chào từ bài Test thuần JUnit 5 siêu tốc!";
                out.writeObject(message);

                out.flush();
                System.out.println("TEST: Đã gửi tin nhắn thành công!");

                Thread.sleep(200);

            } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail("Bài test thất bại do có lỗi xảy ra: " + e.getMessage());

            } finally {

                try {
                    if (out != null) {
                        out.close();
                    }
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                    System.out.println("TEST: Đã đóng kết nối an toàn.");
                } catch (Exception ex) {
                    System.err.println("Lỗi khi đóng tài nguyên: " + ex.getMessage());
                }
            }
        });
    }
}