package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import org.mockito.Mockito;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.testfx.api.FxAssert.verifyThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class SignUpControllerTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;

    @Start
    public void start(Stage stage) throws Exception {
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = (HttpResponse<String>) Mockito.mock(HttpResponse.class);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/SignUp.fxml"));
        Parent root = loader.load();

        SignUpController controller = loader.getController();
        controller.setHttpClient(mockHttpClient);

        stage.setScene(new Scene(root, 1000, 650));
        stage.show();
        stage.toFront();
    }

    // TEST 1: BỎ TRỐNG TRƯỜNG DỮ LIỆU
    @Test
    @DisplayName("Test: Bỏ trống form -> Hiện cảnh báo")
    public void testSignUp_EmptyFields(FxRobot robot) {
        robot.clickOn("#txtFullName").write("Lê Thanh Tùng");
        robot.clickOn("#btnSignUp");
        verifyThat("Vui lòng điền đầy đủ các trường!", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }

    // TEST 2: SAI MẬT KHẨU XÁC NHẬN

    @Test
    @DisplayName("Test: Mật khẩu xác nhận không khớp -> Hiện cảnh báo")
    public void testSignUp_PasswordMismatch(FxRobot robot) {
        robot.clickOn("#txtFullName").write("Lê Thanh Tùng");
        robot.clickOn("#txtUsername").write("lethtu");
        robot.clickOn("#txtEmail").write("lethtu@gmail.com");
        robot.clickOn("#txtPassword").write("123456");
        robot.clickOn("#txtConfirmPassword").write("Sai_Mat_Khau_Nek");

        robot.clickOn("#chkTerms");
        robot.clickOn("#btnSignUp");

        verifyThat("Mật khẩu xác nhận không khớp!", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }


    // TEST 3: SERVER BÁO TRÙNG TÀI KHOẢN (Đăng ký thất bại)

    @Test
    @DisplayName("Test: Server trả về trùng Username -> Hiện thông báo lỗi")
    public void testSignUp_ServerFail_Duplicate(FxRobot robot) throws Exception {
        // Mock Server trả về lỗi
        String jsonError = "{\"message\": \"Email hoặc Username đã tồn tại\"}";
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonError);

        robot.clickOn("#txtFullName").write("Lê Thanh Tùng");
        robot.clickOn("#txtUsername").write("lethtu");
        robot.clickOn("#txtEmail").write("lethtu@gmail.com");
        robot.clickOn("#txtPassword").write("123456");
        robot.clickOn("#txtConfirmPassword").write("123456");

        robot.clickOn("#chkTerms");
        robot.clickOn("#btnSignUp");

        robot.sleep(1500);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("Email hoặc Username đã tồn tại", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }

    // TEST 4: ĐĂNG KÝ THÀNH CÔNG -> NHẢY VỀ LOGIN

    @Test
    @DisplayName("Test: Mock Server báo thành công -> Hiện Popup Thành công")
    public void testSignUp_Success(FxRobot robot) throws Exception {
        // Nặn JSON giả thành công
        String jsonSuccess = "{\"message\": \"Đăng ký thành công\"}";
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);

        robot.clickOn("#txtFullName").write("Thành viên Mới");
        robot.clickOn("#txtUsername").write("newuser");
        robot.clickOn("#txtEmail").write("new@gmail.com");
        robot.clickOn("#txtPassword").write("123456");
        robot.clickOn("#txtConfirmPassword").write("123456");

        robot.clickOn("#chkTerms");
        robot.clickOn("#btnSignUp");

        robot.sleep(1500);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("Đăng ký tài khoản thành công!", NodeMatchers.isVisible());
        robot.clickOn("OK");

        robot.sleep(500);
    }
}