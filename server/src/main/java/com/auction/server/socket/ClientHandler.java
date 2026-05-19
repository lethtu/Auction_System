package com.auction.server.socket;

import com.auction.server.controller.BiddingController;
import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;

public class ClientHandler implements Runnable {
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
                } else if ("JOIN_HOME".equals(inputLine)) {
                    SocketServer.joinHome(out);
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
}