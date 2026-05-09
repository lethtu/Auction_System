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
public class ForgotPasswordControllerTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;

    @Start
    public void start(Stage stage) throws Exception {
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = (HttpResponse<String>) Mockito.mock(HttpResponse.class);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ForgotPassword.fxml"));
        Parent root = loader.load();

        ForgotPasswordController controller = loader.getController();
        controller.setHttpClient(mockHttpClient); // Bơm HTTP giả vào

        stage.setScene(new Scene(root, 400, 500));
        stage.show();
        stage.toFront(); // Ép app nổi lên trên cùng để Robot chiếm quyền điều khiển chuột
    }

    @Test
    @DisplayName("Test: Bỏ trống Email -> Cảnh báo")
    public void testGetOTP_EmptyEmail(FxRobot robot) {
        robot.clickOn("#btnGetOTP");
        verifyThat("Vui lòng nhập Email!", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }

    @Test
    @DisplayName("Test: Email không tồn tại -> Cảnh báo lỗi")
    public void testGetOTP_EmailNotFound(FxRobot robot) throws Exception {
        // Trả về JSON lỗi
        String jsonError = "{\"status\": 404, \"message\": \"Không có tài khoản nào liên kết với Email này\"}";
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonError);

        robot.clickOn("#txtEmail").write("mail_ao@gmail.com");
        robot.clickOn("#btnGetOTP");

        robot.sleep(1000); // Chờ Thread xử lý API
        WaitForAsyncUtils.waitForFxEvents(); // Chờ Platform.runLater vẽ Alert

        verifyThat("Không có tài khoản nào liên kết với Email này", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }

    @Test
    @DisplayName("Test Toàn Tập: Lấy OTP -> Sai Pass -> Đúng Pass -> Thành công")
    public void testFullFlow_SuccessToMismatchToSuccess(FxRobot robot) throws Exception {

        // GIAI ĐOẠN 1: MOCK SERVER TRẢ VỀ GỬI OTP THÀNH CÔNG

        String jsonSendOtpSuccess = "{\"status\": 200, \"message\": \"Đã gửi mã xác nhận\"}";
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonSendOtpSuccess);

        robot.clickOn("#txtEmail").write("real_email@gmail.com");
        robot.clickOn("#btnGetOTP");

        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("Đã gửi mã xác nhận", NodeMatchers.isVisible());
        robot.clickOn("OK");

        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#txtOTP", NodeMatchers.isVisible());

        // GIAI ĐOẠN 2: THỬ NHẬP PASS XÁC NHẬN BỊ LỆCH -> BÁO LỖI

        robot.clickOn("#txtOTP").write("123456");
        robot.clickOn("#txtNewPassword").write("Tungpro@123");
        robot.clickOn("#txtConfirmNewPassword").write("Tungpro@456"); // Sai pass xác nhận

        robot.clickOn("Xác nhận Đổi Mật Khẩu");

        robot.sleep(500);

        verifyThat("Thông tin không hợp lệ hoặc mật khẩu không khớp!", NodeMatchers.isVisible());
        robot.clickOn("OK");

        // GIAI ĐOẠN 3: SỬA PASS ĐÚNG -> MOCK SERVER BÁO THÀNH CÔNG

        String jsonResetSuccess = "{\"status\": 200, \"message\": \"Đổi mật khẩu thành công!\"}";
        when(mockHttpResponse.body()).thenReturn(jsonResetSuccess); // Đổi cục JSON giả

        // Xóa pass cũ và gõ lại pass đúng
        robot.clickOn("#txtConfirmNewPassword").eraseText(11).write("Tungpro@123");

        robot.clickOn("Xác nhận Đổi Mật Khẩu");

        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("Đổi mật khẩu thành công!", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }
}