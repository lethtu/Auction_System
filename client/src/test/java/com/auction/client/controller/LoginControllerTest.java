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

import org.mockito.Mockito;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.testfx.api.FxAssert.verifyThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class LoginControllerTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;

    @Start
    public void start(Stage stage) throws Exception {

        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = (HttpResponse<String>) Mockito.mock(HttpResponse.class);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/Login.fxml"));
        Parent root = loader.load();

        // Tiêm Mock HttpClient vào Controller
        LoginController controller = loader.getController();
        controller.setHttpClient(mockHttpClient);

        stage.setScene(new Scene(root, 400, 500));
        stage.show();
    }

    // TEST 1: SAI MẬT KHẨU (HTTP STATUS != 200)

    @Test
    @DisplayName("Test: Server trả về 401 -> Hiện cảnh báo sai mật khẩu")
    public void testLogin_SaiMatKhau(FxRobot robot) throws Exception {
        when(mockHttpClient.<String>send(any(HttpRequest.class), any()))
                .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(401);

        robot.clickOn("#txtUsername").write("tung_dep_trai");
        robot.clickOn("#txtPassword").write("123");
        robot.clickOn("#btnLogin");

        verifyThat("Sai tài khoản hoặc mật khẩu!", NodeMatchers.isVisible());
        robot.clickOn("OK");
    }

    // TEST 2: ĐĂNG NHẬP ADMIN THÀNH CÔNG

    @Test
    @DisplayName("Test: Server trả về ADMIN -> Check Popup Thành công")
    public void testLogin_AdminRole(FxRobot robot) throws Exception {
        String jsonAdmin = """
            {
                "status": 200,
                "data": {
                    "id": 1,
                    "username": "admin_test",
                    "fullname": "Lê Thanh Tùng",
                    "email": "admin@gmail.com",
                    "role": "ADMIN"
                }
            }
            """;

        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonAdmin);

        robot.clickOn("#txtUsername").write("admin_test");
        robot.clickOn("#txtPassword").write("123456");
        robot.clickOn("#btnLogin");

        verifyThat("Chào mừng bạn đã quay lại!", NodeMatchers.isVisible());
        robot.clickOn("OK");

        robot.sleep(500);
    }

    // TEST 3: ĐĂNG NHẬP SELLER THÀNH CÔNG

    @Test
    @DisplayName("Test: Server trả về SELLER -> Check Popup Thành công")
    public void testLogin_SellerRole(FxRobot robot) throws Exception {
        String jsonSeller = """
            {
                "status": 200,
                "data": {
                    "id": 2,
                    "username": "seller_test",
                    "fullname": "Lê Thanh Tùng",
                    "role": "SELLER"
                }
            }
            """;

        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonSeller);

        robot.clickOn("#txtUsername").write("seller_test");
        robot.clickOn("#txtPassword").write("123456");
        robot.clickOn("#btnLogin");

        verifyThat("Chào mừng bạn đã quay lại!", NodeMatchers.isVisible());
        robot.clickOn("OK");

        robot.sleep(500);
    }

    // TEST 4: ĐĂNG NHẬP BIDDER THÀNH CÔNG

    @Test
    @DisplayName("Test: Server trả về BIDDER -> Check Popup Thành công")
    public void testLogin_BidderRole(FxRobot robot) throws Exception {
        // Nặn JSON giả với Role là BIDDER
        String jsonBidder = """
            {
                "status": 200,
                "data": {
                    "id": 3,
                    "username": "bidder_test",
                    "fullname": "Lê Thanh Tùng",
                    "role": "BIDDER"
                }
            }
            """;

        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonBidder);

        robot.clickOn("#txtUsername").write("bidder_test");
        robot.clickOn("#txtPassword").write("123456");
        robot.clickOn("#btnLogin");

        // Bắt Alert Thành công
        verifyThat("Chào mừng bạn đã quay lại!", NodeMatchers.isVisible());
        robot.clickOn("OK");

        // Chờ load màn hình của Bidder (MainTemplate.fxml)
        robot.sleep(500);
    }
}