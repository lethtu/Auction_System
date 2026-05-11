package com.auction.client.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.math.BigDecimal;

public class PendingSessionRow {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty productName;
    private final ObjectProperty<BigDecimal> startingPrice;

    public PendingSessionRow(int id, String productName, BigDecimal startingPrice) {
        this.id = new SimpleIntegerProperty(id);
        this.productName = new SimpleStringProperty(productName);
        this.startingPrice = new SimpleObjectProperty<>(startingPrice);
    }

    public int getId() {
        return id.get();
    }

    public String getProductName() {
        return productName.get();
    }

    public BigDecimal getStartingPrice() {
        return startingPrice.get();
    }

    public SimpleIntegerProperty idProperty() {
        return id;
    }

    public SimpleStringProperty productNameProperty() {
        return productName;
    }

    public ObjectProperty<BigDecimal> startingPriceProperty() {
        return startingPrice;
    }
}