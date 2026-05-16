package com.auction.client.util;

import com.auction.client.dto.CreateAuctionRequest;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class SellerAuctionFormBuilderTest {

    private static final int SELLER_ID = 7;
    private static final String START_TIME = "2026-05-12T10:00:00";
    private static final String END_TIME = "2026-05-20T10:00:00";

    @Start
    void start(Stage stage) {
    }

    @Test
    void buildCreateRequest_validInput_normalizesMoneyAndCopiesValues() {
        CreateAuctionRequest request = SellerAuctionFormBuilder.buildCreateRequest(
                SELLER_ID,
                combo("Electronics"),
                textField(" Laptop Gaming "),
                textArea(" Máy còn tốt "),
                textField(" upload/images/laptop.png "),
                textField("1.000.000 đ"),
                textField("100.000"),
                textField("1.500.000"),
                START_TIME,
                END_TIME
        );

        assertEquals("Laptop Gaming", request.productName);
        assertEquals("Electronics", request.productType);
        assertEquals("Máy còn tốt", request.description);
        assertEquals("upload/images/laptop.png", request.imagePath);
        assertEquals(new BigDecimal("1000000"), request.startingPrice);
        assertEquals(new BigDecimal("100000"), request.stepPrice);
        assertEquals(new BigDecimal("1500000"), request.reservePrice);
        assertEquals(START_TIME, request.startTime);
        assertEquals(END_TIME, request.endTime);
        assertEquals(SELLER_ID, request.sellerId);
        assertTrue(request.hasImagePath());
        assertTrue(request.hasReservePrice());
    }

    @Test
    void buildCreateRequest_blankReservePrice_allowsNoReservePrice() {
        CreateAuctionRequest request = validRequestWithReservePrice("");

        assertNull(request.reservePrice);
        assertFalse(request.hasReservePrice());
    }

    @Test
    void buildCreateRequest_blankStartAndEndTime_usesDefaultTimes() {
        CreateAuctionRequest request = SellerAuctionFormBuilder.buildCreateRequest(
                SELLER_ID,
                combo("Art"),
                textField("Tranh"),
                textArea("Đẹp"),
                textField(""),
                textField("1000000"),
                textField("100000"),
                textField(""),
                "",
                ""
        );

        assertNotNull(request.startTime);
        assertNotNull(request.endTime);
        assertFalse(request.startTime.isBlank());
        assertFalse(request.endTime.isBlank());
    }

    @Test
    void buildCreateRequest_missingRequiredFields_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SellerAuctionFormBuilder.buildCreateRequest(
                        SELLER_ID,
                        combo("Electronics"),
                        textField(" "),
                        textArea("Mô tả"),
                        textField(""),
                        textField("1000000"),
                        textField("100000"),
                        textField(""),
                        START_TIME,
                        END_TIME
                )
        );

        assertEquals("Vui lòng nhập tên sản phẩm, loại, giá khởi điểm và bước giá.", ex.getMessage());
    }

    @Test
    void buildCreateRequest_invalidMoney_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SellerAuctionFormBuilder.buildCreateRequest(
                        SELLER_ID,
                        combo("Electronics"),
                        textField("Laptop"),
                        textArea("Mô tả"),
                        textField(""),
                        textField("abc"),
                        textField("100000"),
                        textField(""),
                        START_TIME,
                        END_TIME
                )
        );

        assertEquals("Giá khởi điểm, bước giá và giá sàn phải là số hợp lệ.", ex.getMessage());
    }

    @Test
    void buildCreateRequest_zeroStartingPrice_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SellerAuctionFormBuilder.buildCreateRequest(
                        SELLER_ID,
                        combo("Electronics"),
                        textField("Laptop"),
                        textArea("Mô tả"),
                        textField(""),
                        textField("0"),
                        textField("100000"),
                        textField(""),
                        START_TIME,
                        END_TIME
                )
        );

        assertEquals("Giá khởi điểm phải lớn hơn 0.", ex.getMessage());
    }

    @Test
    void buildCreateRequest_zeroStepPrice_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SellerAuctionFormBuilder.buildCreateRequest(
                        SELLER_ID,
                        combo("Electronics"),
                        textField("Laptop"),
                        textArea("Mô tả"),
                        textField(""),
                        textField("1000000"),
                        textField("0"),
                        textField(""),
                        START_TIME,
                        END_TIME
                )
        );

        assertEquals("Bước giá phải lớn hơn 0.", ex.getMessage());
    }

    @Test
    void buildCreateRequest_reservePriceBelowStartingPrice_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> validRequestWithReservePrice("999999")
        );

        assertEquals("Giá sàn không được nhỏ hơn giá khởi điểm.", ex.getMessage());
    }

    private CreateAuctionRequest validRequestWithReservePrice(String reservePrice) {
        return SellerAuctionFormBuilder.buildCreateRequest(
                SELLER_ID,
                combo("Electronics"),
                textField("Laptop"),
                textArea("Mô tả"),
                textField(""),
                textField("1000000"),
                textField("100000"),
                textField(reservePrice),
                START_TIME,
                END_TIME
        );
    }

    private ComboBox<String> combo(String value) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setValue(value);
        return comboBox;
    }

    private TextField textField(String value) {
        TextField textField = new TextField();
        textField.setText(value);
        return textField;
    }

    private TextArea textArea(String value) {
        TextArea textArea = new TextArea();
        textArea.setText(value);
        return textArea;
    }
}