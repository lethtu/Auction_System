package com.auction.client.util;

import com.auction.client.dto.CreateAuctionRequest;
import com.auction.client.model.SessionItem;
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
            TextInputControl imageUrlField,
            TextInputControl descriptionArea,
            TextInputControl startingPriceField,
            TextInputControl stepPriceField,
            TextInputControl endTimeField
    ) {
        String productName = textOrEmpty(productNameField);
        String productType = productTypeCombo == null ? null : productTypeCombo.getValue();
        String imageUrl = textOrEmpty(imageUrlField);
        String description = textOrEmpty(descriptionArea);
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
                productType,
                imageUrl,
                description,
                new BigDecimal(startingPriceText.trim()),
                new BigDecimal(stepPriceText.trim()),
                startTime,
                endTime,
                sellerId
        );
    }

    public static CreateAuctionRequest buildUpdateRequest(
            int sellerId,
            SessionItem selected,
            ComboBox<String> productTypeCombo,
            TextInputControl productNameField,
            TextInputControl imageUrlField,
            TextInputControl descriptionArea,
            TextInputControl startingPriceField,
            TextInputControl stepPriceField,
            TextInputControl endTimeField
    ) {
        String productName = valueOrDefault(textOrEmpty(productNameField), selected.productName);
        String productType = valueOrDefault(productTypeCombo == null ? null : productTypeCombo.getValue(), selected.productType);
        String imageUrl = valueOrDefault(textOrEmpty(imageUrlField), selected.imageUrl);
        String description = valueOrDefault(textOrEmpty(descriptionArea), selected.description);
        String startingPriceText = valueOrDefault(textOrEmpty(startingPriceField), bigDecimalText(selected.startingPrice));
        String stepPriceText = valueOrDefault(textOrEmpty(stepPriceField), bigDecimalText(selected.stepPrice));
        String startTime = defaultStartTime();
        String endTime = valueOrDefault(textOrEmpty(endTimeField), selected.endTime);

        if (isFormInvalid(productName, productType, startingPriceText, stepPriceText)) {
            AlertUtil.show(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại, giá khởi điểm và bước giá.");
            return null;
        }

        return new CreateAuctionRequest(
                productName,
                productType,
                imageUrl,
                description,
                new BigDecimal(startingPriceText.trim()),
                new BigDecimal(stepPriceText.trim()),
                startTime,
                endTime,
                sellerId
        );
    }

    public static void fillDefaultEndTime(TextInputControl endTimeField) {
        if (endTimeField != null && endTimeField.getText().trim().isEmpty()) {
            endTimeField.setText(defaultEndTime());
        }
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

    private static String valueOrDefault(String value, String fallback) {
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        return fallback == null ? "" : fallback.trim();
    }

    private static String bigDecimalText(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }
}