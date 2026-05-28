package com.auction.server.socket;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

public class WebSocketClient implements SocketClient {
    private final WebSocketSession session;

    public WebSocketClient(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public void sendMessage(String message) {
        try {
            if (session != null && session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketClient that = (WebSocketClient) o;
        return java.util.Objects.equals(session, that.session);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(session);
    }
}
