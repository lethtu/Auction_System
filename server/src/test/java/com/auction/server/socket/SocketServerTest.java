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
    private String testToken;

    @BeforeEach
    public void setUp() throws Exception {
        BiddingController biddingController = new BiddingController();
        com.auction.server.service.AuctionService auctionService = org.mockito.Mockito.mock(com.auction.server.service.AuctionService.class);
        org.mockito.Mockito.when(auctionService.updateBid(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(BigDecimal.class)
        )).thenAnswer(invocation -> new com.auction.server.dto.BidResponse(true, "TEST_SUCCESS", invocation.getArgument(2)));
        org.mockito.Mockito.when(auctionService.resolveAutoBids(org.mockito.ArgumentMatchers.anyInt())).thenReturn(null);
        biddingController.setAuctionService(auctionService);

        com.auction.server.util.SessionManager sessionManager = new com.auction.server.util.SessionManager();
        com.auction.server.model.User testUser = new com.auction.server.model.User();
        testUser.setId(12);
        testUser.setUsername("test_bidder");
        testUser.setAccountType("bidder");
        testToken = sessionManager.createSession(testUser);

        socketServer = new SocketServer(biddingController, sessionManager);
        // THÃŠM: Ã‰p server test dÃ¹ng Ä‘Ãºng cá»•ng 8081
        socketServer.setPort(TEST_PORT);
        socketServer.start();
        Thread.sleep(1000);
    }

    @AfterEach
    public void tearDown() {
        if (socketServer != null) socketServer.stop();
    }

    @Test
    @DisplayName("Test káº¿t ná»‘i vÃ  gá»­i lá»‡nh BID báº±ng JSON")
    public void testBidWithJson() {
        assertDoesNotThrow(() -> {
            try (Socket clientSocket = new Socket("127.0.0.1", TEST_PORT)) {
                clientSocket.setSoTimeout(3000);

                try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                    // Authenticate socket connection
                    JSONObject authJson = new JSONObject();
                    authJson.put("token", testToken);
                    out.println("AUTH:" + authJson.toString());

                    // Wait slightly for authentication response if needed, but since it's sequential TCP, we can proceed
                    String authResponse = readUntilPrefix(in, "RESPONSE:");
                    assertNotNull(authResponse, "Auth response should not be null");
                    assertTrue(authResponse.contains("\"success\":true"), "Auth should succeed");

                    out.println("JOIN:1");

                    JSONObject jsonBid = new JSONObject();
                    jsonBid.put("auctionId", 1);
                    jsonBid.put("bidderId", 12);
                    jsonBid.put("amount", new BigDecimal("1000000"));
                    out.println("BID:" + jsonBid.toString());

                    String response = readUntilPrefix(in, "RESPONSE:");
                    System.out.println("TEST: Server tráº£ vá»: " + response);

                    assertNotNull(response, "Server khÃ´ng Ä‘Æ°á»£c tráº£ vá» null!");
                    assertTrue(response.startsWith("RESPONSE:"), "Pháº£i báº¯t Ä‘áº§u báº±ng RESPONSE:");

                    // Cáº¯t láº¥y pháº§n JSON sau "RESPONSE:" (9 kÃ½ tá»±)
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
