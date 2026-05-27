package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;

@ExtendWith(ApplicationExtension.class)
public class ForgotPasswordControllerTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;

    private TextField emailField;
    private TextField otpField;
    private PasswordField newPasswordField;
    private PasswordField confirmNewPasswordField;
    private Button getOtpButton;
    private Button resetPasswordButton;

    @Start
    public void start(Stage stage) throws Exception {
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = Mockito.mock(HttpResponse.class);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/ForgotPassword.fxml"));
        Parent root = loader.load();

        ForgotPasswordController controller = loader.getController();
        controller.setHttpClient(mockHttpClient);

        emailField = (TextField) loader.getNamespace().get("txtEmail");
        otpField = (TextField) loader.getNamespace().get("txtOTP");
        newPasswordField = (PasswordField) loader.getNamespace().get("txtNewPassword");
        confirmNewPasswordField = (PasswordField) loader.getNamespace().get("txtConfirmNewPassword");
        getOtpButton = (Button) loader.getNamespace().get("btnGetOTP");
        resetPasswordButton = (Button) loader.getNamespace().get("btnResetPassword");

        stage.setScene(new Scene(root, 1000, 650));
        stage.show();
        stage.toFront();
    }

    @Test
    @DisplayName("Test: Empty email shows warning")
    public void testGetOTP_EmptyEmail(FxRobot robot) {
        robot.clickOn(getOtpButton);
        assertAlertAndClose(robot, "Please enter your email!");
    }

    @Test
    @DisplayName("Test: Unknown email keeps OTP button usable")
    public void testGetOTP_EmailNotFound(FxRobot robot) throws Exception {
        String jsonError = "{\"status\": 404, \"message\": \"Email not found\"}";

        mockSendResponse(200, jsonError);

        robot.interact(() -> emailField.setText("mail_ao@gmail.com"));
        robot.clickOn(getOtpButton);

        closeAnyAlertIfPresent(robot);
        waitUntilButtonReset(getOtpButton);

        verifyThat(getOtpButton, NodeMatchers.isEnabled());
        String currentOtpButtonText = getOtpButton.getText();
        org.junit.jupiter.api.Assertions.assertTrue(
                equalsAnyIgnoreCase(currentOtpButtonText, "Resend code", "Send verification code", "Gui lai ma", "Gui ma xac thuc"),
                "OTP button should be ready to send again, but was: " + currentOtpButtonText
        );
    }

    @Test
    @DisplayName("Test Full Flow: Get OTP -> Password mismatch -> Correct password -> Success")
    public void testFullFlow_SuccessToMismatchToSuccess(FxRobot robot) throws Exception {
        String jsonSendOtpSuccess = "{\"status\": 200, \"message\": \"OTP sent\"}";

        mockSendResponse(200, jsonSendOtpSuccess);

        robot.interact(() -> emailField.setText("real_email@gmail.com"));
        robot.clickOn(getOtpButton);

        assertAlertAndClose(robot, "OTP sent");
        verifyThat(otpField, NodeMatchers.isVisible());

        robot.interact(() -> {
            otpField.setText("123456");
            newPasswordField.setText("Tungpro@123");
            confirmNewPasswordField.setText("Tungpro@456");
        });

        robot.clickOn(resetPasswordButton);
        assertAlertAndClose(robot, "Invalid information or passwords do not match!");

        String jsonResetSuccess = "{\"status\": 200, \"message\": \"Password changed successfully!\"}";
        when(mockHttpResponse.body()).thenReturn(jsonResetSuccess);

        robot.interact(() -> confirmNewPasswordField.setText("Tungpro@123"));
        robot.clickOn(resetPasswordButton);

        assertAlertAndClose(robot, "Password changed successfully!");
    }

    private boolean equalsAnyIgnoreCase(String value, String... candidates) {
        if (value == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void waitUntilButtonReset(Button button) {
        long deadline = System.currentTimeMillis() + 12000;

        while (System.currentTimeMillis() < deadline) {
            try {
                WaitForAsyncUtils.waitForFxEvents();

                String text = button.getText();
                boolean hasStableText = text != null
                        && !text.trim().isEmpty()
                        && !text.toLowerCase().contains("dang")
                        && !text.contains("...");

                if (!button.isDisabled() && hasStableText) {
                    return;
                }
            } catch (Exception ignored) {
                // Ignore transient TestFX timing issues.
            }

            sleepBriefly();
        }
    }

    private void mockSendResponse(int httpStatus, String body) throws Exception {
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(httpStatus);
        when(mockHttpResponse.body()).thenReturn(body);
    }

    private void closeAnyAlertIfPresent(FxRobot robot) {
        long deadline = System.currentTimeMillis() + 12000;
        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();
            for (Window window : Window.getWindows()) {
                if (window != null && window.isShowing() && window.getScene() != null) {
                    var root = window.getScene().getRoot();
                    if (root != null && !root.getStyleClass().contains("auth-root")) {
                        for (javafx.scene.Node node : robot.from(root).lookup(".button").queryAll()) {
                            if (node instanceof Button btn && "OK".equalsIgnoreCase(btn.getText())) {
                                robot.clickOn(btn);
                                WaitForAsyncUtils.waitForFxEvents();
                                return;
                            }
                        }
                        robot.press(javafx.scene.input.KeyCode.ENTER).release(javafx.scene.input.KeyCode.ENTER);
                        WaitForAsyncUtils.waitForFxEvents();
                        return;
                    }
                }
            }
            sleepBriefly();
        }
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
                            if (node instanceof Label lbl && expectedMessage.equals(lbl.getText())) {
                                foundMessage = true;
                                break;
                            }
                        }
                        if (foundMessage) {
                            for (javafx.scene.Node node : robot.from(root).lookup(".button").queryAll()) {
                                if (node instanceof Button btn && "OK".equalsIgnoreCase(btn.getText())) {
                                    robot.clickOn(btn);
                                    WaitForAsyncUtils.waitForFxEvents();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            sleepBriefly();
        }
        throw new AssertionError("Alert not found with message: " + expectedMessage);
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for JavaFX state.", e);
        }
    }
}
