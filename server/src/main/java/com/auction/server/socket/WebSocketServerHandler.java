package com.auction.server.socket;

import com.auction.server.controller.BiddingController;
import com.auction.server.util.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WebSocketServerHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

    private final BiddingController biddingController;
    private final SessionManager sessionManager;

    @Autowired
    public WebSocketServerHandler(BiddingController biddingController, SessionManager sessionManager) {
        this.biddingController = biddingController;
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
        WebSocketClient client = new WebSocketClient(session);
        SocketMessageProcessor processor = new SocketMessageProcessor(biddingController, sessionManager, client);
        session.getAttributes().put("processor", processor);
        session.getAttributes().put("client", client);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.info("Received WebSocket message: {}", payload);
        SocketMessageProcessor processor = (SocketMessageProcessor) session.getAttributes().get("processor");
        if (processor != null) {
            processor.processMessage(payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket connection closed: {}", session.getId());
        WebSocketClient client = (WebSocketClient) session.getAttributes().get("client");
        if (client != null) {
            SocketServer.removeFromAllRooms(client);
        }
    }
}
