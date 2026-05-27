package com.auction.client.controller;

import com.auction.client.model.User;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

@ExtendWith(ApplicationExtension.class)
public class MainControllerTest {

    @Start
    public void start(Stage stage) throws Exception {
        java.util.prefs.Preferences.userNodeForPackage(com.auction.client.util.SettingsDialog.class)
                .putBoolean(com.auction.client.util.SettingsDialog.KEY_AUTO_COLLAPSE, false);
        try {
            java.lang.reflect.Field field = SidebarController.class.getDeclaredField("preferenceLoaded");
            field.setAccessible(true);
            field.setBoolean(null, false);
        } catch (Exception ignored) {
        }

        SidebarController.isSidebarCollapsed = false;
        User.setSession(1, "testuser", "Nguyen Van A", "test@example.com", "2000-01-01", "Hanoi", "USER", null);

        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/MainTemplate.fxml"));
        stage.setScene(new Scene(root, 1200, 768));
        stage.show();
    }

    @Test
    public void should_have_product_container(FxRobot robot) {
        verifyThat("#productContainer", isVisible());
    }

    @Test
    public void should_have_search_field(FxRobot robot) {
        verifyThat("#txtSearch", isVisible());
    }

    @Test
    @DisplayName("Test: Filter reset and Categories sidebar handlers are wired")
    public void testFilterAndCategoryButtonsHaveHandlers(FxRobot robot) {
        Button resetFilterButton = robot.lookup("#btnResetFilter").queryAs(Button.class);
        Button categoriesButton = robot.lookup("#btnSidebarCategories").queryAs(Button.class);

        assertNotNull(resetFilterButton);
        assertNotNull(categoriesButton);
        assertNotNull(resetFilterButton.getOnAction());
        assertNotNull(categoriesButton.getOnAction());
    }

    @Test
    @DisplayName("Test: Logout returns to Login screen")
    public void testLogout(FxRobot robot) {
        verifyThat("#profile-menu", isVisible());

        Platform.runLater(() -> {
            javafx.scene.control.MenuButton menuButton = robot.lookup("#profile-menu").queryAs(javafx.scene.control.MenuButton.class);
            menuButton.getItems().stream()
                    .filter(item -> "Logout".equals(item.getText()))
                    .findFirst()
                    .ifPresent(javafx.scene.control.MenuItem::fire);
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.sleep(2000);

        verifyThat("Welcome Back", NodeMatchers.isVisible());
    }

    @Test
    @DisplayName("Test: Sidebar and hamburger controls are wired")
    public void testSidebarInteraction(FxRobot robot) {
        Button hamburgerButton = robot.lookup("#btnHamburger").queryAs(Button.class);
        Button dashboardButton = robot.lookup("#btnSidebarDashboard").queryAs(Button.class);

        assertNotNull(hamburgerButton);
        assertNotNull(dashboardButton);
        assertNotNull(hamburgerButton.getOnAction());

        verifyThat("#btnSidebarDashboard", LabeledMatchers.hasText("Dashboard"));

        Platform.runLater(hamburgerButton::fire);
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(dashboardButton.isVisible(), "Dashboard sidebar button should remain visible after hamburger action.");
        assertTrue(dashboardButton.isManaged(), "Dashboard sidebar button should remain managed after hamburger action.");
        assertNotNull(dashboardButton.getText(), "Dashboard sidebar text should never be null.");
    }
}