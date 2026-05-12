package com.auction.client.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class SessionItem {
    private static final NumberFormat MONEY_FORMAT = createMoneyFormat();

    public int id;
    public String productName = "";
    public String productType = "";
    public String description = "";
    public String imagePath = "";
    public BigDecimal startingPrice = BigDecimal.ZERO;
    public BigDecimal currentPrice = BigDecimal.ZERO;
    public BigDecimal stepPrice = BigDecimal.ZERO;
    public String endTime = "";
    public String status = "UNKNOWN";

    public String toDisplayText() {
        return toDisplayText(0);
    }

    public String toDisplayText(int displayIndex) {
        String prefix = displayIndex > 0
                ? "STT " + displayIndex + " | Mã phiên #" + id
                : "Mã phiên #" + id;

        return prefix
                + " | " + safe(productName)
                + " | " + safe(status)
                + " | Giá hiện tại: " + safePrice(currentPrice);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Không rõ" : value;
    }

    private String safePrice(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return MONEY_FORMAT.format(safeValue) + " VND";
    }

    private static NumberFormat createMoneyFormat() {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format;
    }
}
