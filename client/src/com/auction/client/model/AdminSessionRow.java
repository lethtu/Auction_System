package com.auction.client.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.math.BigDecimal;

public class AdminSessionRow {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty productName;
    private final SimpleStringProperty sellerUsername;
    private final ObjectProperty<BigDecimal> startingPrice;
    private final SimpleStringProperty status;

    public AdminSessionRow(int id, String productName, String sellerUsername, BigDecimal startingPrice, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.productName = new SimpleStringProperty(productName);
        this.sellerUsername = new SimpleStringProperty(sellerUsername);
        this.startingPrice = new SimpleObjectProperty<>(startingPrice);
        this.status = new SimpleStringProperty(status);
    }

    public int getId() {
        return id.get();
    }

    public String getStatus() {
        return status.get();
    }

    public SimpleIntegerProperty idProperty() {
        return id;
    }

    public SimpleStringProperty productNameProperty() {
        return productName;
    }

    public SimpleStringProperty sellerUsernameProperty() {
        return sellerUsername;
    }

    public ObjectProperty<BigDecimal> startingPriceProperty() {
        return startingPrice;
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }
}
