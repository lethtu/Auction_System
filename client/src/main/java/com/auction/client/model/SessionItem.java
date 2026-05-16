package com.auction.client.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class SessionItem {
    private static final Locale MONEY_LOCALE = new Locale("vi", "VN");
    private static final String DEFAULT_TEXT = "Không rõ";
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
    public String endTime = "";
    public String status = DEFAULT_STATUS;

    public String toDisplayText() {
        return toDisplayText(0);
    }

    public String toDisplayText(int displayIndex) {
        return buildPrefix(displayIndex)
                + " | " + normalizeText(productName)
                + " | " + normalizeText(status)
                + " | Giá hiện tại: " + formatPrice(currentPrice)
                + " | Bước giá: " + formatPrice(stepPrice)
                + " | Giá sàn: " + formatPrice(reservePrice);
    }

    private String buildPrefix(int displayIndex) {
        if (displayIndex > 0) {
            return "STT " + displayIndex + " | Mã phiên #" + id;
        }

        return "Mã phiên #" + id;
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