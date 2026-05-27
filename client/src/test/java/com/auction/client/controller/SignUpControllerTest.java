package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.stage.Stage;
import javafx.stage.Window;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

    // TEST 1: BO TRONG TRUONG DU LIEU
    @Test
    @DisplayName("Test: Bo trong form -> Hien canh bao")
    public void testSignUp_EmptyFields(FxRobot robot) {
        robot.clickOn("#txtFullName").write("Le Thanh Tung");
        robot.clickOn("#btnSignUp");
        verifyThat("Please fill in all fields!", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }

    // TEST 2: SAI MAT KHAU XAC NHAN
    @Test
    @DisplayName("Test: Mat khau xac nhan khong khop -> Hien canh bao")
    public void testSignUp_PasswordMismatch(FxRobot robot) {
        robot.clickOn("#txtFullName").write("Le Thanh Tung");
        robot.clickOn("#txtUsername").write("lethtu");
        robot.clickOn("#txtEmail").write("lethtu@gmail.com");
        robot.clickOn("#txtPassword").write("123456");
        robot.clickOn("#txtConfirmPassword").write("Sai_Mat_Khau_Nek");

        robot.clickOn("#chkTerms");
        robot.clickOn("#btnSignUp");

        verifyThat("Confirm password does not match!", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }

    // TEST 3: SERVER BAO TRUNG TAI KHOAN
    @Test
    @DisplayName("Test: Server tra ve trung Username -> Hien thong bao loi")
    public void testSignUp_ServerFail_Duplicate(FxRobot robot) throws Exception {
        String jsonError = "{\"message\": \"Email hoac Username da ton tai\"}";
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonError);

        robot.clickOn("#txtFullName").write("Le Thanh Tung");
        robot.clickOn("#txtUsername").write("lethtu");
        robot.clickOn("#txtEmail").write("lethtu@gmail.com");
        robot.clickOn("#txtPassword").write("123456");
        robot.clickOn("#txtConfirmPassword").write("123456");

        robot.clickOn("#chkTerms");
        robot.clickOn("#btnSignUp");

        robot.sleep(1500);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("Email hoac Username da ton tai", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }

    // TEST 4: DANG KY THANH CONG
    @Test
    @DisplayName("Test: Mock Server bao thanh cong -> Hien popup thanh cong")
    public void testSignUp_Success(FxRobot robot) throws Exception {
        String jsonSuccess = "{\"message\": \"success\"}";
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);

        robot.clickOn("#txtFullName").write("Thanh vien Moi");
        robot.clickOn("#txtUsername").write("newuser");
        robot.clickOn("#txtEmail").write("new@gmail.com");
        robot.clickOn("#txtPassword").write("123456");
        robot.clickOn("#txtConfirmPassword").write("123456");

        robot.clickOn("#chkTerms");
        robot.clickOn("#btnSignUp");

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> isTextVisibleInAnyWindow("Account registered successfully!"));
        assertTrue(isTextVisibleInAnyWindow("Account registered successfully!"));
        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();
    }

    private static boolean isTextVisibleInAnyWindow(String expectedText) {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .map(Window::getScene)
                .filter(Objects::nonNull)
                .map(Scene::getRoot)
                .filter(Objects::nonNull)
                .flatMap(root -> root.lookupAll(".label").stream())
                .filter(Node::isVisible)
                .filter(node -> node instanceof Labeled)
                .map(node -> ((Labeled) node).getText())
                .anyMatch(expectedText::equals);
    }
}
