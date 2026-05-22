package com.auction.server.pattern.observer;

/**
 * Interface for the Observer pattern (to receive realtime notifications)
 */
public interface Observer {
    void update(String eventType, Object data);
}
