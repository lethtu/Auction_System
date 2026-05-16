package com.auction.client.util;

import com.auction.client.dto.CreateAuctionRequest;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputControl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class SellerAuctionFormBuilder {

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
            TextInputControl endTimeField
    ) {
        return buildRequest(
                sellerId,
                productTypeCombo,
                productNameField,
                descriptionArea,
                imagePathField,
                startingPriceField,
                stepPriceField,
                null,
                endTimeField
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
            TextInputControl endTimeField
    ) {
        return buildRequest(
                sellerId,
                productTypeCombo,
                productNameField,
                descriptionArea,
                imagePathField,
                startingPriceField,
                stepPriceField,
                null,
                endTimeField
        );
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
                startTime,
                endTime
        );
    }

    public static CreateAuctionRequest buildCreateRequest(
            int sellerId,
            ComboBox<String> productTypeCombo,
            TextInputControl productNameField,
            TextInputControl descriptionArea,
            TextInputControl imagePathField,
            TextInputControl startingPriceField,
            TextInputControl stepPriceField,
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
                null,
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
                null,
                startTime,
                endTime
        );
    }

    public static void fillDefaultEndTime(TextInputControl endTimeField) {
        if (endTimeField != null && endTimeField.getText().trim().isEmpty()) {
            endTimeField.setText(defaultEndTime());
        }
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
            TextInputControl endTimeField
    ) {
        String endTime = textOrEmpty(endTimeField);

        if (endTime.isEmpty()) {
            endTime = defaultEndTime();

            if (endTimeField != null) {
                endTimeField.setText(endTime);
            }
        }

        return buildRequest(
                sellerId,
                productTypeCombo,
                productNameField,
                descriptionArea,
                imagePathField,
                startingPriceField,
                stepPriceField,
                reservePriceField,
                defaultStartTime(),
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
            String startTimeInput,
            String endTimeInput
    ) {
        String productName = textOrEmpty(productNameField);
        String productType = productTypeCombo == null ? null : productTypeCombo.getValue();
        String description = textOrEmpty(descriptionArea);
        String imagePath = textOrEmpty(imagePathField);
        String startingPriceText = textOrEmpty(startingPriceField);
        String stepPriceText = textOrEmpty(stepPriceField);
        String reservePriceText = textOrEmpty(reservePriceField);
        String startTime = startTimeInput == null ? "" : startTimeInput.trim();
        String endTime = endTimeInput == null ? "" : endTimeInput.trim();

        if (isFormInvalid(productName, productType, startingPriceText, stepPriceText)) {
            AlertUtil.show(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại, giá khởi điểm và bước giá.");
            return null;
        }

        BigDecimal startingPrice = parsePositiveMoney(startingPriceText, "Giá khởi điểm phải lớn hơn 0.");
        BigDecimal stepPrice = parsePositiveMoney(stepPriceText, "Bước giá phải lớn hơn 0.");
        BigDecimal reservePrice = parseOptionalReservePrice(reservePriceText, startingPrice);

        if (startTime.isEmpty()) {
            startTime = defaultStartTime();
        }

        if (endTime.isEmpty()) {
            endTime = defaultEndTime();
        }

        return new CreateAuctionRequest(
                productName,
                productType.trim(),
                description,
                imagePath,
                startingPrice,
                stepPrice,
                reservePrice,
                startTime,
                endTime,
                sellerId
        );
    }

    private static boolean isFormInvalid(
            String productName,
            String productType,
            String startingPriceText,
            String stepPriceText
    ) {
        return productName.isEmpty()
                || productType == null
                || productType.trim().isEmpty()
                || startingPriceText.isEmpty()
                || stepPriceText.isEmpty();
    }

    private static BigDecimal parsePositiveMoney(String value, String message) {
        try {
            BigDecimal money = new BigDecimal(normalizeMoneyText(value));

            if (money.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(message);
            }

            return money;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá khởi điểm, bước giá và giá sàn phải là số hợp lệ.");
        }
    }

    private static BigDecimal parseOptionalReservePrice(String value, BigDecimal startingPrice) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        BigDecimal reservePrice = parsePositiveMoney(value, "Giá sàn phải lớn hơn 0.");

        if (reservePrice.compareTo(startingPrice) < 0) {
            throw new IllegalArgumentException("Giá sàn không được nhỏ hơn giá khởi điểm.");
        }

        return reservePrice;
    }

    private static String normalizeMoneyText(String value) {
        return value == null ? "" : value.trim().replace("₫", "").replace("đ", "").replace(" ", "").replace(".", "").replace(",", "");
    }

    private static String defaultStartTime() {
        return LocalDateTime.now().plusMinutes(5).withSecond(0).withNano(0).toString();
    }

    private static String defaultEndTime() {
        return LocalDateTime.now().plusDays(7).withSecond(0).withNano(0).toString();
    }

    private static String textOrEmpty(TextInputControl input) {
        return input == null ? "" : input.getText().trim();
    }
}
