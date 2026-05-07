package com.auction.server.socket;


import com.auction.server.dto.BidRequest;
import com.auction.server.dto.BidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            logger.info("Đã kết nối với Client: {}", clientSocket.getInetAddress());

            while (true) {
                try {
                    Object input = in.readObject();

                    if (input instanceof BidRequest) {
                        BidRequest request = (BidRequest) input;
                        logger.info("SERVER: Nhận lệnh đặt giá: {}", request);

                        String msg = "Người dùng " + request.getUserId() + " đặt giá thành công!";
                        BidResponse response = new BidResponse(true, msg, request.getBidAmount());

                        out.writeObject(response);
                        out.flush();
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("SERVER SOCKET: Không hiểu định dạng dữ liệu nhận được");
                }
            }
        } catch (EOFException | SocketException e) {
            logger.info("Client {} đã ngắt kết nối.", clientSocket.getInetAddress());
        } catch (Exception e) {
            logger.error("Lỗi xử lý Client {}: {}", clientSocket.getInetAddress(), e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Không thể đóng socket của client.");
            }
        }
    }
}