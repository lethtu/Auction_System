package com.auction.client.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class SessionItem {
    private static final Locale MONEY_LOCALE = new Locale.Builder().setLanguage("vi").setRegion("VN").build();
    private static final String DEFAULT_TEXT = "Unknown";
    private static final String DEFAULT_STATUS = "UNKNOWN";
    private static final String CURRENCY_SUFFIX = " VND";

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
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return createMoneyFormat().format(safeValue) + CURRENCY_SUFFIX;
    }

    private static NumberFormat createMoneyFormat() {
        NumberFormat format = NumberFormat.getNumberInstance(MONEY_LOCALE);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format;
    }
}