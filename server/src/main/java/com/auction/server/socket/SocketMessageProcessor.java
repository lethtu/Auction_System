package com.auction.server.socket;

import com.auction.server.controller.BiddingController;
import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import com.auction.server.util.SessionManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;

public class SocketMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SocketMessageProcessor.class);

    private final BiddingController biddingController;
    private final SessionManager sessionManager;
    private final SocketClient socketClient;
    private Integer authenticatedUserId = null;

    public SocketMessageProcessor(BiddingController biddingController, SessionManager sessionManager, SocketClient socketClient) {
        this.biddingController = biddingController;
        this.sessionManager = sessionManager;
        this.socketClient = socketClient;
    }

    public Integer getAuthenticatedUserId() {
        return authenticatedUserId;
    }

    public void setAuthenticatedUserId(Integer authenticatedUserId) {
        this.authenticatedUserId = authenticatedUserId;
    }

    public void processMessage(String inputLine) {
        logger.info("Processing inputLine: {}", inputLine);
        try {
            if (inputLine.startsWith("AUTH:")) {
                handleAuthMessage(inputLine.substring(5));
            } else if (inputLine.startsWith("JOIN:")) {
                int sessionId = Integer.parseInt(inputLine.substring(5));
                WebSocketRoomRegistry.joinRoom(sessionId, socketClient);
            } else if (inputLine.startsWith("BID:")) {
                handleBidMessage(inputLine.substring(4));
            } else if (inputLine.startsWith("AUTOBID:")) {
                handleAutoBidMessage(inputLine.substring(8));
            } else if (inputLine.startsWith("JOIN_HOME")) {
                if (inputLine.contains(":")) {
                    try {
                        int userId = Integer.parseInt(inputLine.substring("JOIN_HOME:".length()));
                        WebSocketRoomRegistry.joinHome(userId, socketClient);
                    } catch (NumberFormatException e) {
                        logger.error("Invalid JOIN_HOME format: {}", inputLine);
                    }
                } else {
                    WebSocketRoomRegistry.joinHome(socketClient);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", inputLine, e);
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
    
            // Return the main bid result first
            sendBidResponse(request, response);
    
            if (response.isSuccess()) {
                broadcastBidNotice(request.getAuctionId(), response);
    
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
        socketClient.sendMessage("RESPONSE:" + jsonResponse.toString());
    }

    private void sendBidResponse(BidRequest request, BidResponse response) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("success", response.isSuccess());
        jsonResponse.put("message", response.getMessage());
        jsonResponse.put("currentPrice", response.getCurrentPrice());
        if (request != null) {
            jsonResponse.put("auctionId", request.getAuctionId());
            jsonResponse.put("bidderId", request.getBidderId());
        }

        if (response.getHighestBidderId() != null) {
            jsonResponse.put("highestBidderId", response.getHighestBidderId());
        }
        if (response.getBidCount() != null) {
            jsonResponse.put("bidCount", response.getBidCount());
        }
        if (response.getNewEndTime() != null) {
            jsonResponse.put("newEndTime", response.getNewEndTime());
        }
        if (response.getBidTime() != null) {
            jsonResponse.put("bidTime", response.getBidTime());
        }
        if (response.getBidId() != null) {
            jsonResponse.put("bidId", response.getBidId());
        }

        logger.info("sendBidResponse: auctionId={}, bidderId={}, highestBidderId={}, bidId={}, success={}",
                request != null ? request.getAuctionId() : null,
                request != null ? request.getBidderId() : null,
                response.getHighestBidderId(),
                response.getBidId(),
                response.isSuccess());
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

        logger.info("broadcastBidNotice: auctionId={}, highestBidderId={}, bidId={}, bidCount={}, newPrice={}",
                auctionId,
                response.getHighestBidderId(),
                response.getBidId(),
                response.getBidCount(),
                response.getCurrentPrice());
        WebSocketRoomRegistry.broadcastToRoom(auctionId, "NOTICE:" + notice);

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

            WebSocketRoomRegistry.sendToHomeUser(response.getPreviousHighestBidderId(), "NOTICE:" + homeNotice);
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
}
