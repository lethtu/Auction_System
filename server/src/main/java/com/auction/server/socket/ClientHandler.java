package com.auction.server.socket;

import com.auction.server.dto.PriceUpdateNotification;
import com.auction.server.controller.BiddingController;
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
    private final BiddingController biddingController;

    public ClientHandler(Socket socket, BiddingController biddingController) {
        this.clientSocket = socket;
        this.biddingController = biddingController;
    }

    @Override
    public void run() {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                Object input = in.readObject();

                if (input instanceof BidRequest) {
                    BidRequest request = (BidRequest) input;

                    SocketServer.joinRoom(request.getAuctionId(), out);

                    BidResponse response = biddingController.handleBid(request);

                    out.writeObject(response);
                    out.flush();

                    if (response.isSuccess()) {
                        PriceUpdateNotification notice = new PriceUpdateNotification(
                                request.getAuctionId(),
                                response.getCurrentPrice(),
                                "Giá sản phẩm đã được cập nhật!"
                        );
                        SocketServer.broadcastToRoom(request.getAuctionId(), notice);
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
                logger.error("Lỗi không mong muốn: {}", e.getMessage(), e);
            }
        }
    }
}