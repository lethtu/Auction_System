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
                new BigDecimal(startingPriceText.trim()),
                new BigDecimal(stepPriceText.trim()),
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
