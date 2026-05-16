package com.auction.client.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.math.BigDecimal;

public final class AdminSessionRow {
    private static final String EMPTY_TEXT = "";
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.ZERO;

    private final SimpleIntegerProperty id;
    private final SimpleStringProperty productName;
    private final SimpleStringProperty sellerUsername;
    private final ObjectProperty<BigDecimal> startingPrice;
    private final SimpleStringProperty status;

    public AdminSessionRow(
            int id,
            String productName,
            String sellerUsername,
            BigDecimal startingPrice,
            String status
    ) {
        this.id = new SimpleIntegerProperty(id);
        this.productName = new SimpleStringProperty(safeText(productName));
        this.sellerUsername = new SimpleStringProperty(safeText(sellerUsername));
        this.startingPrice = new SimpleObjectProperty<>(safePrice(startingPrice));
        this.status = new SimpleStringProperty(safeText(status));
    }

    public int getId() {
        return id.get();
    }

    public String getProductName() {
        return productName.get();
    }

    public String getSellerUsername() {
        return sellerUsername.get();
    }

    public BigDecimal getStartingPrice() {
        return startingPrice.get();
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

    private static String safeText(String value) {
        return value == null ? EMPTY_TEXT : value;
    }

    private static BigDecimal safePrice(BigDecimal value) {
        return value == null ? DEFAULT_PRICE : value;
    }
}