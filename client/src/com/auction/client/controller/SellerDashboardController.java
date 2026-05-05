package com.auction.client.controller;

import com.auction.client.common.AuctionStatus;
import com.auction.client.dto.ApiResult;
import com.auction.client.dto.CreateAuctionRequest;
import com.auction.client.model.SessionItem;
import com.auction.client.model.User;
import com.auction.client.service.SellerDashboardService;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.SellerStatsCalculator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
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

    private final SellerDashboardService sellerDashboardService = new SellerDashboardService();

    private final List<SessionItem> allSessions = new ArrayList<>();
    private final List<SessionItem> displayedSessions = new ArrayList<>();

    @FXML
    public void initialize() {
        productTypeCombo.setItems(FXCollections.observableArrayList("Electronics", "Art", "Vehicle"));
        productTypeCombo.setValue("Electronics");
        fillDefaultEndTime();
        loadMySessions();
    }

    @FXML
    private void handleCreateSession() {
        Integer sellerId = getValidSellerId();
        if (sellerId == null) {
            return;
        }

        try {
            CreateAuctionRequest request = buildCreateAuctionRequest(sellerId);
            if (request == null) {
                return;
            }

            ApiResult api = sellerDashboardService.createAuction(request);
            handleCreateResult(api);

        } catch (NumberFormatException e) {
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Giá khởi điểm và bước giá phải là số.");
        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến máy chủ: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    private CreateAuctionRequest buildCreateAuctionRequest(int sellerId) {
        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String imageUrl = textOrEmpty(imageUrlField);
        String description = textOrEmpty(descriptionArea);
        String startingPriceText = textOrEmpty(startingPriceField);
        String stepPriceText = textOrEmpty(stepPriceField);
        String endTime = textOrEmpty(endTimeField);

        if (isCreateFormInvalid(productName, productType, startingPriceText, stepPriceText)) {
            AlertUtil.show(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại, giá khởi điểm và bước giá.");
            return null;
        }

        if (endTime.isEmpty()) {
            endTime = defaultEndTime();
            endTimeField.setText(endTime);
        }

        return new CreateAuctionRequest(
                productName,
                productType,
                imageUrl,
                description,
                new BigDecimal(startingPriceText.trim()),
                new BigDecimal(stepPriceText.trim()),
                endTime,
                sellerId
        );
    }

    private boolean isCreateFormInvalid(
            String productName,
            String productType,
            String startingPriceText,
            String stepPriceText
    ) {
        return productName.isEmpty()
                || productType == null
                || startingPriceText.isEmpty()
                || stepPriceText.isEmpty();
    }

    private void handleCreateResult(ApiResult api) {
        if (api.success) {
            clearForm();
            loadMySessions();
            AlertUtil.show(Alert.AlertType.INFORMATION, "Thành công", api.message);
            return;
        }

        logger.error("Lỗi api: {}", api.message);
        AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", api.message);
    }

    @FXML
    private void handleShowAllSessions() {
        renderSessions(allSessions);
    }

    @FXML
    private void handleShowPendingSessions() {
        renderSessions(filterByStatus(AuctionStatus.PENDING));
    }

    @FXML
    private void handleShowActiveSessions() {
        renderSessions(filterByStatus(AuctionStatus.ACTIVE));
    }

    @FXML
    private void handleShowRejectedSessions() {
        renderSessions(filterByStatus(AuctionStatus.REJECTED));
    }

    @FXML
    private void handleEditSelectedSession() {
        AlertUtil.show(Alert.AlertType.INFORMATION,
                "Tạm khóa chức năng",
                "Chức năng sửa phiên đang tạm khóa để bản demo ổn định hơn.\nBạn có thể demo tạo, xem, lọc, hủy phiên và phân quyền.");
    }

    @FXML
    private void handleCancelSelectedSession() {
        SessionItem selected = getCancelableSelectedSession();
        if (selected == null) {
            return;
        }

        Integer sellerId = getValidSellerId();
        if (sellerId == null) {
            return;
        }

        if (!confirmCancelSession(selected.id)) {
            return;
        }

        try {
            ApiResult api = sellerDashboardService.cancelSession(selected.id, sellerId);
            handleCancelResult(api);

        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến máy chủ: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    private SessionItem getCancelableSelectedSession() {
        SessionItem selected = getSelectedSession();

        if (selected == null) {
            AlertUtil.show(Alert.AlertType.WARNING, "Chưa chọn phiên", "Bạn hãy chọn một phiên để hủy.");
            return null;
        }

        if (!AuctionStatus.PENDING.equalsIgnoreCase(selected.status)) {
            AlertUtil.show(Alert.AlertType.WARNING, "Không thể hủy",
                    "Chỉ được hủy phiên đang ở trạng thái PENDING.");
            return null;
        }

        return selected;
    }

    private void handleCancelResult(ApiResult api) {
        if (api.success) {
            loadMySessions();
            AlertUtil.show(Alert.AlertType.INFORMATION, "Thành công", api.message);
            return;
        }

        AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", api.message);
    }

    @FXML
    private void goBack(javafx.event.ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1024, 768);
    }

    private void loadMySessions() {
        Integer sellerId = getValidSellerId();
        if (sellerId == null) {
            return;
        }

        try {
            allSessions.clear();
            allSessions.addAll(sellerDashboardService.getMySessions(sellerId));

            renderSessions(allSessions);
            updateStats();

        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến server: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể tải dữ liệu seller từ server.");
        }
    }

    private void renderSessions(List<SessionItem> sessions) {
        displayedSessions.clear();
        displayedSessions.addAll(sessions);

        List<String> rendered = new ArrayList<>();

        for (SessionItem session : sessions) {
            rendered.add(session.toDisplayText());
        }

        mySessionsList.setItems(FXCollections.observableArrayList(rendered));
    }

    private List<SessionItem> filterByStatus(String status) {
        List<SessionItem> filtered = new ArrayList<>();

        for (SessionItem session : allSessions) {
            if (session.status != null && session.status.equalsIgnoreCase(status)) {
                filtered.add(session);
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

    private boolean confirmCancelSession(int sessionId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn hủy phiên #" + sessionId + " không?");

        return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void updateStats() {
        statsArea.setText(SellerStatsCalculator.buildStatsText(allSessions));
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

    private String textOrEmpty(TextInputControl input) {
        return input == null ? "" : input.getText().trim();
    }

    private Integer getValidSellerId() {
        Integer sellerId = User.getId();

        if (sellerId == null || sellerId <= 0) {
            logger.error("Không lấy được sellerId từ session");
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return null;
        }

        return sellerId;
    }
}