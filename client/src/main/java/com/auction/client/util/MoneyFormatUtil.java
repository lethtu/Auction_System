package com.auction.client.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyFormatUtil {

    private MoneyFormatUtil() {
    }

    public static String formatGrouped(BigDecimal value) {
        if (value == null) {
            return "0";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');

        DecimalFormat decimalFormat = new DecimalFormat("###,###", symbols);
        return decimalFormat.format(value);
    }

    public static String formatVndSuffix(BigDecimal value) {
        return formatGrouped(value) + " \u20ab";
    }

    public static String formatVndPrefix(BigDecimal value) {
        return "\u20ab " + formatGrouped(value);
    }

    public static String formatVndCode(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        return format.format(safeValue) + " VND";
    }

    public static BigDecimal parseMoneyInput(String raw) {
        String normalized = raw == null
                ? ""
                : raw.trim()
                        .replace("\u20ab", "")
                        .replace("\u0111", "")
                        .replace(" ", "")
                        .replace(".", "")
                        .replace(",", "");

        return new BigDecimal(normalized);
    }
}
