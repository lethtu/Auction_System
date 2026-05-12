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
            TextInputControl endTimeField
    ) {
        String productName = textOrEmpty(productNameField);
        String productType = productTypeCombo == null ? null : productTypeCombo.getValue();
        String description = textOrEmpty(descriptionArea);
        String imagePath = textOrEmpty(imagePathField);
        String startingPriceText = textOrEmpty(startingPriceField);
        String stepPriceText = textOrEmpty(stepPriceField);
        String startTime = defaultStartTime();
        String endTime = textOrEmpty(endTimeField);

        if (isFormInvalid(productName, productType, startingPriceText, stepPriceText)) {
            AlertUtil.show(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại, giá khởi điểm và bước giá.");
            return null;
        }

        BigDecimal startingPrice = parsePositiveMoney(startingPriceText, "Giá khởi điểm phải lớn hơn 0.");
        BigDecimal stepPrice = parsePositiveMoney(stepPriceText, "Bước giá phải lớn hơn 0.");

        if (endTime.isEmpty()) {
            endTime = defaultEndTime();

            if (endTimeField != null) {
                endTimeField.setText(endTime);
            }
        }

        return new CreateAuctionRequest(
                productName,
                productType.trim(),
                description,
                imagePath,
                startingPrice,
                stepPrice,
                startTime,
                endTime,
                sellerId
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
            String startTimeInput,
            String endTimeInput
    ) {
        String productName = textOrEmpty(productNameField);
        String productType = productTypeCombo == null ? null : productTypeCombo.getValue();
        String description = textOrEmpty(descriptionArea);
        String imagePath = textOrEmpty(imagePathField);
        String startingPriceText = textOrEmpty(startingPriceField);
        String stepPriceText = textOrEmpty(stepPriceField);
        String startTime = startTimeInput == null ? "" : startTimeInput.trim();
        String endTime = endTimeInput == null ? "" : endTimeInput.trim();

        if (isFormInvalid(productName, productType, startingPriceText, stepPriceText)) {
            AlertUtil.show(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại, giá khởi điểm và bước giá.");
            return null;
        }

        BigDecimal startingPrice = parsePositiveMoney(startingPriceText, "Giá khởi điểm phải lớn hơn 0.");
        BigDecimal stepPrice = parsePositiveMoney(stepPriceText, "Bước giá phải lớn hơn 0.");

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
            BigDecimal money = new BigDecimal(value.trim());

            if (money.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(message);
            }

            return money;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá khởi điểm và bước giá phải là số hợp lệ.");
        }
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
