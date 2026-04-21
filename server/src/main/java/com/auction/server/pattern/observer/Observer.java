package com.auction.server.pattern.observer;

/**
 * Interface cho mẫu thiết kế Observer (để nhận notification realtime)
 */
public interface Observer {
    void update(String eventType, Object data);
}
