package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.HttpClientSingleton;
import com.auction.client.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(SellerDashboardController.class);

    @FXML private ListView<String> mySessionsList;
    @FXML private ComboBox<String> productTypeCombo;
    @FXML private TextField productNameField;
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startingPriceField;
    @FXML private TextField stepPriceField;
    @FXML private TextField endTimeField;
    @FXML private TextArea statsArea;
    @FXML private DatePicker datePickerStart;
    @FXML private TextField txtStartTime;
    @FXML private DatePicker datePickerEnd;
    @FXML private TextField txtEndTime;

    private final HttpClient httpClient = HttpClientSingleton.getInstance().getHttpClient();
    private final List<SessionItem> allSessions = new ArrayList<>();
    private final List<SessionItem> displayedSessions = new ArrayList<>();

    @FXML
    public void initialize() {
        productTypeCombo.setItems(FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"
        ));
        productTypeCombo.setValue("Electronics");
        fillDefaultEndTime();
        loadMySessions();
    }

    @FXML
    private void handleCreateSession() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Không lấy được sellerId từ session");
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String imageUrl = productNameOrEmpty(imageUrlField);
        String description = productNameOrEmpty(descriptionArea);
        String startingPriceText = productNameOrEmpty(startingPriceField);
        String stepPriceText = productNameOrEmpty(stepPriceField);

        if (productName.isEmpty() || productType == null || startingPriceText.isEmpty() || stepPriceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại, giá khởi điểm và bước giá.");
            return;
        }

        try {
            BigDecimal startingPrice = new BigDecimal(startingPriceText.trim());
            BigDecimal stepPrice = new BigDecimal(stepPriceText.trim());

            JSONObject body = new JSONObject();

            body.put("name", productName);
            body.put("type", productType);
            body.put("imagePath", imageUrl);
            body.put("description", description);
            body.put("startingPrice", startingPrice);
            body.put("stepPrice", stepPrice);
            body.put("sellerId", sellerId);

            // --- XỬ LÝ THỜI GIAN BẮT ĐẦU ---
            if (datePickerStart.getValue() == null) {
                // Để trống -> Gửi Null để Server tự gán giờ hiện tại
                body.put("startTime", JSONObject.NULL);
            } else {
                String timePart = txtStartTime.getText().trim().isEmpty() ? "00:00" : txtStartTime.getText().trim();
                LocalDateTime startDT = LocalDateTime.of(datePickerStart.getValue(), java.time.LocalTime.parse(timePart));

                // Validation: Không cho phép nhập quá khứ
                if (startDT.isBefore(LocalDateTime.now())) {
                    showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian bắt đầu không được ở quá khứ!");
                    return;
                }
                body.put("startTime", startDT.toString());
            }

            // --- XỬ LÝ THỜI GIAN KẾT THÚC ---
            if (datePickerEnd.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng chọn ngày kết thúc!");
                return;
            }

            String endTimePart = txtEndTime.getText().trim().isEmpty() ? "23:59" : txtEndTime.getText().trim();
            LocalDateTime endDT = LocalDateTime.of(datePickerEnd.getValue(), java.time.LocalTime.parse(endTimePart));

            if (!endDT.isAfter(LocalDateTime.now())) {
                showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phải ở tương lai!");
                return;
            }
            body.put("endTime", endDT.toString());

            // --- GỬI REQUEST LÊN SERVER ---
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/create-auction"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(), "Tạo phiên đấu giá thành công.");

            if (api.success) {
                clearForm();
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", api.message);
            } else {
                logger.error("Lỗi api: {}", api.message);
                showAlert(Alert.AlertType.ERROR, "Lỗi", api.message);
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Giá khởi điểm và bước giá phải là số.");
        } catch (java.time.format.DateTimeParseException e) {
            // Bắt lỗi nếu gõ sai định dạng giờ (VD: gõ "abc" thay vì "14:00")
            showAlert(Alert.AlertType.ERROR, "Sai định dạng giờ", "Vui lòng nhập giờ theo định dạng HH:mm (ví dụ 08:30 hoặc 14:00)");
        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến máy chủ: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    @FXML
    private void handleShowAllSessions() {
        renderSessions(allSessions);
    }

    @FXML
    private void handleShowPendingSessions() {
        renderSessions(filterByStatus("PENDING"));
    }

    @FXML
    private void handleShowActiveSessions() {
        renderSessions(filterByStatus("ACTIVE"));
    }

    @FXML
    private void handleShowRejectedSessions() {
        renderSessions(filterByStatus("REJECTED"));
    }

    @FXML
    private void handleEditSelectedSession() {
        showAlert(Alert.AlertType.INFORMATION,
                "Tạm khóa chức năng",
                "Chức năng sửa phiên đang tạm khóa để bản demo ổn định hơn.\nBạn có thể demo tạo, xem, lọc, hủy phiên và phân quyền.");
    }

    @FXML
    private void handleCancelSelectedSession() {
        SessionItem selected = getSelectedSession();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn phiên", "Bạn hãy chọn một phiên để hủy.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selected.status)) {
            showAlert(Alert.AlertType.WARNING, "Không thể hủy",
                    "Chỉ được hủy phiên đang ở trạng thái PENDING.");
            return;
        }

        Integer sellerId = User.getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn hủy phiên #" + selected.id + " không?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/cancel-session/" + selected.id + "?sellerId=" + sellerId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(), "Đã hủy phiên thành công.");

            if (api.success) {
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", api.message);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", api.message);
            }
        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến máy chủ: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    @FXML
    private void goBack(javafx.event.ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1024, 768);
    }

    private void loadMySessions() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Lỗi không lấy được sellerId từ session");
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/my-sessions/" + sellerId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            ApiArrayResult api = extractDataArray(response.body(), response.statusCode());
            if (!api.success) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", api.message);
                return;
            }

            allSessions.clear();
            for (int i = 0; i < api.data.length(); i++) {
                allSessions.add(parseSession(api.data.getJSONObject(i)));
            }

            renderSessions(allSessions);
            updateStats();

        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể tải dữ liệu seller từ server.");
        }
    }

    private SessionItem parseSession(JSONObject item) {
        SessionItem s = new SessionItem();
        s.id = item.optInt("id", 0);

        if (item.has("productName")) {
            s.productName = item.optString("productName", "Không rõ");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.productName = product != null ? product.optString("name", "Không rõ") : "Không rõ";
        } else {
            s.productName = "Không rõ";
        }

        if (item.has("productType")) {
            s.productType = item.optString("productType", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.productType = product != null ? product.optString("type", "") : "";
        }

        if (item.has("imageUrl")) {
            s.imageUrl = item.optString("imageUrl", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.imageUrl = product != null ? product.optString("imageUrl", "") : "";
        }

        if (item.has("description")) {
            s.description = item.optString("description", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.description = product != null ? product.optString("description", "") : "";
        }

        s.startingPrice = parseBigDecimal(item, "startingPrice");
        s.currentPrice = parseBigDecimal(item, "currentPrice");
        s.stepPrice = parseBigDecimal(item, "stepPrice");
        s.endTime = item.optString("endTime", "");
        s.status = item.optString("status", "UNKNOWN");
        return s;
    }

    private BigDecimal parseBigDecimal(JSONObject item, String key) {
        if (!item.has(key) || item.isNull(key)) {
            return BigDecimal.ZERO;
        }

        Object value = item.get(key);

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void renderSessions(List<SessionItem> sessions) {
        displayedSessions.clear();
        displayedSessions.addAll(sessions);

        List<String> rendered = new ArrayList<>();
        for (SessionItem s : sessions) {
            rendered.add("Session #" + s.id + " | " + s.productName + " | " + s.status);
        }
        mySessionsList.setItems(FXCollections.observableArrayList(rendered));
    }

    private List<SessionItem> filterByStatus(String status) {
        List<SessionItem> filtered = new ArrayList<>();
        for (SessionItem s : allSessions) {
            if (s.status != null && s.status.equalsIgnoreCase(status)) {
                filtered.add(s);
            }
        }
        return filtered;
    }

    private SessionItem getSelectedSession() {
        int index = mySessionsList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= displayedSessions.size()) {
            return null;
        }
        return displayedSessions.get(index);
    }

    private void updateStats() {
        int pending = 0, active = 0, rejected = 0, completed = 0, canceled = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        for (SessionItem s : allSessions) {
            if (s.status == null) continue;
            switch (s.status.toUpperCase()) {
                case "PENDING" -> pending++;
                case "ACTIVE" -> active++;
                case "REJECTED" -> rejected++;
                case "COMPLETED" -> {
                    completed++;
                    if (s.currentPrice != null) {
                        revenue = revenue.add(s.currentPrice);
                    }
                }
                case "CANCELED" -> canceled++;
            }
        }

        DecimalFormat df = new DecimalFormat("#,##0.##");
        statsArea.setText(
                "Tổng số phiên: " + allSessions.size() + "\n" +
                        "Số phiên chờ duyệt: " + pending + "\n" +
                        "Số phiên đang hoạt động: " + active + "\n" +
                        "Số phiên bị từ chối: " + rejected + "\n" +
                        "Số phiên đã hoàn thành: " + completed + "\n" +
                        "Số phiên đã hủy: " + canceled + "\n" +
                        "Tổng doanh thu phiên hoàn thành: " + df.format(revenue)
        );
    }

    private void clearForm() {
        productNameField.clear();
        imageUrlField.clear();
        descriptionArea.clear();
        startingPriceField.clear();
        stepPriceField.clear();
        fillDefaultEndTime();
        productTypeCombo.setValue("Electronics");
    }

    private void fillDefaultEndTime() {
        if (endTimeField != null && endTimeField.getText().trim().isEmpty()) {
            endTimeField.setText(defaultEndTime());
        }
    }

    private String defaultEndTime() {
        return LocalDateTime.now().plusDays(7).withSecond(0).withNano(0).toString();
    }

    private String productNameOrEmpty(TextInputControl input) {
        return input == null ? "" : input.getText().trim();
    }

    private String safeMessage(String body) {
        if (body == null || body.isBlank()) return "Có lỗi xảy ra từ server.";
        return body;
    }

    private ApiResult parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        if (body == null || body.isBlank()) {
            return new ApiResult(httpStatus >= 200 && httpStatus < 300,
                    httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage : "Có lỗi xảy ra từ server.");
        }

        try {
            String trimmed = body.trim();
            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                int status = obj.optInt("status", httpStatus);
                String message = obj.optString("message",
                        status >= 200 && status < 300 ? defaultSuccessMessage : "Có lỗi xảy ra từ server.");
                return new ApiResult(status >= 200 && status < 300, message);
            }
        } catch (Exception ignored) {
        }

        return new ApiResult(httpStatus >= 200 && httpStatus < 300,
                httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage : safeMessage(body));
    }

    private ApiArrayResult extractDataArray(String body, int httpStatus) {
        if (body == null || body.isBlank()) {
            return new ApiArrayResult(false, "Không có dữ liệu từ server.", new JSONArray());
        }

        try {
            String trimmed = body.trim();

            if (trimmed.startsWith("[")) {
                return new ApiArrayResult(true, "OK", new JSONArray(trimmed));
            }

            JSONObject obj = new JSONObject(trimmed);
            int status = obj.optInt("status", httpStatus);
            String message = obj.optString("message", "Có lỗi xảy ra từ server.");

            if (status < 200 || status >= 300) {
                return new ApiArrayResult(false, message, new JSONArray());
            }

            Object data = obj.opt("data");
            if (data instanceof JSONArray) {
                return new ApiArrayResult(true, message, (JSONArray) data);
            }

            return new ApiArrayResult(true, message, new JSONArray());
        } catch (Exception e) {
            return new ApiArrayResult(false, "Không đọc được dữ liệu từ server.", new JSONArray());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class SessionItem {
        int id;
        String productName;
        String productType;
        String imageUrl;
        String description;
        BigDecimal startingPrice = BigDecimal.ZERO;
        BigDecimal currentPrice = BigDecimal.ZERO;
        BigDecimal stepPrice = BigDecimal.ZERO;
        String endTime;
        String status;
    }

    private static class ApiResult {
        boolean success;
        String message;

        ApiResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private static class ApiArrayResult {
        boolean success;
        String message;
        JSONArray data;

        ApiArrayResult(boolean success, String message, JSONArray data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }
}