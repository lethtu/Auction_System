package com.auction.server.pattern.observer;

/**
 * Interface cho Subject (chứa danh sách các Observer)
 */
public interface Subject {
    void addObserver(Observer o);
    void removeObserver(Observer o);
    void notifyObservers(String eventType, Object data);
}
