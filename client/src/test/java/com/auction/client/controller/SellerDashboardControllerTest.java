package com.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import org.mockito.Mockito;
import com.auction.client.HttpClientSingleton;
import com.auction.client.model.User;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class SellerDashboardControllerTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;
    private SellerDashboardController controller;

    @Start
    public void start(Stage stage) throws Exception {
        // 1. Giả lập phiên đăng nhập của Seller
        User.setSession(2, "seller_test", "Lê Thanh Tùng", "seller@gmail.com", "2000-01-01", "Hà Nội", "SELLER");

        // 2. Mock HttpClient
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = (HttpResponse<String>) Mockito.mock(HttpResponse.class);

        // Đăng ký Mock HttpClient vào Singleton
        HttpClientSingleton.getInstance().setHttpClient(mockHttpClient);

        // Giả lập API lấy danh sách phiên ban đầu trả về rỗng để khởi tạo Dashboard
        // mượt mà
        when(mockHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("[]"); // Mảng JSON rỗng

        // 3. Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/SellerDashboard.fxml"));
        Parent root = loader.load();

        controller = loader.getController();

        stage.setScene(new Scene(root, 1280, 720));
        stage.show();
        stage.toFront();
    }

    @Test
    @DisplayName("Test 1: Mở modal và kiểm tra Validation lỗi khi bỏ trống các trường thông tin")
    public void testFormValidation_EmptyFields(FxRobot robot) {
        // 1. Nhấn nút mở Modal thêm sản phẩm mới
        robot.clickOn("Add New Item");
        robot.sleep(500);

        // Xác nhận Modal đã hiển thị
        verifyThat("#productNameField", NodeMatchers.isVisible());

        // 2. Xóa các trường thời gian tự động điền để kiểm thử validation khi bỏ trống
        Platform.runLater(() -> {
            ((TextField) robot.lookup("#txtStartDay").query()).clear();
            ((TextField) robot.lookup("#txtStartMonth").query()).clear();
            ((TextField) robot.lookup("#txtStartYear").query()).clear();
            ((TextField) robot.lookup("#txtStartHour").query()).clear();
            ((TextField) robot.lookup("#txtStartMin").query()).clear();

            ((TextField) robot.lookup("#txtEndDay").query()).clear();
            ((TextField) robot.lookup("#txtEndMonth").query()).clear();
            ((TextField) robot.lookup("#txtEndYear").query()).clear();
            ((TextField) robot.lookup("#txtEndHour").query()).clear();
            ((TextField) robot.lookup("#txtEndMin").query()).clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // 3. Nhấn nút Submit (Launch Auction) khi chưa điền thông tin
        robot.clickOn("#btnSubmit");
        robot.sleep(500);

        // 3. Xác minh các thông báo lỗi inline hiển thị đồng bộ màu hồng thương hiệu
        HBox errorTitle = (HBox) robot.lookup("#errorTitle").query();
        HBox errorPrice = (HBox) robot.lookup("#errorPrice").query();
        HBox errorStartDT = (HBox) robot.lookup("#errorStartDT").query();
        HBox errorEndDT = (HBox) robot.lookup("#errorEndDT").query();

        assertTrue(errorTitle.isVisible(), "Thông báo lỗi Title phải hiển thị");
        assertTrue(errorPrice.isVisible(), "Thông báo lỗi Starting Price phải hiển thị");
        assertTrue(errorStartDT.isVisible(), "Thông báo lỗi Start Time phải hiển thị");
        assertTrue(errorEndDT.isVisible(), "Thông báo lỗi End Time phải hiển thị");
        Platform.runLater(() -> controller.handleCloseModal());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Test 2: Tạo phiên COMING thành công (Thời gian bắt đầu ở tương lai -> Không hiện cảnh báo)")
    public void testCreateSession_Coming_Success(FxRobot robot) throws Exception {
        // 1. Nhấn nút mở Modal thêm mới
        robot.clickOn("Add New Item");
        robot.sleep(500);

        // 2. Nhập các thông tin hợp lệ
        robot.clickOn("#productNameField").write("Sản Phẩm Đấu Giá Coming");

        Platform.runLater(() -> {
            ComboBox<String> combo = (ComboBox<String>) robot.lookup("#productTypeCombo").query();
            combo.setValue("Electronics");
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#startingPriceField").write("5000000");

        // Xóa sạch các ô nhập thời gian
        Platform.runLater(() -> {
            ((TextField) robot.lookup("#txtStartDay").query()).clear();
            ((TextField) robot.lookup("#txtStartMonth").query()).clear();
            ((TextField) robot.lookup("#txtStartYear").query()).clear();
            ((TextField) robot.lookup("#txtStartHour").query()).clear();
            ((TextField) robot.lookup("#txtStartMin").query()).clear();

            ((TextField) robot.lookup("#txtEndDay").query()).clear();
            ((TextField) robot.lookup("#txtEndMonth").query()).clear();
            ((TextField) robot.lookup("#txtEndYear").query()).clear();
            ((TextField) robot.lookup("#txtEndHour").query()).clear();
            ((TextField) robot.lookup("#txtEndMin").query()).clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Thiết lập thời gian bắt đầu ở tương lai (Ví dụ: Năm 2029)
        robot.clickOn("#txtStartDay").write("12");
        robot.clickOn("#txtStartMonth").write("05");
        robot.clickOn("#txtStartYear").write("2029");
        robot.clickOn("#txtStartHour").write("10");
        robot.clickOn("#txtStartMin").write("30");

        // Thiết lập thời gian kết thúc ở tương lai xa hơn (Năm 2029)
        robot.clickOn("#txtEndDay").write("15");
        robot.clickOn("#txtEndMonth").write("05");
        robot.clickOn("#txtEndYear").write("2029");
        robot.clickOn("#txtEndHour").write("18");
        robot.clickOn("#txtEndMin").write("00");

        // 3. Mock API trả về Đăng bán thành công từ Server
        String jsonSuccess = "{\"status\": 200, \"message\": \"Tạo phiên đấu giá thành công.\" }";
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        // 4. Nhấn nút Launch Auction (Không hiện xác nhận giờ bắt đầu vì ở tương lai)
        robot.clickOn("#btnSubmit");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // 5. Xác minh thông báo Alert thành công xuất hiện và xác nhận
        verifyThat("Tạo phiên đấu giá thành công.", NodeMatchers.isVisible());
        robot.clickOn("Đồng ý");
    }

    @Test
    @DisplayName("Test 3: Tạo phiên ACTIVE thành công (Thời gian bắt đầu ở quá khứ -> Hiện cảnh báo kích hoạt ngay)")
    public void testCreateSession_Active_Success(FxRobot robot) throws Exception {
        // 1. Nhấn nút mở Modal thêm mới
        robot.clickOn("Add New Item");
        robot.sleep(500);

        // 2. Nhập các thông tin hợp lệ
        robot.clickOn("#productNameField").write("Sản Phẩm Đấu Giá Active");

        Platform.runLater(() -> {
            ComboBox<String> combo = (ComboBox<String>) robot.lookup("#productTypeCombo").query();
            combo.setValue("Electronics");
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#startingPriceField").write("3000000");

        // Xóa sạch các ô nhập thời gian
        Platform.runLater(() -> {
            ((TextField) robot.lookup("#txtStartDay").query()).clear();
            ((TextField) robot.lookup("#txtStartMonth").query()).clear();
            ((TextField) robot.lookup("#txtStartYear").query()).clear();
            ((TextField) robot.lookup("#txtStartHour").query()).clear();
            ((TextField) robot.lookup("#txtStartMin").query()).clear();

            ((TextField) robot.lookup("#txtEndDay").query()).clear();
            ((TextField) robot.lookup("#txtEndMonth").query()).clear();
            ((TextField) robot.lookup("#txtEndYear").query()).clear();
            ((TextField) robot.lookup("#txtEndHour").query()).clear();
            ((TextField) robot.lookup("#txtEndMin").query()).clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Thiết lập thời gian bắt đầu ở quá khứ (Ví dụ: Năm 2020)
        robot.clickOn("#txtStartDay").write("01");
        robot.clickOn("#txtStartMonth").write("01");
        robot.clickOn("#txtStartYear").write("2020");
        robot.clickOn("#txtStartHour").write("08");
        robot.clickOn("#txtStartMin").write("00");

        // Thiết lập thời gian kết thúc ở tương lai (Ví dụ: Năm 2029)
        robot.clickOn("#txtEndDay").write("15");
        robot.clickOn("#txtEndMonth").write("05");
        robot.clickOn("#txtEndYear").write("2029");
        robot.clickOn("#txtEndHour").write("18");
        robot.clickOn("#txtEndMin").write("00");

        // 3. Mock API trả về Đăng bán thành công từ Server
        String jsonSuccess = "{\"status\": 200, \"message\": \"Tạo phiên đấu giá thành công.\" }";
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        // 4. Nhấn nút Launch Auction
        robot.clickOn("#btnSubmit");
        robot.sleep(500);

        // 5. Xác minh hộp thoại xác nhận thời gian bắt đầu quá khứ "Thời gian bắt đầu
        // đã đến!" xuất hiện
        verifyThat("Thời gian bắt đầu đã đến!", NodeMatchers.isVisible());

        // Giả lập click vào nút "Bắt đầu ngay" trên hộp thoại xác nhận
        robot.clickOn("Bắt đầu ngay");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // 6. Xác minh thông báo Alert thành công xuất hiện và xác nhận
        verifyThat("Tạo phiên đấu giá thành công.", NodeMatchers.isVisible());
        robot.clickOn("Đồng ý");
    }

    @Test
    @DisplayName("Test 4: Kiểm tra lỗi thời gian kết thúc ở quá khứ (End Time in Past)")
    public void testFormValidation_EndTimeInPast(FxRobot robot) throws Exception {
        // 1. Nhấn nút mở Modal thêm mới
        robot.clickOn("Add New Item");
        robot.sleep(500);

        // 2. Nhập các thông tin hợp lệ
        robot.clickOn("#productNameField").write("Sản Phẩm Thời Gian Lỗi 1");

        Platform.runLater(() -> {
            ComboBox<String> combo = (ComboBox<String>) robot.lookup("#productTypeCombo").query();
            combo.setValue("Electronics");
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#startingPriceField").write("1000000");

        // Xóa sạch các ô nhập thời gian
        Platform.runLater(() -> {
            ((TextField) robot.lookup("#txtStartDay").query()).clear();
            ((TextField) robot.lookup("#txtStartMonth").query()).clear();
            ((TextField) robot.lookup("#txtStartYear").query()).clear();
            ((TextField) robot.lookup("#txtStartHour").query()).clear();
            ((TextField) robot.lookup("#txtStartMin").query()).clear();

            ((TextField) robot.lookup("#txtEndDay").query()).clear();
            ((TextField) robot.lookup("#txtEndMonth").query()).clear();
            ((TextField) robot.lookup("#txtEndYear").query()).clear();
            ((TextField) robot.lookup("#txtEndHour").query()).clear();
            ((TextField) robot.lookup("#txtEndMin").query()).clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Thiết lập thời gian bắt đầu ở tương lai (Ví dụ: Năm 2029)
        robot.clickOn("#txtStartDay").write("12");
        robot.clickOn("#txtStartMonth").write("05");
        robot.clickOn("#txtStartYear").write("2029");
        robot.clickOn("#txtStartHour").write("10");
        robot.clickOn("#txtStartMin").write("30");

        // Thiết lập thời gian kết thúc ở quá khứ (Ví dụ: Năm 2020)
        robot.clickOn("#txtEndDay").write("01");
        robot.clickOn("#txtEndMonth").write("01");
        robot.clickOn("#txtEndYear").write("2020");
        robot.clickOn("#txtEndHour").write("12");
        robot.clickOn("#txtEndMin").write("00");

        // 3. Nhấn nút Launch Auction
        robot.clickOn("#btnSubmit");
        robot.sleep(500);

        // 4. Xác minh thông báo lỗi kết thúc ở quá khứ hiển thị trực quan
        HBox errorEndDT = (HBox) robot.lookup("#errorEndDT").query();
        Label lblErrorEndDT = (Label) robot.lookup("#lblErrorEndDT").query();

        assertTrue(errorEndDT.isVisible(), "Thông báo lỗi End Time phải hiển thị");
        assertEquals("Thời gian kết thúc phải ở tương lai", lblErrorEndDT.getText(),
                "Nội dung thông báo lỗi phải chính xác");
        Platform.runLater(() -> controller.handleCloseModal());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Test 5: Kiểm tra lỗi thời gian kết thúc trước thời gian bắt đầu (End Time before Start Time)")
    public void testFormValidation_EndTimeBeforeStartTime(FxRobot robot) throws Exception {
        // 1. Nhấn nút mở Modal thêm mới
        robot.clickOn("Add New Item");
        robot.sleep(500);

        // 2. Nhập các thông tin hợp lệ
        robot.clickOn("#productNameField").write("Sản Phẩm Thời Gian Lỗi 2");

        Platform.runLater(() -> {
            ComboBox<String> combo = (ComboBox<String>) robot.lookup("#productTypeCombo").query();
            combo.setValue("Electronics");
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#startingPriceField").write("2000000");

        // Xóa sạch các ô nhập thời gian
        Platform.runLater(() -> {
            ((TextField) robot.lookup("#txtStartDay").query()).clear();
            ((TextField) robot.lookup("#txtStartMonth").query()).clear();
            ((TextField) robot.lookup("#txtStartYear").query()).clear();
            ((TextField) robot.lookup("#txtStartHour").query()).clear();
            ((TextField) robot.lookup("#txtStartMin").query()).clear();

            ((TextField) robot.lookup("#txtEndDay").query()).clear();
            ((TextField) robot.lookup("#txtEndMonth").query()).clear();
            ((TextField) robot.lookup("#txtEndYear").query()).clear();
            ((TextField) robot.lookup("#txtEndHour").query()).clear();
            ((TextField) robot.lookup("#txtEndMin").query()).clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Thiết lập thời gian bắt đầu ở tương lai xa (Ví dụ: Năm 2029)
        robot.clickOn("#txtStartDay").write("15");
        robot.clickOn("#txtStartMonth").write("05");
        robot.clickOn("#txtStartYear").write("2029");
        robot.clickOn("#txtStartHour").write("10");
        robot.clickOn("#txtStartMin").write("30");

        // Thiết lập thời gian kết thúc trước thời gian bắt đầu (Ví dụ: Năm 2028)
        robot.clickOn("#txtEndDay").write("12");
        robot.clickOn("#txtEndMonth").write("05");
        robot.clickOn("#txtEndYear").write("2028");
        robot.clickOn("#txtEndHour").write("10");
        robot.clickOn("#txtEndMin").write("30");

        // 3. Nhấn nút Launch Auction
        robot.clickOn("#btnSubmit");
        robot.sleep(500);

        // 4. Xác minh thông báo lỗi kết thúc trước bắt đầu hiển thị trực quan
        HBox errorEndDT = (HBox) robot.lookup("#errorEndDT").query();
        Label lblErrorEndDT = (Label) robot.lookup("#lblErrorEndDT").query();

        assertTrue(errorEndDT.isVisible(), "Thông báo lỗi End Time phải hiển thị");
        assertEquals("Thời gian kết thúc phải sau thời gian bắt đầu", lblErrorEndDT.getText(),
                "Nội dung thông báo lỗi phải chính xác");
        Platform.runLater(() -> controller.handleCloseModal());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Test 6: Lưu bản nháp (Save as Draft) thành công")
    public void testSaveAsDraft_Success(FxRobot robot) throws Exception {
        // 1. Nhấn nút mở Modal thêm mới
        robot.clickOn("Add New Item");
        robot.sleep(500);

        // 2. Nhập các thông tin hợp lệ
        robot.clickOn("#productNameField").write("Bản Nháp Sản Phẩm Mới");

        Platform.runLater(() -> {
            ComboBox<String> combo = (ComboBox<String>) robot.lookup("#productTypeCombo").query();
            combo.setValue("Electronics");
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#startingPriceField").write("3000000");

        // Xóa sạch các ô nhập thời gian
        Platform.runLater(() -> {
            ((TextField) robot.lookup("#txtStartDay").query()).clear();
            ((TextField) robot.lookup("#txtStartMonth").query()).clear();
            ((TextField) robot.lookup("#txtStartYear").query()).clear();
            ((TextField) robot.lookup("#txtStartHour").query()).clear();
            ((TextField) robot.lookup("#txtStartMin").query()).clear();

            ((TextField) robot.lookup("#txtEndDay").query()).clear();
            ((TextField) robot.lookup("#txtEndMonth").query()).clear();
            ((TextField) robot.lookup("#txtEndYear").query()).clear();
            ((TextField) robot.lookup("#txtEndHour").query()).clear();
            ((TextField) robot.lookup("#txtEndMin").query()).clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Thiết lập thời gian bắt đầu ở tương lai (Ví dụ: Năm 2029)
        robot.clickOn("#txtStartDay").write("20");
        robot.clickOn("#txtStartMonth").write("05");
        robot.clickOn("#txtStartYear").write("2029");
        robot.clickOn("#txtStartHour").write("09");
        robot.clickOn("#txtStartMin").write("00");

        // Thiết lập thời gian kết thúc ở tương lai xa hơn (Năm 2029)
        robot.clickOn("#txtEndDay").write("21");
        robot.clickOn("#txtEndMonth").write("05");
        robot.clickOn("#txtEndYear").write("2029");
        robot.clickOn("#txtEndHour").write("09");
        robot.clickOn("#txtEndMin").write("00");

        // 3. Mock API trả về Lưu bản nháp thành công từ Server
        String jsonSuccess = "{\"status\": 200, \"message\": \"Lưu bản nháp thành công.\" }";
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        // 4. Nhấn nút Save as Draft
        robot.clickOn("#btnDraftOrReset");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // 5. Xác minh thông báo Alert thành công xuất hiện và xác nhận
        verifyThat("Lưu bản nháp thành công.", NodeMatchers.isVisible());
        robot.clickOn("Đồng ý");
    }

    @Test
    @DisplayName("Test 7: Kiểm tra validation khi nhấn Save as Draft mà không điền đầy đủ")
    public void testSaveAsDraft_ValidationErrors(FxRobot robot) throws Exception {
        // 1. Nhấn nút mở Modal thêm mới
        robot.clickOn("Add New Item");
        robot.sleep(500);

        // 2. Xóa các trường thời gian tự động điền để kiểm thử validation khi bỏ trống
        Platform.runLater(() -> {
            ((ComboBox<String>) robot.lookup("#productTypeCombo").query()).setValue(null);
            ((TextField) robot.lookup("#txtStartDay").query()).clear();
            ((TextField) robot.lookup("#txtStartMonth").query()).clear();
            ((TextField) robot.lookup("#txtStartYear").query()).clear();
            ((TextField) robot.lookup("#txtStartHour").query()).clear();
            ((TextField) robot.lookup("#txtStartMin").query()).clear();

            ((TextField) robot.lookup("#txtEndDay").query()).clear();
            ((TextField) robot.lookup("#txtEndMonth").query()).clear();
            ((TextField) robot.lookup("#txtEndYear").query()).clear();
            ((TextField) robot.lookup("#txtEndHour").query()).clear();
            ((TextField) robot.lookup("#txtEndMin").query()).clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // 3. Nhấn nút Save as Draft khi bỏ trống tất cả
        robot.clickOn("#btnDraftOrReset");
        robot.sleep(500);

        // 4. Xác minh các thông báo lỗi hiển thị đồng bộ màu hồng thương hiệu
        HBox errorTitle = (HBox) robot.lookup("#errorTitle").query();
        HBox errorCategory = (HBox) robot.lookup("#errorCategory").query();
        HBox errorPrice = (HBox) robot.lookup("#errorPrice").query();
        HBox errorStartDT = (HBox) robot.lookup("#errorStartDT").query();
        HBox errorEndDT = (HBox) robot.lookup("#errorEndDT").query();

        assertTrue(errorTitle.isVisible(), "Thông báo lỗi Title phải hiển thị");
        assertTrue(errorCategory.isVisible(), "Thông báo lỗi Category phải hiển thị");
        assertTrue(errorPrice.isVisible(), "Thông báo lỗi Starting Price phải hiển thị");
        assertTrue(errorStartDT.isVisible(), "Thông báo lỗi Start Time phải hiển thị");
        assertTrue(errorEndDT.isVisible(), "Thông báo lỗi End Time phải hiển thị");
        Platform.runLater(() -> controller.handleCloseModal());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Test 8: Save as Draft - Kiểm tra lỗi thời gian kết thúc ở quá khứ (End Time in Past)")
    public void testSaveAsDraft_EndTimeInPast(FxRobot robot) throws Exception {
        // 1. Nhấn nút mở Modal thêm mới
        robot.clickOn("Add New Item");
        robot.sleep(500);

        // 2. Nhập các thông tin hợp lệ
        robot.clickOn("#productNameField").write("Bản Nháp Thời Gian Lỗi 1");

        Platform.runLater(() -> {
            ComboBox<String> combo = (ComboBox<String>) robot.lookup("#productTypeCombo").query();
            combo.setValue("Electronics");
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#startingPriceField").write("1000000");

        // Xóa sạch các ô nhập thời gian
        Platform.runLater(() -> {
            ((TextField) robot.lookup("#txtStartDay").query()).clear();
            ((TextField) robot.lookup("#txtStartMonth").query()).clear();
            ((TextField) robot.lookup("#txtStartYear").query()).clear();
            ((TextField) robot.lookup("#txtStartHour").query()).clear();
            ((TextField) robot.lookup("#txtStartMin").query()).clear();

            ((TextField) robot.lookup("#txtEndDay").query()).clear();
            ((TextField) robot.lookup("#txtEndMonth").query()).clear();
            ((TextField) robot.lookup("#txtEndYear").query()).clear();
            ((TextField) robot.lookup("#txtEndHour").query()).clear();
            ((TextField) robot.lookup("#txtEndMin").query()).clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Thiết lập thời gian bắt đầu ở tương lai (Ví dụ: Năm 2029)
        robot.clickOn("#txtStartDay").write("12");
        robot.clickOn("#txtStartMonth").write("05");
        robot.clickOn("#txtStartYear").write("2029");
        robot.clickOn("#txtStartHour").write("10");
        robot.clickOn("#txtStartMin").write("30");

        // Thiết lập thời gian kết thúc ở quá khứ (Ví dụ: Năm 2020)
        robot.clickOn("#txtEndDay").write("01");
        robot.clickOn("#txtEndMonth").write("01");
        robot.clickOn("#txtEndYear").write("2020");
        robot.clickOn("#txtEndHour").write("12");
        robot.clickOn("#txtEndMin").write("00");

        // 3. Nhấn nút Save as Draft
        robot.clickOn("#btnDraftOrReset");
        robot.sleep(500);

        // 4. Xác minh thông báo lỗi kết thúc ở quá khứ hiển thị trực quan
        HBox errorEndDT = (HBox) robot.lookup("#errorEndDT").query();
        Label lblErrorEndDT = (Label) robot.lookup("#lblErrorEndDT").query();

        assertTrue(errorEndDT.isVisible(), "Thông báo lỗi End Time phải hiển thị");
        assertEquals("Thời gian kết thúc phải ở tương lai", lblErrorEndDT.getText(),
                "Nội dung thông báo lỗi phải chính xác");
        Platform.runLater(() -> controller.handleCloseModal());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Test 9: Save as Draft - Kiểm tra lỗi thời gian kết thúc trước thời gian bắt đầu")
    public void testSaveAsDraft_EndTimeBeforeStartTime(FxRobot robot) throws Exception {
        // 1. Nhấn nút mở Modal thêm mới
        robot.clickOn("Add New Item");
        robot.sleep(500);

        // 2. Nhập các thông tin hợp lệ
        robot.clickOn("#productNameField").write("Bản Nháp Thời Gian Lỗi 2");

        Platform.runLater(() -> {
            ComboBox<String> combo = (ComboBox<String>) robot.lookup("#productTypeCombo").query();
            combo.setValue("Electronics");
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#startingPriceField").write("2000000");

        // Xóa sạch các ô nhập thời gian
        Platform.runLater(() -> {
            ((TextField) robot.lookup("#txtStartDay").query()).clear();
            ((TextField) robot.lookup("#txtStartMonth").query()).clear();
            ((TextField) robot.lookup("#txtStartYear").query()).clear();
            ((TextField) robot.lookup("#txtStartHour").query()).clear();
            ((TextField) robot.lookup("#txtStartMin").query()).clear();

            ((TextField) robot.lookup("#txtEndDay").query()).clear();
            ((TextField) robot.lookup("#txtEndMonth").query()).clear();
            ((TextField) robot.lookup("#txtEndYear").query()).clear();
            ((TextField) robot.lookup("#txtEndHour").query()).clear();
            ((TextField) robot.lookup("#txtEndMin").query()).clear();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Thiết lập thời gian bắt đầu ở tương lai xa (Ví dụ: Năm 2029)
        robot.clickOn("#txtStartDay").write("15");
        robot.clickOn("#txtStartMonth").write("05");
        robot.clickOn("#txtStartYear").write("2029");
        robot.clickOn("#txtStartHour").write("10");
        robot.clickOn("#txtStartMin").write("30");

        // Thiết lập thời gian kết thúc trước thời gian bắt đầu (Ví dụ: Năm 2028)
        robot.clickOn("#txtEndDay").write("12");
        robot.clickOn("#txtEndMonth").write("05");
        robot.clickOn("#txtEndYear").write("2028");
        robot.clickOn("#txtEndHour").write("10");
        robot.clickOn("#txtEndMin").write("30");

        // 3. Nhấn nút Save as Draft
        robot.clickOn("#btnDraftOrReset");
        robot.sleep(500);

        // 4. Xác minh thông báo lỗi kết thúc trước bắt đầu hiển thị trực quan
        HBox errorEndDT = (HBox) robot.lookup("#errorEndDT").query();
        Label lblErrorEndDT = (Label) robot.lookup("#lblErrorEndDT").query();

        assertTrue(errorEndDT.isVisible(), "Thông báo lỗi End Time phải hiển thị");
        assertEquals("Thời gian kết thúc phải sau thời gian bắt đầu", lblErrorEndDT.getText(),
                "Nội dung thông báo lỗi phải chính xác");
        Platform.runLater(() -> controller.handleCloseModal());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Test 10: Kiểm tra chế độ sửa đổi sản phẩm đang ACTIVE")
    public void testEditActiveSession(FxRobot robot) throws Exception {
        // 1. Chuẩn bị dữ liệu Mock cho phiên đấu giá đang hoạt động (ACTIVE)
        SellerDashboardController.SessionItem activeSession = new SellerDashboardController.SessionItem();
        activeSession.id = 99;
        activeSession.productName = "Sản phẩm Active Test";
        activeSession.productType = "Electronics";
        activeSession.startingPrice = new BigDecimal("5000000");
        activeSession.startTime = "2026-05-19T10:30:00";
        activeSession.endTime = "2026-05-20T10:30:00";
        activeSession.status = "ACTIVE";

        // 2. Thêm sản phẩm ACTIVE vào TableView và click vào nút Sửa (biểu tượng cây
        // bút #btnEdit_99)
        Platform.runLater(() -> {
            controller.displayedSessions.clear();
            controller.displayedSessions.add(activeSession);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#btnEdit_99");
        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // 3. Xác minh tiêu đề modal và các trường thông tin được điền đúng
        TextField nameField = (TextField) robot.lookup("#productNameField").query();
        assertEquals("Sản phẩm Active Test", nameField.getText(), "Tên sản phẩm phải khớp");

        // Xác minh trường giá bắt đầu và các ô nhập thời gian bắt đầu bị VÔ HIỆU HÓA
        // (disable)
        TextField startingPriceField = (TextField) robot.lookup("#startingPriceField").query();
        TextField txtStartDay = (TextField) robot.lookup("#txtStartDay").query();
        TextField txtStartMonth = (TextField) robot.lookup("#txtStartMonth").query();
        TextField txtStartYear = (TextField) robot.lookup("#txtStartYear").query();
        TextField txtStartHour = (TextField) robot.lookup("#txtStartHour").query();
        TextField txtStartMin = (TextField) robot.lookup("#txtStartMin").query();

        assertTrue(startingPriceField.isDisable(), "Giá bắt đầu phải bị vô hiệu hóa khi sản phẩm active");
        assertTrue(txtStartDay.isDisable(), "Start Day phải bị vô hiệu hóa khi sản phẩm active");
        assertTrue(txtStartMonth.isDisable(), "Start Month phải bị vô hiệu hóa khi sản phẩm active");
        assertTrue(txtStartYear.isDisable(), "Start Year phải bị vô hiệu hóa khi sản phẩm active");
        assertTrue(txtStartHour.isDisable(), "Start Hour phải bị vô hiệu hóa khi sản phẩm active");
        assertTrue(txtStartMin.isDisable(), "Start Min phải bị vô hiệu hóa khi sản phẩm active");

        // Test thử click và nhập vào các trường bị vô hiệu hóa -> đảm bảo giá trị gốc
        // không đổi
        try {
            robot.clickOn("#startingPriceField").write("9999999");
        } catch (Exception e) {
            // TestFX ném exception khi tương tác với control bị disable - đúng thiết kế
        }
        assertEquals("5000000", startingPriceField.getText(), "Giá trị starting price không được thay đổi");

        try {
            robot.clickOn("#txtStartDay").write("25");
        } catch (Exception e) {
            // Đúng thiết kế
        }
        assertEquals("19", txtStartDay.getText(), "Giá trị ngày bắt đầu không được thay đổi");

        // 4. Xác minh nút 'Save as Draft' đã được đổi tên thành 'Reset'
        Button btnDraftOrReset = (Button) robot.lookup("#btnDraftOrReset").query();
        assertEquals("Reset", btnDraftOrReset.getText(), "Nút phải chuyển thành Reset khi sp đang active");

        // 5. Thay đổi tên sản phẩm trên giao diện
        Platform.runLater(() -> nameField.clear());
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#productNameField").write("Tên Đã Sửa");

        // 6. Nhấn nút Reset
        robot.clickOn("#btnDraftOrReset");
        WaitForAsyncUtils.waitForFxEvents();

        // 7. Xác minh giá trị được hoàn tác về dữ liệu gốc ACTIVE
        assertEquals("Sản phẩm Active Test", nameField.getText(), "Giá trị phải được reset hoàn tác khi click Reset");
        Platform.runLater(() -> controller.handleCloseModal());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Test 11: Kiểm tra lưu thay đổi và PUBLISH cho phiên DRAFT")
    public void testSaveChanges_Draft_Success(FxRobot robot) throws Exception {
        // 1. Chuẩn bị dữ liệu Mock cho phiên đấu giá đang được chỉnh sửa DRAFT
        SellerDashboardController.SessionItem draftSession = new SellerDashboardController.SessionItem();
        draftSession.id = 123;
        draftSession.productName = "Sản phẩm Draft Ban Đầu";
        draftSession.productType = "Electronics";
        draftSession.startingPrice = new BigDecimal("3000000");
        draftSession.startTime = "2029-05-19T10:30:00";
        draftSession.endTime = "2029-05-20T10:30:00";
        draftSession.status = "DRAFT";

        // 2. Thêm sản phẩm vào TableView và click vào nút Sửa (biểu tượng cây bút
        // #btnEdit_123)
        Platform.runLater(() -> {
            controller.displayedSessions.clear();
            controller.displayedSessions.add(draftSession);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#btnEdit_123");
        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // Xác minh tên nút ở chế độ edit Draft: nút chính là "Publish", nút nháp là
        // "Save Changes"
        Button btnSubmit = (Button) robot.lookup("#btnSubmit").query();
        Button btnDraftOrReset = (Button) robot.lookup("#btnDraftOrReset").query();
        assertEquals("Publish", btnSubmit.getText(), "Nút submit phải đổi thành Publish");
        assertEquals("Save Changes", btnDraftOrReset.getText(), "Nút draft/reset phải đổi thành Save Changes");

        // 3. Thay đổi tên sản phẩm trên giao diện
        TextField nameField = (TextField) robot.lookup("#productNameField").query();
        Platform.runLater(() -> nameField.clear());
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#productNameField").write("Sản phẩm Draft Đã Sửa");

        // 4. Mock API lưu thay đổi thành công
        String jsonSuccess = "{\"status\": 200, \"message\": \"Cập nhật phiên đấu giá thành công.\" }";
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        // 5. Nhấn nút Publish (#btnSubmit)
        robot.clickOn("#btnSubmit");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // 6. Xác minh hộp thoại thành công hiển thị và click xác nhận
        verifyThat("Cập nhật phiên đấu giá thành công.", NodeMatchers.isVisible());
        robot.clickOn("Đồng ý");
    }

    @Test
    @DisplayName("Test 12: Kiểm tra lưu thay đổi dạng DRAFT (Save Changes) cho phiên DRAFT")
    public void testSaveChanges_Draft_SaveDraft(FxRobot robot) throws Exception {
        // 1. Chuẩn bị dữ liệu Mock cho phiên đấu giá DRAFT
        SellerDashboardController.SessionItem draftSession = new SellerDashboardController.SessionItem();
        draftSession.id = 124;
        draftSession.productName = "Sản phẩm Draft Bản Gốc";
        draftSession.productType = "Electronics";
        draftSession.startingPrice = new BigDecimal("3500000");
        draftSession.startTime = "2029-05-19T10:30:00";
        draftSession.endTime = "2029-05-20T10:30:00";
        draftSession.status = "DRAFT";

        // 2. Thêm sản phẩm vào TableView và click vào nút Sửa
        Platform.runLater(() -> {
            controller.displayedSessions.clear();
            controller.displayedSessions.add(draftSession);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#btnEdit_124");
        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // 3. Sửa thông tin sản phẩm
        TextField nameField = (TextField) robot.lookup("#productNameField").query();
        Platform.runLater(() -> nameField.clear());
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#productNameField").write("Sản phẩm Draft Sửa Tên");

        // 4. Mock API lưu bản nháp thành công
        String jsonSuccess = "{\"status\": 200, \"message\": \"Cập nhật bản nháp thành công.\" }";
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        // 5. Nhấn nút Save Changes (#btnDraftOrReset)
        robot.clickOn("#btnDraftOrReset");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // 6. Xác minh hộp thoại thành công hiển thị và click xác nhận
        verifyThat("Cập nhật bản nháp thành công.", NodeMatchers.isVisible());
        robot.clickOn("Đồng ý");
    }

    @Test
    @DisplayName("Test 13: Kiểm tra lưu thay đổi và PUBLISH cho phiên COMING")
    public void testSaveChanges_Coming_Success(FxRobot robot) throws Exception {
        // 1. Chuẩn bị dữ liệu Mock cho phiên đấu giá COMING
        SellerDashboardController.SessionItem comingSession = new SellerDashboardController.SessionItem();
        comingSession.id = 101;
        comingSession.productName = "Sản phẩm Coming Ban Đầu";
        comingSession.productType = "Electronics";
        comingSession.startingPrice = new BigDecimal("4000000");
        comingSession.startTime = "2029-05-19T10:30:00";
        comingSession.endTime = "2029-05-20T10:30:00";
        comingSession.status = "COMING";

        // 2. Thêm sản phẩm vào TableView và click vào nút Sửa (biểu tượng cây bút
        // #btnEdit_101)
        Platform.runLater(() -> {
            controller.displayedSessions.clear();
            controller.displayedSessions.add(comingSession);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#btnEdit_101");
        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // Xác minh tên nút ở chế độ edit Coming
        Button btnSubmit = (Button) robot.lookup("#btnSubmit").query();
        Button btnDraftOrReset = (Button) robot.lookup("#btnDraftOrReset").query();
        assertEquals("Save Changes", btnSubmit.getText(), "Nút submit phải đổi thành Save Changes");
        assertEquals("Reset", btnDraftOrReset.getText(), "Nút draft/reset phải đổi thành Reset");

        // 3. Thay đổi tên sản phẩm trên giao diện
        TextField nameField = (TextField) robot.lookup("#productNameField").query();
        Platform.runLater(() -> nameField.clear());
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#productNameField").write("Sản phẩm Coming Đã Sửa");

        // 4. Mock API lưu thay đổi thành công
        String jsonSuccess = "{\"status\": 200, \"message\": \"Cập nhật phiên đấu giá thành công.\" }";
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        // 5. Nhấn nút Save Changes (#btnSubmit)
        robot.clickOn("#btnSubmit");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // 6. Xác minh hộp thoại thành công hiển thị và click xác nhận
        verifyThat("Cập nhật phiên đấu giá thành công.", NodeMatchers.isVisible());
        robot.clickOn("Đồng ý");
    }

    @Test
    @DisplayName("Test 14: Kiểm tra lưu thay đổi thành công cho phiên ACTIVE khi nhấn Save Changes")
    public void testSaveChanges_Active_Success(FxRobot robot) throws Exception {
        // 1. Chuẩn bị dữ liệu Mock cho phiên đấu giá ACTIVE (thời gian bắt đầu ở quá
        // khứ)
        SellerDashboardController.SessionItem activeSession = new SellerDashboardController.SessionItem();
        activeSession.id = 102;
        activeSession.productName = "Sản phẩm Active Ban Đầu";
        activeSession.productType = "Electronics";
        activeSession.startingPrice = new BigDecimal("6000000");
        activeSession.startTime = "2026-05-19T10:30:00"; // Quá khứ
        activeSession.endTime = "2029-05-20T10:30:00"; // Tương lai
        activeSession.status = "ACTIVE";

        // 2. Thêm sản phẩm vào TableView và click vào nút Sửa (biểu tượng cây bút
        // #btnEdit_102)
        Platform.runLater(() -> {
            controller.displayedSessions.clear();
            controller.displayedSessions.add(activeSession);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#btnEdit_102");
        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // Xác minh tên nút ở chế độ edit Active
        Button btnSubmit = (Button) robot.lookup("#btnSubmit").query();
        Button btnDraftOrReset = (Button) robot.lookup("#btnDraftOrReset").query();
        assertEquals("Save Changes", btnSubmit.getText(), "Nút submit phải là Save Changes");
        assertEquals("Reset", btnDraftOrReset.getText(), "Nút draft/reset phải là Reset");

        // 3. Thay đổi tên sản phẩm trên giao diện
        TextField nameField = (TextField) robot.lookup("#productNameField").query();
        Platform.runLater(() -> nameField.clear());
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#productNameField").write("Sản phẩm Active Đã Sửa");

        // 4. Mock API lưu thay đổi thành công
        String jsonSuccess = "{\"status\": 200, \"message\": \"Cập nhật phiên đấu giá thành công.\" }";
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        // 5. Nhấn nút Save Changes (#btnSubmit)
        robot.clickOn("#btnSubmit");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // Vì start time ở quá khứ, cảnh báo bắt đầu ngay sẽ hiển thị -> click "Bắt đầu
        // ngay"
        verifyThat("Thời gian bắt đầu đã đến!", NodeMatchers.isVisible());
        robot.clickOn("Bắt đầu ngay");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // 6. Xác minh hộp thoại thành công hiển thị và click xác nhận
        verifyThat("Cập nhật phiên đấu giá thành công.", NodeMatchers.isVisible());
        robot.clickOn("Đồng ý");
    }

    @Test
    @DisplayName("Test 15: Kiểm tra chế độ sửa đổi và nút Reset cho sản phẩm COMING")
    public void testEditComingSession_Reset(FxRobot robot) throws Exception {
        // 1. Chuẩn bị dữ liệu Mock cho phiên đấu giá COMING
        SellerDashboardController.SessionItem comingSession = new SellerDashboardController.SessionItem();
        comingSession.id = 105;
        comingSession.productName = "Sản phẩm Coming Reset Test";
        comingSession.productType = "Electronics";
        comingSession.startingPrice = new BigDecimal("4000000");
        comingSession.startTime = "2029-05-19T10:30:00";
        comingSession.endTime = "2029-05-20T10:30:00";
        comingSession.status = "COMING";

        // 2. Thêm sản phẩm COMING vào TableView và click vào nút Sửa (#btnEdit_105)
        Platform.runLater(() -> {
            controller.displayedSessions.clear();
            controller.displayedSessions.add(comingSession);
        });
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#btnEdit_105");
        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // 3. Xác minh trường thông tin và trạng thái nút (Save Changes & Reset)
        TextField nameField = (TextField) robot.lookup("#productNameField").query();
        assertEquals("Sản phẩm Coming Reset Test", nameField.getText(), "Tên sản phẩm phải khớp");

        Button btnSubmit = (Button) robot.lookup("#btnSubmit").query();
        Button btnDraftOrReset = (Button) robot.lookup("#btnDraftOrReset").query();
        assertEquals("Save Changes", btnSubmit.getText(), "Nút submit phải là Save Changes");
        assertEquals("Reset", btnDraftOrReset.getText(), "Nút draft/reset phải là Reset");

        // 4. Thay đổi tên sản phẩm trên giao diện
        Platform.runLater(() -> nameField.clear());
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#productNameField").write("Tên Coming Đã Sửa");

        // 5. Nhấn nút Reset
        robot.clickOn("#btnDraftOrReset");
        WaitForAsyncUtils.waitForFxEvents();

        // 6. Xác minh giá trị được hoàn tác về dữ liệu gốc COMING
        assertEquals("Sản phẩm Coming Reset Test", nameField.getText(),
                "Giá trị phải được reset hoàn tác khi click Reset");
        Platform.runLater(() -> controller.handleCloseModal());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Test 16: Kiểm tra đăng bán nhanh thành công phiên DRAFT có thời gian bắt đầu ở Tương lai")
    public void testQuickPublish_Future_Success(FxRobot robot) throws Exception {
        // 1. Chuẩn bị dữ liệu bản nháp (DRAFT) có thời gian bắt đầu ở tương lai
        SellerDashboardController.SessionItem draftSession = new SellerDashboardController.SessionItem();
        draftSession.id = 106;
        draftSession.productName = "Sản phẩm Draft Publish Nhanh Future";
        draftSession.productType = "Electronics";
        draftSession.startingPrice = new BigDecimal("1200000");
        draftSession.stepPrice = new BigDecimal("20000");
        draftSession.startTime = "2029-05-19T10:30:00";
        draftSession.endTime = "2029-05-20T10:30:00";
        draftSession.status = "DRAFT";
        draftSession.imageUrl = "";
        draftSession.description = "";

        // 2. Thêm vào bảng
        Platform.runLater(() -> {
            controller.displayedSessions.clear();
            controller.displayedSessions.add(draftSession);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Mock API
        String jsonSuccess = "{\"status\": 200, \"message\": \"Đăng bán phiên đấu giá thành công.\" }";
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        // 3. Click nút Đăng bán nhanh (#btnView_106)
        robot.clickOn("#btnView_106");
        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // 4. Click Đăng bán trên dialog xác nhận
        verifyThat("Đăng bán nhanh", NodeMatchers.isVisible());
        robot.clickOn("Đăng bán");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // 5. Xác minh hộp thoại thành công hiển thị và click xác nhận
        verifyThat("Đăng bán phiên đấu giá thành công.", NodeMatchers.isVisible());
        robot.clickOn("Đồng ý");
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Test 17: Kiểm tra đăng bán nhanh thành công phiên DRAFT có thời gian bắt đầu ở Quá khứ")
    public void testQuickPublish_Past_Success(FxRobot robot) throws Exception {
        // 1. Chuẩn bị dữ liệu bản nháp (DRAFT) có thời gian bắt đầu ở quá khứ
        SellerDashboardController.SessionItem draftSession = new SellerDashboardController.SessionItem();
        draftSession.id = 107;
        draftSession.productName = "Sản phẩm Draft Publish Nhanh Past";
        draftSession.productType = "Electronics";
        draftSession.startingPrice = new BigDecimal("1500000");
        draftSession.stepPrice = new BigDecimal("20000");
        draftSession.startTime = "2020-05-19T10:30:00";
        draftSession.endTime = "2029-05-20T10:30:00";
        draftSession.status = "DRAFT";
        draftSession.imageUrl = "";
        draftSession.description = "";

        // 2. Thêm vào bảng
        Platform.runLater(() -> {
            controller.displayedSessions.clear();
            controller.displayedSessions.add(draftSession);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Mock API
        String jsonSuccess = "{\"status\": 200, \"message\": \"Đăng bán phiên đấu giá thành công.\" }";
        when(mockHttpResponse.body()).thenReturn(jsonSuccess);
        when(mockHttpResponse.statusCode()).thenReturn(200);

        // 3. Click nút Đăng bán nhanh (#btnView_107)
        robot.clickOn("#btnView_107");
        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // 4. Click Đăng bán trên dialog xác nhận
        verifyThat("Đăng bán nhanh", NodeMatchers.isVisible());
        robot.clickOn("Đăng bán");
        robot.sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // 5. Vì start time ở quá khứ, xác nhận bắt đầu ngay hiển thị -> click "Bắt đầu
        // ngay"
        verifyThat("Thời gian bắt đầu đã đến!", NodeMatchers.isVisible());
        robot.clickOn("Bắt đầu ngay");
        robot.sleep(1000);
        WaitForAsyncUtils.waitForFxEvents();

        // 6. Xác minh hộp thoại thành công hiển thị và click xác nhận
        verifyThat("Đăng bán phiên đấu giá thành công.", NodeMatchers.isVisible());
        robot.clickOn("Đồng ý");
        WaitForAsyncUtils.waitForFxEvents();
    }
}