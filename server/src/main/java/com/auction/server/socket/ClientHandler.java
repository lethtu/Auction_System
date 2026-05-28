package com.auction.server.socket;

import com.auction.server.controller.BiddingController;
import com.auction.server.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket clientSocket;
    private final BiddingController biddingController;
    private final SessionManager sessionManager;
    private PrintWriter out;

    public ClientHandler(Socket socket, BiddingController biddingController, SessionManager sessionManager) {
        this.clientSocket = socket;
        this.biddingController = biddingController;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        SocketClient socketClient = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            socketClient = new TcpSocketClient(out);
            SocketMessageProcessor processor = new SocketMessageProcessor(biddingController, sessionManager, socketClient);
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                logger.info("Received inputLine from client: {}", inputLine);
                processor.processMessage(inputLine);
            }

        } catch (IOException e) {
            if (e instanceof java.net.SocketException) {
                System.out.println("Client disconnected: " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        } finally {
            if (socketClient != null) {
                SocketServer.removeFromAllRooms(socketClient);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
