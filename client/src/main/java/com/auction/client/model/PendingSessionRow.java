package com.auction.client.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.math.BigDecimal;

public final class PendingSessionRow {
    private static final String EMPTY_TEXT = "";
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.ZERO;

    private final SimpleIntegerProperty id;
    private final SimpleStringProperty productName;
    private final ObjectProperty<BigDecimal> startingPrice;

    public PendingSessionRow(int id, String productName, BigDecimal startingPrice) {
        this.id = new SimpleIntegerProperty(id);
        this.productName = new SimpleStringProperty(safeText(productName));
        this.startingPrice = new SimpleObjectProperty<>(safePrice(startingPrice));
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

    private static String safeText(String value) {
        return value == null ? EMPTY_TEXT : value;
    }

    private static BigDecimal safePrice(BigDecimal value) {
        return value == null ? DEFAULT_PRICE : value;
    }
}