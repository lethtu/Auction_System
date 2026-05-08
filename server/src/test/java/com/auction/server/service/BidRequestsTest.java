package com.auction.server.service;

import com.auction.server.controller.BiddingController;
import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import com.auction.server.socket.SocketServer;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BidRequestsTest {

    @Test
    public void testBidRequestOverSocket() {
        final int TEST_PORT = 8081;

        BiddingController dummyController = new BiddingController() {
            @Override
            public synchronized BidResponse handleBid(BidRequest req) {
                System.out.println("TEST SERVER: Đã nhận được yêu cầu đóng thế cho ID = " + req.getAuctionId());
                // Luôn trả về thành công để test luồng Socket
                return new BidResponse(true, "Thành công (từ Dummy Controller)!", req.getBidAmount());
            }
        };

        SocketServer socketServer = new SocketServer(dummyController);
        socketServer.start();

        assertDoesNotThrow(() -> {
            Thread.sleep(1000);

            try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                BidRequest bid = new BidRequest(5, 12, new BigDecimal("99999999999"));
                out.writeObject(bid);
                out.flush();
                System.out.println("TEST CLIENT: Đã gửi BidRequest");

                Object obj = in.readObject();
                assertTrue(obj instanceof BidResponse, "Phải nhận được BidResponse");

                BidResponse res = (BidResponse) obj;
                System.out.println("TEST CLIENT: Server phản hồi: " + res.getMessage());
                assertTrue(res.isSuccess());

            } finally {
                socketServer.stop();
            }
        });
    }
}