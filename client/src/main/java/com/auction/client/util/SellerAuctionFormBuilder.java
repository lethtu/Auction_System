package com.auction.client.util;

import com.auction.client.dto.CreateAuctionRequest;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputControl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class SellerAuctionFormBuilder {
    private static final int DEFAULT_START_DELAY_MINUTES = 5;
    private static final int DEFAULT_DURATION_DAYS = 7;

    private static final String MISSING_REQUIRED_FIELDS_MESSAGE =
            "Vui lòng nhập tên sản phẩm, loại, giá khởi điểm và bước giá.";
    private static final String INVALID_MONEY_MESSAGE =
            "Giá khởi điểm, bước giá và giá sàn phải là số hợp lệ.";
    private static final String INVALID_STARTING_PRICE_MESSAGE = "Giá khởi điểm phải lớn hơn 0.";
    private static final String INVALID_STEP_PRICE_MESSAGE = "Bước giá phải lớn hơn 0.";
    private static final String INVALID_RESERVE_PRICE_MESSAGE = "Giá sàn phải lớn hơn 0.";
    private static final String RESERVE_PRICE_TOO_LOW_MESSAGE = "Giá sàn không được nhỏ hơn giá khởi điểm.";

    private SellerAuctionFormBuilder() {
    }

    public static CreateAuctionRequest buildCreateRequest(
            int sellerId,
            ComboBox<String> productTypeCombo,
            TextInputControl productNameField,
            TextInputControl descriptionArea,
            TextInputControl imagePathField,
            TextInputControl startingPriceField,
            TextInputControl stepPriceField,
            TextInputControl reservePriceField,
            CheckBox applyMinRateCheck,
            TextInputControl minRateField,
            String startTime,
            String endTime
    ) {
        return buildRequest(
                sellerId,
                productTypeCombo,
                productNameField,
                descriptionArea,
                imagePathField,
                startingPriceField,
                stepPriceField,
                reservePriceField,
                applyMinRateCheck,
                minRateField,
                startTime,
                endTime
        );
    }

    public static CreateAuctionRequest buildUpdateRequest(
            int sellerId,
            ComboBox<String> productTypeCombo,
            TextInputControl productNameField,
            TextInputControl descriptionArea,
            TextInputControl imagePathField,
            TextInputControl startingPriceField,
            TextInputControl stepPriceField,
            TextInputControl reservePriceField,
            CheckBox applyMinRateCheck,
            TextInputControl minRateField,
            String startTime,
            String endTime
    ) {
        return buildRequest(
                sellerId,
                productTypeCombo,
                productNameField,
                descriptionArea,
                imagePathField,
                startingPriceField,
                stepPriceField,
                reservePriceField,
                applyMinRateCheck,
                minRateField,
                startTime,
                endTime
        );
    }

    private static CreateAuctionRequest buildRequest(
            int sellerId,
            ComboBox<String> productTypeCombo,
            TextInputControl productNameField,
            TextInputControl descriptionArea,
            TextInputControl imagePathField,
            TextInputControl startingPriceField,
            TextInputControl stepPriceField,
            TextInputControl reservePriceField,
            CheckBox applyMinRateCheck,
            TextInputControl minRateField,
            String startTimeInput,
            String endTimeInput
    ) {
        AuctionFormValues formValues = readFormValues(
                productTypeCombo,
                productNameField,
                descriptionArea,
                imagePathField,
                startingPriceField,
                stepPriceField,
                reservePriceField,
                applyMinRateCheck,
                minRateField,
                startTimeInput,
                endTimeInput
        );

        validateRequiredFields(formValues);

        BigDecimal startingPrice = parsePositiveMoney(
                formValues.startingPriceText(),
                INVALID_STARTING_PRICE_MESSAGE
        );
        BigDecimal stepPrice = parsePositiveMoney(
                formValues.stepPriceText(),
                INVALID_STEP_PRICE_MESSAGE
        );
        BigDecimal reservePrice = parseOptionalReservePrice(
                formValues.reservePriceText(),
                startingPrice
        );

        Boolean applyMinRate = formValues.applyMinRateCheck() != null && formValues.applyMinRateCheck().isSelected();
        BigDecimal minRate = null;
        if (applyMinRate) {
            if (formValues.minRateText().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập giá tối thiểu (Min rate).");
            }
            minRate = parsePositiveMoney(formValues.minRateText(), "Giá tối thiểu phải là số hợp lệ lớn hơn 0.");
            if (minRate.compareTo(startingPrice) < 0) {
                throw new IllegalArgumentException("Giá tối thiểu phải lớn hơn hoặc bằng giá khởi điểm.");
            }
        }

        return new CreateAuctionRequest(
                formValues.productName(),
                formValues.productType(),
                formValues.description(),
                formValues.imagePath(),
                startingPrice,
                stepPrice,
                reservePrice,
                defaultIfBlank(formValues.startTime(), SellerAuctionFormBuilder::defaultStartTime),
                defaultIfBlank(formValues.endTime(), SellerAuctionFormBuilder::defaultEndTime),
                sellerId,
                applyMinRate,
                minRate
        );
    }

    private static AuctionFormValues readFormValues(
            ComboBox<String> productTypeCombo,
            TextInputControl productNameField,
            TextInputControl descriptionArea,
            TextInputControl imagePathField,
            TextInputControl startingPriceField,
            TextInputControl stepPriceField,
            TextInputControl reservePriceField,
            CheckBox applyMinRateCheck,
            TextInputControl minRateField,
            String startTimeInput,
            String endTimeInput
    ) {
        return new AuctionFormValues(
                textOrEmpty(productNameField),
                selectedValueOrEmpty(productTypeCombo),
                textOrEmpty(descriptionArea),
                textOrEmpty(imagePathField),
                textOrEmpty(startingPriceField),
                textOrEmpty(stepPriceField),
                textOrEmpty(reservePriceField),
                applyMinRateCheck,
                textOrEmpty(minRateField),
                trimOrEmpty(startTimeInput),
                trimOrEmpty(endTimeInput)
        );
    }

    private static void validateRequiredFields(AuctionFormValues formValues) {
        if (formValues.productName().isEmpty()
                || formValues.productType().isEmpty()
                || formValues.startingPriceText().isEmpty()
                || formValues.stepPriceText().isEmpty()) {
            throw new IllegalArgumentException(MISSING_REQUIRED_FIELDS_MESSAGE);
        }
    }

    private static BigDecimal parsePositiveMoney(String value, String message) {
        try {
            BigDecimal money = new BigDecimal(normalizeMoneyText(value));

            if (money.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(message);
            }

            return money;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(INVALID_MONEY_MESSAGE);
        }
    }

    private static BigDecimal parseOptionalReservePrice(String value, BigDecimal startingPrice) {
        if (isBlank(value)) {
            return null;
        }

        BigDecimal reservePrice = parsePositiveMoney(value, INVALID_RESERVE_PRICE_MESSAGE);

        if (reservePrice.compareTo(startingPrice) < 0) {
            throw new IllegalArgumentException(RESERVE_PRICE_TOO_LOW_MESSAGE);
        }

        return reservePrice;
    }

    private static String normalizeMoneyText(String value) {
        return trimOrEmpty(value)
                .replace("₫", "")
                .replace("đ", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", "");
    }

    private static String defaultStartTime() {
        return LocalDateTime.now()
                .plusMinutes(DEFAULT_START_DELAY_MINUTES)
                .withSecond(0)
                .withNano(0)
                .toString();
    }

    private static String defaultEndTime() {
        return LocalDateTime.now()
                .plusDays(DEFAULT_DURATION_DAYS)
                .withSecond(0)
                .withNano(0)
                .toString();
    }

    private static String textOrEmpty(TextInputControl input) {
        return input == null ? "" : trimOrEmpty(input.getText());
    }

    private static String selectedValueOrEmpty(ComboBox<String> comboBox) {
        return comboBox == null ? "" : trimOrEmpty(comboBox.getValue());
    }

    private static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return trimOrEmpty(value).isEmpty();
    }

    private static String defaultIfBlank(String value, DefaultValueSupplier defaultValueSupplier) {
        return isBlank(value) ? defaultValueSupplier.get() : value;
    }

    @FunctionalInterface
    private interface DefaultValueSupplier {
        String get();
    }

    private record AuctionFormValues(
            String productName,
            String productType,
            String description,
            String imagePath,
            String startingPriceText,
            String stepPriceText,
            String reservePriceText,
            CheckBox applyMinRateCheck,
            String minRateText,
            String startTime,
            String endTime
    ) {
    }
}
