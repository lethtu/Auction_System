package com.auction.server.socket;

import com.auction.server.controller.BiddingController;
import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import com.auction.server.util.SessionManager;
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
    private final SessionManager sessionManager;
    private PrintWriter out;
    private Integer authenticatedUserId = null;

    public ClientHandler(Socket socket, BiddingController biddingController, SessionManager sessionManager) {
        this.clientSocket = socket;
        this.biddingController = biddingController;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                logger.info("Received inputLine from client: {}", inputLine);
                if (inputLine.startsWith("AUTH:")) {
                    handleAuthMessage(inputLine.substring(5));
                } else if (inputLine.startsWith("JOIN:")) {
                    int sessionId = Integer.parseInt(inputLine.substring(5));
                    SocketServer.joinRoom(sessionId, out);

                } else if (inputLine.startsWith("BID:")) {
                    handleBidMessage(inputLine.substring(4));

                } else if (inputLine.startsWith("AUTOBID:")) {
                    handleAutoBidMessage(inputLine.substring(8));

                } else if (inputLine.startsWith("JOIN_HOME")) {
                    if (inputLine.contains(":")) {
                        try {
                            int userId = Integer.parseInt(inputLine.substring("JOIN_HOME:".length()));
                            SocketServer.joinHome(userId, out);
                        } catch (NumberFormatException e) {
                            logger.error("Invalid JOIN_HOME format: {}", inputLine);
                        }
                    } else {
                        SocketServer.joinHome(out);
                    }
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
        logger.info("handleBidMessage called with payload: {}", jsonString);
        
        if (authenticatedUserId == null) {
            logger.warn("Unauthorized bid attempt: connection not authenticated.");
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "UNAUTHORIZED: Please authenticate first.");
            sendRawResponse(errorResponse);
            return;
        }

        BidRequest request = null;
    
        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            request = new BidRequest(
                    jsonObj.getInt("auctionId"),
                    authenticatedUserId, // Safe bidderId from session
                    new BigDecimal(jsonObj.get("amount").toString())
            );
            logger.info("Parsed BidRequest: auctionId={}, bidderId={}, amount={}", request.getAuctionId(), request.getBidderId(), request.getBidAmount());
    
            BidResponse response;
            try {
                logger.info("Calling biddingController.handleBid...");
                response = biddingController.handleBid(request);
                logger.info("biddingController.handleBid returned success={}", response.isSuccess());
            } catch (com.auction.server.exception.AuctionClosedException e) {
                logger.error("Auction closed exception: {}", e.getMessage());
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", e.getMessage());
                sendRawResponse(errorJson);
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
            sendRawResponse(errorResponse);
        }
    }
    
    private void sendRawResponse(JSONObject jsonResponse) {
        out.println("RESPONSE:" + jsonResponse);
        out.flush();
        if (out.checkError()) {
            logger.error("Failed to send RESPONSE to client (out.checkError() is true)");
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

        sendRawResponse(jsonResponse);
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

        if (response.getPreviousHighestBidderId() != null) {
            JSONObject homeNotice = new JSONObject(notice.toString());
            homeNotice.put("auctionId", auctionId);
            homeNotice.put("previousHighestBidderId", response.getPreviousHighestBidderId());

            String itemName = "Unknown Item";
            try {
                com.auction.server.model.AuctionSession session = biddingController.getAuctionService().getSessionById(auctionId);
                if (session != null && session.getItem() != null) {
                    itemName = session.getItem().getName();
                }
            } catch (Exception e) {
                logger.warn("Could not retrieve item name for notice broadcast: {}", e.getMessage());
            }
            homeNotice.put("itemName", itemName);

            SocketServer.sendToHomeUser(response.getPreviousHighestBidderId(), "NOTICE:" + homeNotice);
        }
    }

    private String generateMaskedCode(Integer sessionId, Integer bidderId) {
        if (bidderId == null) {
            return "#????";
        }

        int hash = java.util.Objects.hash(sessionId, bidderId, "BidPop");
        return String.format("#%04X", Math.abs(hash) % 0xFFFF);
    }

    private void handleAutoBidMessage(String jsonString) {
        if (authenticatedUserId == null) {
            logger.warn("Unauthorized auto-bid attempt: connection not authenticated.");
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "UNAUTHORIZED: Please authenticate first.");
            sendRawResponse(errorResponse);
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            int auctionId = jsonObj.getInt("auctionId");
            BigDecimal maxBid = new BigDecimal(jsonObj.get("maxBid").toString());
            BigDecimal increment = new BigDecimal(jsonObj.get("increment").toString());

            biddingController.registerAutoBid(auctionId, authenticatedUserId, maxBid, increment);

            logger.info(
                    "AUTOBID registered — auctionId={}, bidderId={}, maxBid={}, increment={}",
                    auctionId,
                    authenticatedUserId,
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
            sendRawResponse(response);

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
            sendRawResponse(errorResponse);
        }
    }

    private void handleAuthMessage(String jsonString) {
        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            String token = jsonObj.getString("token");
            SessionManager.SessionUser sessionUser = sessionManager.getSession(token);
            
            JSONObject response = new JSONObject();
            if (sessionUser != null) {
                this.authenticatedUserId = sessionUser.getUserId();
                logger.info("Socket connection authenticated successfully for user: {}", authenticatedUserId);
                response.put("success", true);
                response.put("message", "Authenticated successfully.");
            } else {
                logger.warn("Socket authentication failed: invalid token.");
                response.put("success", false);
                response.put("message", "UNAUTHORIZED: Invalid token.");
            }
            sendRawResponse(response);
        } catch (Exception e) {
            logger.error("Error processing AUTH message", e);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "AUTH ERROR: Invalid payload.");
            sendRawResponse(errorResponse);
        }
    }
}
