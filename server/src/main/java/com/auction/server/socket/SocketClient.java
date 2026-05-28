package com.auction.server.socket;

public interface SocketClient {
    void sendMessage(String message);
    boolean isOpen();
}
