package com.auction.client.model;

import javafx.beans.property.*;

public class AuctionSession {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty productName = new SimpleStringProperty();
    private final DoubleProperty startingPrice = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty();

    public AuctionSession(int id, String productName, double startingPrice, String status) {
        this.id.set(id);
        this.productName.set(productName);
        this.startingPrice.set(startingPrice);
        this.status.set(status);
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty productNameProperty() { return productName; }
    public DoubleProperty startingPriceProperty() { return startingPrice; }
    public StringProperty statusProperty() { return status; }
    public int getId() { return id.get(); }
}