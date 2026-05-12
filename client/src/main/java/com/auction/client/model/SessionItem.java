package com.auction.client.model;

import java.math.BigDecimal;

public class SessionItem {
    public int id;
    public String productName = "";
    public String productType = "";
    public String description = "";
    public BigDecimal startingPrice = BigDecimal.ZERO;
    public BigDecimal currentPrice = BigDecimal.ZERO;
    public BigDecimal stepPrice = BigDecimal.ZERO;
    public String endTime = "";
    public String status = "UNKNOWN";

    public String toDisplayText() {
        return "Session #" + id
                + " | " + safe(productName)
                + " | " + safe(status)
                + " | Giá hiện tại: " + safePrice(currentPrice);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Không rõ" : value;
    }

    private String safePrice(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }
}
