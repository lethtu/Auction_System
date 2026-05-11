package com.auction.server.socket;

import com.auction.server.dto.PriceUpdateNotification;
import com.auction.server.controller.BiddingController;
import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.math.BigDecimal;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket clientSocket;
    private final BiddingController biddingController;

    public ClientHandler(Socket socket, BiddingController biddingController) {
        this.clientSocket = socket;
        this.biddingController = biddingController;
    }

    @Override
    public void run() {
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            // ĐỔI SANG DÙNG READER & WRITER ĐỂ XỬ LÝ CHUỖI (TEXT)
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            // Lắng nghe liên tục các dòng dữ liệu Client gửi lên
            while ((inputLine = in.readLine()) != null) {

                // Phân loại tin nhắn nhận được
                if (inputLine.startsWith("BID:")) {

                    // 1. Tách lấy phần JSON (cắt bỏ 4 ký tự "BID:")
                    String jsonString = inputLine.substring(4);

                    // 2. PARSE CHUỖI THÀNH JSON OBJECT BẰNG ORG.JSON
                    JSONObject jsonObj = new JSONObject(jsonString);

                    // 3. Chuyển JSON thành Đối tượng BidRequest của Server
                    BidRequest request = new BidRequest(jsonObj.getInt("auctionId"), jsonObj.getInt("bidderId"), jsonObj.getBigDecimal("amount"));

                    // --- TIẾP TỤC LOGIC ĐẤU GIÁ BÌNH THƯỜNG ---
                    SocketServer.joinRoom(request.getAuctionId(), out);
                    BidResponse response = biddingController.handleBid(request);

                    // Trả kết quả về cho Client (Cũng phải gói thành JSON)
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("success", response.isSuccess());
                    jsonResponse.put("currentPrice", response.getCurrentPrice());
                    jsonResponse.put("message", response.getMessage());

                    out.println("RESPONSE BID:" + jsonResponse.toString());

                    // Nếu đấu giá thành công, báo cho mọi người
                    if (response.isSuccess()) {
                        JSONObject jsonNotice = new JSONObject();
                        jsonNotice.put("auctionId", request.getAuctionId());
                        jsonNotice.put("newPrice", response.getCurrentPrice());
                        jsonNotice.put("message", "Giá sản phẩm đã được cập nhật!");

                        // Broadcast dạng JSON String
                        SocketServer.broadcastToRoom(request.getAuctionId(), "NOTICE:" + jsonNotice.toString());
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Lỗi Client: {}", e.getMessage());
        }
        finally {
            if (out != null) {
                SocketServer.removeFromAllRooms(out);
            }
            try {
                clientSocket.close();
            }
            catch (IOException e) {
                logger.error("Lỗi đóng kết nối: {}", e.getMessage(), e);
            }
        }
    }
}