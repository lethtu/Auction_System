package com.auction.client.model;

import java.math.BigDecimal;

public class SessionItem {
    public int id;
    public String productName;
    public String productType;
    public String imageUrl;
    public String description;
    public BigDecimal startingPrice = BigDecimal.ZERO;
    public BigDecimal currentPrice = BigDecimal.ZERO;
    public BigDecimal stepPrice = BigDecimal.ZERO;
    public String endTime;
    public String status;

    public String toDisplayText() {
        return "Session #" + id + " | " + productName + " | " + status;
    }
}