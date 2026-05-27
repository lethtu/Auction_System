package com.auction.client.model;

import com.auction.client.util.MoneyFormatUtil;

import java.math.BigDecimal;

public class SessionItem {
    private static final String DEFAULT_TEXT = "Unknown";
    private static final String DEFAULT_STATUS = "UNKNOWN";


    public int id;
    public String productName = "";
    public String productType = "";
    public String description = "";
    public String imagePath = "";
    public BigDecimal startingPrice = BigDecimal.ZERO;
    public BigDecimal currentPrice = BigDecimal.ZERO;
    public BigDecimal stepPrice = BigDecimal.ZERO;
    public BigDecimal reservePrice = BigDecimal.ZERO;
    public Integer highestBidderId;
    public String startTime = "";
    public String endTime = "";
    public String status = DEFAULT_STATUS;
    public boolean applyMinRate;
    public BigDecimal minRate;

    public String toDisplayText() {
        return toDisplayText(0);
    }

    public String toDisplayText(int displayIndex) {
        String minRateInfo = applyMinRate && minRate != null ? " | MinRate: " + formatPrice(minRate) : "";
        return buildPrefix(displayIndex)
                + " | " + normalizeText(productName)
                + minRateInfo
                + " | " + normalizeText(status)
                + " | Current Price: " + formatPrice(currentPrice)
                + " | Step Price: " + formatPrice(stepPrice)
                + " | Reserve Price: " + formatPrice(reservePrice);
    }

    private String buildPrefix(int displayIndex) {
        if (displayIndex > 0) {
            return "Index " + displayIndex + " | Session #" + id;
        }

        return "Session #" + id;
    }

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? DEFAULT_TEXT : value;
    }

    private static String formatPrice(BigDecimal value) {
        return MoneyFormatUtil.formatVndCode(value);
    }
}