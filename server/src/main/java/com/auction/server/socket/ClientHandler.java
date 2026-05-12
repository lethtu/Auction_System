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
                    SocketServer.joinRoom(sessionId, out);
                }
                else if (inputLine.startsWith("BID:")) {
                    String jsonString = inputLine.substring(4);
                    JSONObject jsonObj = new JSONObject(jsonString);

                    BidRequest request = new BidRequest(
                            jsonObj.getInt("auctionId"),
                            jsonObj.getInt("bidderId"),
                            new java.math.BigDecimal(jsonObj.get("amount").toString())
                    );

                    // BỌC GIÁP TASK 7 TẠI ĐÂY
                    BidResponse response;
                    try {
                        response = biddingController.handleBid(request);
                    } catch (com.auction.server.exception.AuctionClosedException e) {
                        // Bắt lỗi ném ra từ Service, gửi trả Client an toàn, không sập Server
                        JSONObject errorJson = new JSONObject();
                        errorJson.put("success", false);
                        errorJson.put("message", e.getMessage());
                        out.println("RESPONSE:" + errorJson.toString());
                        continue; // Bỏ qua các bước phía dưới, quay lại lắng nghe tiếp
                    }

                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("success", response.isSuccess());
                    jsonResponse.put("message", response.getMessage());
                    jsonResponse.put("currentPrice", response.getCurrentPrice());

                    // QUAN TRỌNG: Nhét thời gian mới vào JSON gửi cho người đặt giá
                    if (response.getNewEndTime() != null) {
                        jsonResponse.put("newEndTime", response.getNewEndTime());
                    }

                    out.println("RESPONSE:" + jsonResponse.toString());

                    // Broadcast nếu thành công cho toàn bộ người trong phòng
                    if (response.isSuccess()) {
                        JSONObject notice = new JSONObject();
                        notice.put("newPrice", response.getCurrentPrice());

                        // QUAN TRỌNG: Nhét thời gian mới vào JSON để mọi người cùng thấy đồng hồ nảy lên
                        if (response.getNewEndTime() != null) {
                            notice.put("newEndTime", response.getNewEndTime());
                        }
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