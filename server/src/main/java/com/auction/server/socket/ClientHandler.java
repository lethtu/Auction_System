package com.auction.server.socket;

import com.auction.server.controller.BiddingController;
import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket clientSocket;
    private final BiddingController biddingController;
    private PrintWriter out;

    public ClientHandler(Socket socket, BiddingController biddingController) {
        this.clientSocket = socket;
        this.biddingController = biddingController;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("JOIN:")) {
                    int sessionId = Integer.parseInt(inputLine.substring(5));
                    SocketServer.joinRoom(sessionId, out);
                } else if (inputLine.startsWith("BID:")) {
                    handleBidMessage(inputLine.substring(4));
                } else if (inputLine.startsWith("AUTOBID:")) {
                    handleAutoBidMessage(inputLine.substring(8));
                }
            }
        } catch (IOException e) {
            if (e instanceof java.net.SocketException) {
                System.out.println("Client disconnected: " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        } finally {
            SocketServer.removeFromAllRooms(out);
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleBidMessage(String jsonString) {
        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            BidRequest request = new BidRequest(
                    jsonObj.getInt("auctionId"),
                    jsonObj.getInt("bidderId"),
                    new BigDecimal(jsonObj.get("amount").toString())
            );

            BidResponse response = biddingController.handleBid(request);
            sendBidResponse(response);

            if (response.isSuccess()) {
                broadcastBidNotice(request.getAuctionId(), response);

                // Resolve auto-bid O(1) sau khi bid thành công — 1 lần duy nhất
                BidResponse autoBidResult = biddingController.resolveAutoBids(request.getAuctionId());
                if (autoBidResult != null && autoBidResult.isSuccess()) {
                    broadcastBidNotice(request.getAuctionId(), autoBidResult);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "LỖI HỆ THỐNG: Không xử lý được yêu cầu đặt giá.");
            out.println("RESPONSE:" + errorResponse);
        }
    }

    private void sendBidResponse(BidResponse response) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("success", response.isSuccess());
        jsonResponse.put("message", response.getMessage());
        jsonResponse.put("currentPrice", response.getCurrentPrice());

        if (response.getHighestBidderId() != null) {
            jsonResponse.put("highestBidderId", response.getHighestBidderId());
        }
        if (response.getBidCount() != null) {
            jsonResponse.put("bidCount", response.getBidCount());
        }
        if (response.getNewEndTime() != null) {
            jsonResponse.put("newEndTime", response.getNewEndTime());
        }

        out.println("RESPONSE:" + jsonResponse);
    }

    private void broadcastBidNotice(Integer auctionId, BidResponse response) {
        JSONObject notice = new JSONObject();
        notice.put("newPrice", response.getCurrentPrice());

        if (response.getHighestBidderId() != null) {
            notice.put("highestBidderId", response.getHighestBidderId());
            notice.put("bidderId", response.getHighestBidderId());
        }
        if (response.getBidCount() != null) {
            notice.put("bidCount", response.getBidCount());
        }
        if (response.getNewEndTime() != null) {
            notice.put("newEndTime", response.getNewEndTime());
        }

        SocketServer.broadcastToRoom(auctionId, "NOTICE:" + notice);
    }

    private void handleAutoBidMessage(String jsonString) {
        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            int auctionId = jsonObj.getInt("auctionId");
            int bidderId = jsonObj.getInt("bidderId");
            BigDecimal maxBid = new BigDecimal(jsonObj.get("maxBid").toString());
            BigDecimal increment = new BigDecimal(jsonObj.get("increment").toString());

            // Lưu config vào Database qua service layer
            biddingController.registerAutoBid(auctionId, bidderId, maxBid, increment);

            logger.info("AUTOBID registered — auctionId={}, bidderId={}, maxBid={}, increment={}",
                    auctionId, bidderId, maxBid, increment);

            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("message", "Kích hoạt Auto-bidding thành công! Hệ thống sẽ tự đặt giá khi có người trả giá cao hơn.");
            out.println("RESPONSE:" + response);

            // Resolve ngay lập tức nếu đang có giá cần phản đòn
            BidResponse autoBidResult = biddingController.resolveAutoBids(auctionId);
            if (autoBidResult != null && autoBidResult.isSuccess()) {
                broadcastBidNotice(auctionId, autoBidResult);
            }
        } catch (Exception e) {
            logger.error("Error processing AUTOBID message", e);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi xử lý Auto-bidding: " + e.getMessage());
            out.println("RESPONSE:" + errorResponse);
        }
    }
}