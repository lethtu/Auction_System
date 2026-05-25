package com.auction.server.socket;

import com.auction.server.controller.BiddingController;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

public class SocketServerTest {
    private final int TEST_PORT = 8082;
    private SocketServer socketServer;

    @BeforeEach
    public void setUp() throws Exception {
        BiddingController biddingController = new BiddingController();
        biddingController.setAuctionService(new com.auction.server.service.AuctionService() {
            @Override
            public com.auction.server.dto.BidResponse updateBid(Integer itemId, Integer bidderId, BigDecimal amount) {
                return new com.auction.server.dto.BidResponse(true, "TEST_SUCCESS", amount);
            }

            @Override
            public com.auction.server.dto.BidResponse resolveAutoBids(Integer sessionId) {
                return null;
            }
        });

        socketServer = new SocketServer(biddingController);
        // THÊM: Ép server test dùng đúng cổng 8081
        socketServer.setPort(TEST_PORT);
        socketServer.start();
        Thread.sleep(1000);
    }

    @AfterEach
    public void tearDown() {
        if (socketServer != null) socketServer.stop();
    }

    @Test
    @DisplayName("Test kết nối và gửi lệnh BID bằng JSON")
    public void testBidWithJson() {
        assertDoesNotThrow(() -> {
            try (Socket clientSocket = new Socket("127.0.0.1", TEST_PORT)) {
                clientSocket.setSoTimeout(3000);

                try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                    out.println("JOIN:1");

                    JSONObject jsonBid = new JSONObject();
                    jsonBid.put("auctionId", 1);
                    jsonBid.put("bidderId", 12);
                    jsonBid.put("amount", new BigDecimal("1000000"));
                    out.println("BID:" + jsonBid.toString());

                    String response = readUntilPrefix(in, "RESPONSE:");
                    System.out.println("TEST: Server trả về: " + response);

                    assertNotNull(response, "Server không được trả về null!");
                    assertTrue(response.startsWith("RESPONSE:"), "Phải bắt đầu bằng RESPONSE:");

                    // Cắt lấy phần JSON sau "RESPONSE:" (9 ký tự)
                    JSONObject resJson = new JSONObject(response.substring(9));
                    assertTrue(resJson.getBoolean("success"));
                    assertEquals("TEST_SUCCESS", resJson.getString("message"));
                }
            }
        });
    }

    private String readUntilPrefix(BufferedReader in, String prefix) throws IOException {
        long deadline = System.currentTimeMillis() + 3000;
        String line;

        try {
            while (System.currentTimeMillis() < deadline && (line = in.readLine()) != null) {
                if (line.startsWith(prefix)) {
                    return line;
                }
            }
        } catch (java.net.SocketTimeoutException e) {
            return null;
        }

        return null;
    }
}
