package com.auction.client.controller;

import com.auction.client.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardController {

    @FXML private ListView<String> mySessionsList;
    @FXML private ComboBox<String> productTypeCombo;
    @FXML private TextField productNameField;
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startingPriceField;
    @FXML private TextField stepPriceField;
    @FXML private TextField endTimeField;
    @FXML private TextArea statsArea;

    @FXML private Tab tabMySessions;
    @FXML private Tab tabCreateSession;
    @FXML private TabPane sellerTabPane;

    @FXML private Button btnCreateOrUpdate;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final List<SessionItem> allSessions = new ArrayList<>();
    private final List<SessionItem> displayedSessions = new ArrayList<>();
    private Integer editingSessionId = null;

    @FXML
    public void initialize() {
        productTypeCombo.setItems(FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"
        ));
        productTypeCombo.setValue("Electronics");
        resetCreateButton();
        loadMySessions();
    }

    @FXML
    private void handleCreateSession() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String imageUrl = imageUrlField.getText().trim();
        String description = descriptionArea.getText().trim();
        String startingPriceText = startingPriceField.getText().trim();
        String stepPriceText = stepPriceField.getText().trim();
        String endTime = endTimeField.getText().trim();

        if (productName.isEmpty() || productType == null || startingPriceText.isEmpty()
                || stepPriceText.isEmpty() || endTime.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại, giá khởi điểm, bước giá và thời gian kết thúc.");
            return;
        }

        try {
            double startingPrice = Double.parseDouble(startingPriceText);
            double stepPrice = Double.parseDouble(stepPriceText);

            JSONObject body = new JSONObject();
            body.put("productName", productName);
            body.put("productType", productType);
            body.put("imageUrl", imageUrl);
            body.put("description", description);
            body.put("startingPrice", startingPrice);
            body.put("stepPrice", stepPrice);
            body.put("endTime", endTime);
            body.put("sellerId", sellerId);

            HttpRequest request;
            String successMessage;

            if (editingSessionId == null) {
                request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/seller/create"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();
                successMessage = "Tạo phiên đấu giá thành công.";
            } else {
                request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/seller/update-session/" + editingSessionId + "?sellerId=" + sellerId))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();
                successMessage = "Cập nhật phiên chờ duyệt thành công.";
            }

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                clearForm();
                editingSessionId = null;
                resetCreateButton();
                loadMySessions();
                sellerTabPane.getSelectionModel().select(tabMySessions);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", safeMessage(response.body()));
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Giá khởi điểm và bước giá phải là số.");
        } catch (Exception e) {
            e.printStackTrace();
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
        SessionItem selected = getSelectedSession();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn phiên", "Bạn hãy chọn một phiên để sửa.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selected.status)) {
            showAlert(Alert.AlertType.WARNING, "Không thể sửa",
                    "Chỉ được sửa phiên đang ở trạng thái PENDING.");
            return;
        }

        Integer sellerId = User.getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/seller/session-detail/" + selected.id + "?sellerId=" + sellerId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                showAlert(Alert.AlertType.ERROR, "Lỗi API", safeMessage(response.body()));
                return;
            }

            JSONObject item = new JSONObject(response.body());
            SessionItem detail = parseSession(item);

            editingSessionId = detail.id;

            productNameField.setText(detail.productName);
            productTypeCombo.setValue(detail.productType == null || detail.productType.isBlank()
                    ? "Electronics"
                    : detail.productType);
            imageUrlField.setText(detail.imageUrl);
            descriptionArea.setText(detail.description);
            startingPriceField.setText(cleanNumber(detail.startingPrice));
            stepPriceField.setText(cleanNumber(detail.stepPrice));
            endTimeField.setText(detail.endTime);

            btnCreateOrUpdate.setText("Lưu cập nhật");
            sellerTabPane.getSelectionModel().select(tabCreateSession);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể tải chi tiết phiên.");
        }
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
                    .uri(URI.create("http://localhost:8080/api/seller/cancel-session/" + selected.id + "?sellerId=" + sellerId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                if (editingSessionId != null && editingSessionId.equals(selected.id)) {
                    editingSessionId = null;
                    clearForm();
                    resetCreateButton();
                }
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã hủy phiên thành công.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", safeMessage(response.body()));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/seller/my-sessions/" + sellerId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tải được danh sách phiên.");
                return;
            }

            JSONArray array = new JSONArray(response.body());
            allSessions.clear();

            for (int i = 0; i < array.length(); i++) {
                allSessions.add(parseSession(array.getJSONObject(i)));
            }

            renderSessions(allSessions);
            updateStats();

        } catch (Exception e) {
            e.printStackTrace();
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

        s.startingPrice = item.optDouble("startingPrice", 0.0);
        s.currentPrice = item.optDouble("currentPrice", 0.0);
        s.stepPrice = item.optDouble("stepPrice", 0.0);
        s.endTime = item.optString("endTime", "");
        s.status = item.optString("status", "UNKNOWN");
        return s;
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
        double revenue = 0;

        for (SessionItem s : allSessions) {
            if (s.status == null) continue;
            switch (s.status.toUpperCase()) {
                case "PENDING" -> pending++;
                case "ACTIVE" -> active++;
                case "REJECTED" -> rejected++;
                case "COMPLETED" -> {
                    completed++;
                    revenue += s.currentPrice;
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
        endTimeField.clear();
        productTypeCombo.setValue("Electronics");
    }

    private void resetCreateButton() {
        if (btnCreateOrUpdate != null) {
            btnCreateOrUpdate.setText("Tạo phiên");
        }
    }

    private String cleanNumber(double value) {
        if (Math.floor(value) == value) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    private String safeMessage(String body) {
        if (body == null || body.isBlank()) return "Có lỗi xảy ra từ server.";
        return body;
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
        double startingPrice;
        double currentPrice;
        double stepPrice;
        String endTime;
        String status;
    }
}