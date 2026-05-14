package com.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class AuctionPageControllerTest {

    private AuctionPageController controller;
    private Scene scene;

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/AuctionPage.fxml"));
        scene = new Scene(loader.load(), 1280, 800);
        controller = loader.getController();
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testLayoutRatio() {
        // Kiểm tra xem layout chính có sử dụng GridPane với tỷ lệ 60/40 không
        GridPane mainGrid = (GridPane) scene.lookup("#mainContentGrid"); 
        
        assertNotNull(mainGrid, "Main layout should be a GridPane with id #mainContentGrid");
        assertEquals(2, mainGrid.getColumnConstraints().size());
        
        ColumnConstraints col0 = mainGrid.getColumnConstraints().get(0);
        ColumnConstraints col1 = mainGrid.getColumnConstraints().get(1);
        
        assertEquals(60.0, col0.getPercentWidth(), 0.1, "Left column should be 60%");
        assertEquals(40.0, col1.getPercentWidth(), 0.1, "Right column should be 40%");
    }

    @Test
    public void testCurrencyPrefixFormatting() throws InterruptedException {
        // Mock data
        JSONObject session = new JSONObject();
        session.put("id", 1);
        session.put("currentPrice", new BigDecimal("1500000"));
        session.put("startPrice", new BigDecimal("1000000"));
        session.put("endTime", "2026-12-31T23:59:59");

        JSONObject item = new JSONObject();
        item.put("name", "Test Premium Product");
        item.put("imagePath", "");

        // Gọi setItem trên JavaFX thread
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            controller.setItem(session, item);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Verify Current Price (₫ 1,500,000)
        FxAssert.verifyThat("#currentPriceLabel", LabeledMatchers.hasText("₫ 1.500.000"));
        
        // Verify Start Price (₫ 1,000,000)
        FxAssert.verifyThat("#startPriceLabel", LabeledMatchers.hasText("₫ 1.000.000"));
    }

    @Test
    public void testResponsiveFontScaling() {
        // Kiểm tra xem các label tiêu đề đã được gán fx:id đúng chưa
        assertNotNull(scene.lookup("#endingInTitleLabel"));
        assertNotNull(scene.lookup("#startPriceTitleLabel"));
        assertNotNull(scene.lookup("#highestBidTitleLabel"));

        // Giả lập thay đổi kích thước cửa sổ (Width = 800)
        Platform.runLater(() -> {
            scene.getWindow().setWidth(800);
            // Logic updateResponsiveFonts sẽ tự động chạy do listener
        });

        // Chờ một chút để UI update
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        Label currentPrice = (Label) scene.lookup("#currentPriceLabel");
        String style = currentPrice.getStyle();
        assertTrue(style.contains("-fx-font-size:"), "Style should contain dynamic font size");
        
        // Kiểm tra xem font size có nhỏ hơn mức tối đa (48px) khi ở 800px width không
        // BaseFont at 800px width = 800 * 0.034 = 27.2px
        assertTrue(style.contains("27px") || style.contains("26px") || style.contains("28px"), 
            "Font size should scale down to around 27px at 800px width. Current style: " + style);
    }

    @Test
    public void testNavigationButtons() {
        // Kiểm tra sự tồn tại của 2 nút quay về Main (Logo và Dashboard sidebar)
        assertNotNull(scene.lookup("#logoBtn"), "Logo button (BidPop) should exist");
        assertNotNull(scene.lookup("#dashboardBtn"), "Dashboard sidebar button should exist");
        
        // Kiểm tra text của Logo
        javafx.scene.control.Button logoBtn = (javafx.scene.control.Button) scene.lookup("#logoBtn");
        assertEquals("BidPop", logoBtn.getText());
        
        // Kiểm tra xem nút Dashboard có action không
        javafx.scene.control.Button dashboardBtn = (javafx.scene.control.Button) scene.lookup("#dashboardBtn");
        assertNotNull(dashboardBtn.getOnAction(), "Dashboard button should have an onAction handler");
    }

    @Test
    public void testSidebarToggle(FxRobot robot) {
        javafx.scene.layout.VBox sideBar = (javafx.scene.layout.VBox) scene.lookup("#sideBar");
        javafx.scene.control.Label dashboardText = (javafx.scene.control.Label) scene.lookup("#dashboardText");
        
        // Ban đầu sidebar thu gọn (70px)
        assertEquals(70.0, sideBar.getPrefWidth(), 0.1);
        assertFalse(dashboardText.isVisible());

        // Click Hamburger
        robot.clickOn("#hamburgerBtn");
        
        // Verify đã mở rộng (200px) và hiện chữ
        assertEquals(200.0, sideBar.getPrefWidth(), 0.1);
        assertTrue(dashboardText.isVisible());

        // Click lần nữa để thu gọn
        robot.clickOn("#hamburgerBtn");
        assertEquals(70.0, sideBar.getPrefWidth(), 0.1);
        assertFalse(dashboardText.isVisible());
    }
}
