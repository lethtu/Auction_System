package com.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class AuctionPageControllerTest {

    private AuctionPageController controller;
    private Scene scene;

    @Start
    public void start(Stage stage) throws Exception {
        SidebarController.isSidebarCollapsed = false;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/AuctionPage.fxml"));
        scene = new Scene(loader.load(), 1280, 800);
        controller = loader.getController();

        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testLayoutRatio() {
        GridPane mainGrid = (GridPane) scene.lookup("#mainContentGrid");

        assertNotNull(mainGrid, "Main layout should be a GridPane with id #mainContentGrid");
        assertEquals(2, mainGrid.getColumnConstraints().size());

        ColumnConstraints leftColumn = mainGrid.getColumnConstraints().get(0);
        ColumnConstraints rightColumn = mainGrid.getColumnConstraints().get(1);

        assertEquals(60.0, leftColumn.getPercentWidth(), 0.1, "Left column should be 60%");
        assertEquals(40.0, rightColumn.getPercentWidth(), 0.1, "Right column should be 40%");
    }

    @Test
    public void testCurrencyPrefixFormatting() throws Exception {
        JSONObject session = new JSONObject();
        session.put("id", 1);
        session.put("currentPrice", new BigDecimal("1500000"));
        session.put("startPrice", new BigDecimal("1000000"));
        session.put("endTime", "2026-12-31T23:59:59");

        JSONObject item = new JSONObject();
        item.put("name", "Test Premium Product");
        item.put("imagePath", "");

        runOnFxThread(() -> controller.setItem(session, item));

        FxAssert.verifyThat("#currentPriceLabel", LabeledMatchers.hasText("₫ 1.500.000"));
        FxAssert.verifyThat("#startPriceLabel", LabeledMatchers.hasText("₫ 1.000.000"));
    }

    @Test
    public void testResponsiveFontScaling() throws Exception {
        assertNotNull(scene.lookup("#endingInTitleLabel"));
        assertNotNull(scene.lookup("#startPriceTitleLabel"));
        assertNotNull(scene.lookup("#highestBidTitleLabel"));

        runOnFxThread(() -> invokeUpdateResponsiveFonts(800.0));

        Label currentPrice = (Label) scene.lookup("#currentPriceLabel");
        String style = currentPrice.getStyle();

        assertTrue(style.contains("-fx-font-size:"), "Style should contain dynamic font size");
        assertTrue(
                style.contains("-fx-font-size: 27px;"),
                "Font size should scale down to 27px at 800px width. Current style: " + style
        );
    }

    @Test
    public void testNavigationButtons() {
        Label logoBrand = (Label) scene.lookup("#logoBrand");
        Button dashboardBtn = (Button) scene.lookup("#btnSidebarDashboard");

        assertNotNull(logoBrand, "Clickable BidPop brand label should exist");
        assertNotNull(dashboardBtn, "Dashboard sidebar button should exist");

        assertEquals("BidPop", logoBrand.getText());
        assertNotNull(logoBrand.getOnMouseClicked(), "BidPop brand should navigate to the dashboard");
        assertNotNull(dashboardBtn.getOnAction(), "Dashboard button should have an onAction handler");
    }

    @Test
    public void testSidebarToggle() throws Exception {
        javafx.scene.control.ScrollPane sideBar = (javafx.scene.control.ScrollPane) scene.lookup("#sidebarContainer");
        Button hamburgerBtn = (Button) scene.lookup("#btnHamburger");
        Button dashboardBtn = (Button) scene.lookup("#btnSidebarDashboard");

        assertNotNull(sideBar);
        assertNotNull(hamburgerBtn);
        assertNotNull(dashboardBtn);

        assertEquals(200.0, sideBar.getPrefWidth(), 0.1);
        assertEquals("Dashboard", dashboardBtn.getText());

        runOnFxThread(hamburgerBtn::fire);

        assertEquals(70.0, sideBar.getPrefWidth(), 0.1);
        assertEquals("", dashboardBtn.getText());

        runOnFxThread(hamburgerBtn::fire);

        assertEquals(200.0, sideBar.getPrefWidth(), 0.1);
        assertEquals("Dashboard", dashboardBtn.getText());
    }

    private void invokeUpdateResponsiveFonts(double width) {
        try {
            Method method = AuctionPageController.class.getDeclaredMethod("updateResponsiveFonts", double.class);
            method.setAccessible(true);
            method.invoke(controller, width);
        } catch (Exception e) {
            throw new RuntimeException("Cannot invoke updateResponsiveFonts", e);
        }
    }

    private void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX action timed out");

        if (error.get() != null) {
            throw new AssertionError("JavaFX action failed", error.get());
        }
    }
}
