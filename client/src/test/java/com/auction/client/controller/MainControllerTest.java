package com.auction.client.controller;

import com.auction.client.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

@ExtendWith(ApplicationExtension.class)
public class MainControllerTest {

    @Start
    public void start(Stage stage) throws Exception {
        User.setSession(1, "testuser", "Nguyen Van A", "test@example.com", "2000-01-01", "Hanoi", "USER");
        
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/MainTemplate.fxml"));
        stage.setScene(new Scene(root, 1024, 768));
        stage.show();
    }

    @Test
    public void should_show_welcome_message(FxRobot robot) {
        verifyThat("#lblWelcome", LabeledMatchers.hasText("Chào, Nguyen Van A"));
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
    @DisplayName("Test: Logout -> Quay về màn hình Login")
    public void testLogout(FxRobot robot) {
// 1. Click vào MenuButton (Tên user) để mở Dropdown
        robot.clickOn("#profile-menu");

        // 2. Chờ 300 mili-giây cho giao diện menu xổ xuống hoàn toàn
        robot.sleep(300);

        // 3. Click vào chữ Đăng xuất
        robot.clickOn("Đăng Xuất");

        // 4. Chờ hiệu ứng chuyển cảnh
        robot.sleep(500);

        // 5. Kiểm tra xem đã bay sang màn hình Login chưa
        verifyThat("Đăng nhập Hệ thống Đấu giá", NodeMatchers.isVisible());
    }
}
