package com.auction.server.pattern.observer;

/**
 * Interface for Subject (maintains a list of Observers)
 */
public interface Subject {
    void addObserver(Observer o);
    void removeObserver(Observer o);
    void notifyObservers(String eventType, Object data);
}
