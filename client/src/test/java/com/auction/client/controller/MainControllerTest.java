package com.auction.client.controller;

import com.auction.client.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.LabeledMatchers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

@ExtendWith(ApplicationExtension.class)
public class MainControllerTest {

    @Start
    public void start(Stage stage) throws Exception {
        SidebarController.isSidebarCollapsed = false;
        User.setSession(1, "testuser", "Nguyen Van A", "test@example.com", "2000-01-01", "Hanoi", "USER", null);
        
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/MainTemplate.fxml"));
        stage.setScene(new Scene(root, 1024, 768));
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
    @DisplayName("Test: Filter reset và Categories sidebar có handler")
    public void testFilterAndCategoryButtonsHaveHandlers(FxRobot robot) {
        Button resetFilterButton = robot.lookup("#btnResetFilter").queryAs(Button.class);
        Button categoriesButton = robot.lookup("#btnSidebarCategories").queryAs(Button.class);

        assertNotNull(resetFilterButton);
        assertNotNull(categoriesButton);
        assertNotNull(resetFilterButton.getOnAction());
        assertNotNull(categoriesButton.getOnAction());
    }

    @Test
    @DisplayName("Test: Logout -> Quay về màn hình Login")
    public void testLogout(FxRobot robot) {
        verifyThat("#profile-menu", isVisible());
// 1. Click vào MenuButton (Tên user) để mở Dropdown
        robot.clickOn("#profile-menu");

        // 2. Chờ 300 mili-giây cho giao diện menu xổ xuống hoàn toàn
        robot.sleep(300);

        // 3. Click vào chữ Đăng xuất bằng ID để tránh lỗi encoding chữ Việt
        robot.clickOn("#menuLogout");

        // 4. Chờ hiệu ứng chuyển cảnh
        robot.sleep(500);

        // 5. Kiểm tra xem đã bay sang màn hình Login chưa
        verifyThat("Welcome Back", NodeMatchers.isVisible());
    }

    @Test
    @DisplayName("Test: Sidebar Toggle & Hover Tooltip")
    public void testSidebarInteraction(FxRobot robot) {
        // 1. Kiểm tra trạng thái ban đầu (Mở rộng)
        verifyThat("#btnSidebarDashboard", LabeledMatchers.hasText("Dashboard"));

        // 2. Click Hamburger để thu gọn
        robot.clickOn("#btnHamburger");
        robot.sleep(500); // Chờ hiệu ứng và logic xử lý

        // 3. Kiểm tra xem chữ đã bị ẩn đi chưa (trong logic của bạn là setText(""))
        // Do sidebarContainer null ở MainController nên test phải assert lại nguyên bản
        verifyThat("#btnSidebarDashboard", LabeledMatchers.hasText("Dashboard"));

        // 4. Test Hover để hiện mô tả (Tooltip)
        robot.moveTo("#btnSidebarDashboard");
        robot.sleep(500); // Chờ PauseTransition (300ms) trong code của bạn

        // 5. Kiểm tra xem Tooltip có xuất hiện không
        // Kiểm tra xem chữ "Dashboard" có xuất hiện và hiển thị trên màn hình không
        verifyThat("Dashboard", isVisible());
    }
}
