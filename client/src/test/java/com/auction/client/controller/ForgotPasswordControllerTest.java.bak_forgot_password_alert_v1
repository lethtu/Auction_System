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
        assertAlertAndClose(robot, "Please enter your email!");
    }

    @Test
    @DisplayName("Test: Email không tồn tại -> Cảnh báo lỗi")
    public void testGetOTP_EmailNotFound(FxRobot robot) throws Exception {
        String jsonError = "{\"status\": 404, \"message\": \"Không có tài khoản nào liên kết với Email này\"}";

        mockSendResponse(200, jsonError);

        robot.clickOn("#txtEmail").write("mail_ao@gmail.com");
        robot.clickOn("#btnGetOTP");

        assertAlertAndClose(robot, "Không có tài khoản nào liên kết với Email này");

        Button getOtpButton = robot.lookup("#btnGetOTP").queryAs(Button.class);
        waitUntilButtonReset(getOtpButton);

        verifyThat("#btnGetOTP", NodeMatchers.isEnabled());
        String currentOtpButtonText = getOtpButton.getText();
        org.junit.jupiter.api.Assertions.assertTrue(
                currentOtpButtonText.equalsIgnoreCase("Resend code") || currentOtpButtonText.equals("Gửi lại mã") || currentOtpButtonText.equals("Gửi mã xác thực"),
                "Nút lấy OTP phải ở trạng thái có thể gửi lại hoặc gửi mã xác thực, nhưng đang là: " + currentOtpButtonText
        );
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
        assertAlertAndClose(robot, "Invalid information or passwords do not match!");

        String jsonResetSuccess = "{\"status\": 200, \"message\": \"Đổi mật khẩu thành công!\"}";
        when(mockHttpResponse.body()).thenReturn(jsonResetSuccess);

        robot.clickOn("#txtConfirmNewPassword").eraseText(11).write("Tungpro@123");
        robot.clickOn("#btnResetPassword");
        robot.sleep(500);
        assertAlertAndClose(robot, "Đổi mật khẩu thành công!");
    }

    private void waitUntilButtonReset(Button button) {
        long deadline = System.currentTimeMillis() + 12000;

        while (System.currentTimeMillis() < deadline) {
            try {
                org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

                String text = button.getText();
                boolean hasStableText = text != null
                        && !text.trim().isEmpty()
                        && !text.toLowerCase().contains("đang")
                        && !text.toLowerCase().contains("dang")
                        && !text.contains("...");

                if (!button.isDisabled() && hasStableText) {
                    return;
                }
            } catch (Exception ignored) {
                // TestFX/Fx timing can be unstable on CI/local machines.
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Keep this fallback non-fatal: this test already verifies the error path by
        // checking the dialog when it appears. Some styled dialogs/async transitions
        // take longer on slower machines, so failing here would be flaky.
    }

    private void mockSendResponse(int httpStatus, String body) throws Exception {
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(httpStatus);
        when(mockHttpResponse.body()).thenReturn(body);
    }

    private void assertAlertAndClose(FxRobot robot, String expectedMessage) {
        long deadline = System.currentTimeMillis() + 12000;
        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();
            for (Window window : Window.getWindows()) {
                if (window != null && window.isShowing() && window.getScene() != null) {
                    var root = window.getScene().getRoot();
                    if (root != null && !root.getStyleClass().contains("auth-root")) {
                        boolean foundMessage = false;
                        for (javafx.scene.Node node : robot.from(root).lookup(".label").queryAll()) {
                            if (node instanceof javafx.scene.control.Label lbl) {
                                if (expectedMessage.equals(lbl.getText())) {
                                    foundMessage = true;
                                    break;
                                }
                            }
                        }
                        if (foundMessage) {
                            for (javafx.scene.Node node : robot.from(root).lookup(".button").queryAll()) {
                                if (node instanceof Button btn) {
                                    if ("OK".equalsIgnoreCase(btn.getText())) {
                                        robot.clickOn(btn);
                                        WaitForAsyncUtils.waitForFxEvents();
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            sleepBriefly();
        }
        throw new AssertionError("Không tìm thấy Alert với tin nhắn: " + expectedMessage);
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