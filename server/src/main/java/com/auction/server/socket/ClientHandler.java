package com.auction.server.socket;

import com.auction.server.controller.BiddingController;
import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import org.json.JSONObject;
import java.io.*;
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
                    SocketServer.joinRoom(sessionId, out); // Đã khớp với PrintWriter
                }
                else if (inputLine.startsWith("BID:")) {
                    String jsonString = inputLine.substring(4);
                    JSONObject jsonObj = new JSONObject(jsonString);

                    BidRequest request = new BidRequest(
                            jsonObj.getInt("auctionId"),
                            jsonObj.getInt("bidderId"),
                            new java.math.BigDecimal(jsonObj.get("amount").toString())
                    );

                    BidResponse response = biddingController.handleBid(request);

                    // SỬA: Prefix phải là "RESPONSE:" để bài Test cắt chuỗi (substring(9)) không bị lỗi
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("success", response.isSuccess());
                    jsonResponse.put("message", response.getMessage());
                    jsonResponse.put("currentPrice", response.getCurrentPrice());

                    out.println("RESPONSE:" + jsonResponse.toString());

                    // Broadcast nếu thành công
                    if (response.isSuccess()) {
                        JSONObject notice = new JSONObject();
                        notice.put("newPrice", response.getCurrentPrice());
                        SocketServer.broadcastToRoom(request.getAuctionId(), "NOTICE:" + notice.toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SocketServer.removeFromAllRooms(out);
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}