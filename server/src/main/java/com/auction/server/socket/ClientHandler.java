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

                } else if ("JOIN_HOME".equals(inputLine)) {
                    SocketServer.joinHome(out);
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
        BidRequest request = null;
    
        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            request = new BidRequest(
                    jsonObj.getInt("auctionId"),
                    jsonObj.getInt("bidderId"),
                    new BigDecimal(jsonObj.get("amount").toString())
            );
    
            BidResponse response;
            try {
                response = biddingController.handleBid(request);
            } catch (com.auction.server.exception.AuctionClosedException e) {
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", e.getMessage());
                out.println("RESPONSE:" + errorJson);
                return;
            }
    
            // IMPORTANT:
            // Return the main bid result first, so client/test receives the correct SUCCESS RESPONSE.
            sendBidResponse(response);
    
            if (response.isSuccess()) {
                broadcastBidNotice(request.getAuctionId(), response);
    
                // Auto-bid is secondary processing. Auto-bid errors must not fail the main bid.
                try {
                    BidResponse autoBidResult = biddingController.resolveAutoBids(request.getAuctionId());
                    if (autoBidResult != null && autoBidResult.isSuccess()) {
                        broadcastBidNotice(request.getAuctionId(), autoBidResult);
                    }
                } catch (Exception autoBidException) {
                    logger.error(
                            "Auto-bid resolve failed after successful bid. auctionId={}",
                            request.getAuctionId(),
                            autoBidException
                    );
                }
            }
    
        } catch (Exception e) {
            logger.error("Error processing BID message", e);
    
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "SYSTEM ERROR: Could not process bid request.");
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
            notice.put("maskedBidderCode", generateMaskedCode(auctionId, response.getHighestBidderId()));
        }
        if (response.getBidCount() != null) {
            notice.put("bidCount", response.getBidCount());
        }
        if (response.getNewEndTime() != null) {
            notice.put("newEndTime", response.getNewEndTime());
        }
        if (response.getBidTime() != null) {
            notice.put("bidTime", response.getBidTime());
        }
        if (response.getBidId() != null) {
            notice.put("bidId", response.getBidId());
        }

        SocketServer.broadcastToRoom(auctionId, "NOTICE:" + notice);
    }

    private String generateMaskedCode(Integer sessionId, Integer bidderId) {
        if (bidderId == null) {
            return "#????";
        }

        int hash = java.util.Objects.hash(sessionId, bidderId, "BidPop");
        return String.format("#%04X", Math.abs(hash) % 0xFFFF);
    }

    private void handleAutoBidMessage(String jsonString) {
        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            int auctionId = jsonObj.getInt("auctionId");
            int bidderId = jsonObj.getInt("bidderId");
            BigDecimal maxBid = new BigDecimal(jsonObj.get("maxBid").toString());
            BigDecimal increment = new BigDecimal(jsonObj.get("increment").toString());

            biddingController.registerAutoBid(auctionId, bidderId, maxBid, increment);

            logger.info(
                    "AUTOBID registered — auctionId={}, bidderId={}, maxBid={}, increment={}",
                    auctionId,
                    bidderId,
                    maxBid,
                    increment
            );

            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("type", "AUTOBID_CONFIG");
            response.put(
                    "message",
                    "Auto-bidding activated successfully! The system will automatically bid when someone offers a higher price."
            );
            out.println("RESPONSE:" + response);

            // Resolve immediately if there's a price that needs counter-bidding
            BidResponse autoBidResult = biddingController.resolveAutoBids(auctionId);
            if (autoBidResult != null && autoBidResult.isSuccess()) {
                broadcastBidNotice(auctionId, autoBidResult);
            }

        } catch (Exception e) {
            logger.error("Error processing AUTOBID message", e);

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "Auto-bidding processing error: " + e.getMessage());
            out.println("RESPONSE:" + errorResponse);
        }
    }
}
