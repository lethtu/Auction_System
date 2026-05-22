package com.auction.client.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.math.BigDecimal;

public final class AdminSessionRow {
    private static final String EMPTY_TEXT = "";
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.ZERO;

    private final SimpleIntegerProperty id;
    private final SimpleIntegerProperty productId;
    private final SimpleStringProperty productName;
    private final SimpleStringProperty sellerUsername;
    private final ObjectProperty<BigDecimal> startingPrice;
    private final ObjectProperty<BigDecimal> currentPrice;
    private final SimpleStringProperty status;
    private final SimpleBooleanProperty productVisible;

    public AdminSessionRow(
            int id,
            String productName,
            String sellerUsername,
            BigDecimal startingPrice,
            String status
    ) {
        this(id, 0, productName, sellerUsername, startingPrice, status, true);
    }

    public AdminSessionRow(
            int id,
            int productId,
            String productName,
            String sellerUsername,
            BigDecimal startingPrice,
            String status,
            boolean productVisible
    ) {
        this(id, productId, productName, sellerUsername, startingPrice, startingPrice, status, productVisible);
    }

    public AdminSessionRow(
            int id,
            int productId,
            String productName,
            String sellerUsername,
            BigDecimal startingPrice,
            BigDecimal currentPrice,
            String status,
            boolean productVisible
    ) {
        this.id = new SimpleIntegerProperty(id);
        this.productId = new SimpleIntegerProperty(productId);
        this.productName = new SimpleStringProperty(safeText(productName));
        this.sellerUsername = new SimpleStringProperty(safeText(sellerUsername));
        this.startingPrice = new SimpleObjectProperty<>(safePrice(startingPrice));
        this.currentPrice = new SimpleObjectProperty<>(safePrice(currentPrice));
        this.status = new SimpleStringProperty(safeText(status));
        this.productVisible = new SimpleBooleanProperty(productVisible);
    }

    public int getId() {
        return id.get();
    }

    public int getProductId() {
        return productId.get();
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

    public BigDecimal getCurrentPrice() {
        return currentPrice.get();
    }

    public String getStatus() {
        return status.get();
    }

    public boolean isProductVisible() {
        return productVisible.get();
    }

    public SimpleIntegerProperty idProperty() {
        return id;
    }

    public SimpleIntegerProperty productIdProperty() {
        return productId;
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

    public ObjectProperty<BigDecimal> currentPriceProperty() {
        return currentPrice;
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    public SimpleBooleanProperty productVisibleProperty() {
        return productVisible;
    }

    private static String safeText(String value) {
        return value == null ? EMPTY_TEXT : value;
    }

    private static BigDecimal safePrice(BigDecimal value) {
        return value == null ? DEFAULT_PRICE : value;
    }
}