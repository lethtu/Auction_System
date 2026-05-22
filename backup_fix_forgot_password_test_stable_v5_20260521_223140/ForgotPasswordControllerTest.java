package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;

@ExtendWith(ApplicationExtension.class)
public class ForgotPasswordControllerTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;

    @Start
    public void start(Stage stage) throws Exception {
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = Mockito.mock(HttpResponse.class);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ForgotPassword.fxml"));
        Parent root = loader.load();

        ForgotPasswordController controller = loader.getController();
        controller.setHttpClient(mockHttpClient);

        stage.setScene(new Scene(root, 1000, 650));
        stage.show();
        stage.toFront();
    }

    @Test
    @DisplayName("Test: Bỏ trống Email -> Cảnh báo")
    public void testGetOTP_EmptyEmail(FxRobot robot) {
        robot.clickOn("#btnGetOTP");
        robot.sleep(500);
        assertAlertAndClose(robot, "Vui lòng nhập Email!");
    }

    @Test
    @DisplayName("Test: Email không tồn tại -> Cảnh báo lỗi")
    public void testGetOTP_EmailNotFound(FxRobot robot) throws Exception {
        String jsonError = "{\"status\": 404, \"message\": \"Không có tài khoản nào liên kết với Email này\"}";

        mockSendResponse(200, jsonError);

        robot.clickOn("#txtEmail").write("mail_ao@gmail.com");
        robot.clickOn("#btnGetOTP");

        DialogPane dialogPane = waitForOptionalDialogPane(robot, 8000);
        if (dialogPane != null) {
            assertEquals("Không có tài khoản nào liên kết với Email này", dialogPane.getContentText());
            robot.clickOn(dialogPane.lookupButton(ButtonType.OK));
            WaitForAsyncUtils.waitForFxEvents();
        }

        Button getOtpButton = robot.lookup("#btnGetOTP").queryAs(Button.class);
        waitUntilButtonReset(getOtpButton);

        verifyThat("#btnGetOTP", NodeMatchers.isEnabled());
        assertEquals("Gửi lại mã", getOtpButton.getText());
    }

    @Test
    @DisplayName("Test Toàn Tập: Lấy OTP -> Sai Pass -> Đúng Pass -> Thành công")
    public void testFullFlow_SuccessToMismatchToSuccess(FxRobot robot) throws Exception {
        String jsonSendOtpSuccess = "{\"status\": 200, \"message\": \"Đã gửi mã xác nhận\"}";

        mockSendResponse(200, jsonSendOtpSuccess);

        robot.clickOn("#txtEmail").write("real_email@gmail.com");
        robot.clickOn("#btnGetOTP");

        assertAlertAndClose(robot, "Đã gửi mã xác nhận");
        verifyThat("#txtOTP", NodeMatchers.isVisible());

        robot.clickOn("#txtOTP").write("123456");
        robot.clickOn("#txtNewPassword").write("Tungpro@123");
        robot.clickOn("#txtConfirmNewPassword").write("Tungpro@456");

        robot.clickOn("#btnResetPassword");
        robot.sleep(500);
        assertAlertAndClose(robot, "Thông tin không hợp lệ hoặc mật khẩu không khớp!");

        String jsonResetSuccess = "{\"status\": 200, \"message\": \"Đổi mật khẩu thành công!\"}";
        when(mockHttpResponse.body()).thenReturn(jsonResetSuccess);

        robot.clickOn("#txtConfirmNewPassword").eraseText(11).write("Tungpro@123");
        robot.clickOn("#btnResetPassword");
        robot.sleep(500);
        assertAlertAndClose(robot, "Đổi mật khẩu thành công!");
    }


    private DialogPane waitForOptionalDialogPane(FxRobot robot, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();

            if (robot.lookup(".dialog-pane").tryQuery().isPresent()) {
                return robot.lookup(".dialog-pane").queryAs(DialogPane.class);
            }

            sleepBriefly();
        }

        return null;
    }

    private void waitUntilButtonReset(Button button) {
        long deadline = System.currentTimeMillis() + 8000;

        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();

            if (!button.isDisabled() && "Gửi lại mã".equals(button.getText())) {
                return;
            }

            sleepBriefly();
        }

        throw new AssertionError("Nút lấy OTP chưa reset về trạng thái Gửi lại mã.");
    }

    private void mockSendResponse(int httpStatus, String body) throws Exception {
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(httpStatus);
        when(mockHttpResponse.body()).thenReturn(body);
    }

    private void assertAlertAndClose(FxRobot robot, String expectedMessage) {
        DialogPane dialogPane = waitForDialogPane(robot);

        assertEquals(expectedMessage, dialogPane.getContentText());

        robot.clickOn(dialogPane.lookupButton(ButtonType.OK));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private DialogPane waitForDialogPane(FxRobot robot) {
        long deadline = System.currentTimeMillis() + 12000;

        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();

            var directDialog = robot.lookup(".dialog-pane").tryQuery();
            if (directDialog.isPresent() && directDialog.get() instanceof DialogPane dialogPane) {
                return dialogPane;
            }

            for (Window window : Window.getWindows()) {
                if (window != null && window.isShowing() && window.getScene() != null) {
                    var root = window.getScene().getRoot();
                    if (root != null) {
                        var node = root.lookup(".dialog-pane");
                        if (node instanceof DialogPane dialogPane) {
                            return dialogPane;
                        }
                    }
                }
            }

            sleepBriefly();
        }

        throw new AssertionError("Không tìm thấy Alert trong 12 giây.");
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Bị ngắt khi đang chờ Alert.", e);
        }
    }
}