package com.auction.server.socket;

import com.auction.server.controller.BiddingController;
import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SocketServerTest {
    private final int TEST_PORT = 8081;
    private SocketServer socketServer;

    @BeforeEach
    public void setUp() throws Exception {
        // We pass a BiddingController with a dummy AuctionService to avoid DB dependencies in this unit test
        BiddingController biddingController = new BiddingController();
        biddingController.setAuctionService(new com.auction.server.service.AuctionService() {
            @Override
            public com.auction.server.dto.BidResponse updateBid(Integer itemId, Integer bidderId, java.math.BigDecimal amount) {
                return new com.auction.server.dto.BidResponse(true, "TEST: Đặt giá thành công!", amount);
            }
        });
        
        socketServer = new SocketServer(biddingController);
        socketServer.start();
        Thread.sleep(1000);
    }

    @AfterEach
    public void tearDown() {
        if (socketServer != null) {
            socketServer.stop();
        }
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
                clientSocket = new Socket("127.0.0.1", TEST_PORT);

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
    @Test
    public void testBidRequestSerialization() {
        assertDoesNotThrow(() -> {
            try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                BidRequest bid = new BidRequest(1, 100, new BigDecimal("1000"));
                out.writeObject(bid);
                out.flush();
                System.out.println("TEST: Đã gửi BidRequest");

                Object obj = in.readObject();
                assertTrue(obj instanceof BidResponse, "Phải nhận được BidResponse");

                BidResponse res = (BidResponse) obj;
                System.out.println("TEST: Server phản hồi: " + res.getMessage());
                assertTrue(res.isSuccess());
            }
        });
    }
}