package com.auction.client.controller;

import com.auction.client.common.AuctionStatus;
import com.auction.client.dto.ApiResult;
import com.auction.client.dto.CreateAuctionRequest;
import com.auction.client.model.SessionItem;
import com.auction.client.model.User;
import com.auction.client.service.SellerDashboardService;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.SellerAuctionFormBuilder;
import com.auction.client.util.SellerStatsCalculator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(SellerDashboardController.class);

    @FXML private TabPane sellerTabPane;
    @FXML private Tab tabCreateSession;
    @FXML private ListView<String> mySessionsList;
    @FXML private ComboBox<String> productTypeCombo;
    @FXML private TextField productNameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField imagePathField;
    @FXML private TextField startingPriceField;
    @FXML private TextField stepPriceField;
    @FXML private TextField endTimeField;
    @FXML private TextArea statsArea;
    @FXML private Button btnCreateOrUpdate;

    private final SellerDashboardService sellerDashboardService = new SellerDashboardService();

    private final List<SessionItem> allSessions = new ArrayList<>();
    private final List<SessionItem> displayedSessions = new ArrayList<>();

    private SessionItem editingSession;
    private File selectedImageFile;

    @FXML
    public void initialize() {
        productTypeCombo.setItems(FXCollections.observableArrayList("Electronics", "Art", "Vehicle"));
        productTypeCombo.setValue("Electronics");

        SellerAuctionFormBuilder.fillDefaultEndTime(endTimeField);

        loadMySessions();
        resetSubmitButton();
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File file = fileChooser.showOpenDialog(btnCreateOrUpdate.getScene().getWindow());
        if (file == null) {
            return;
        }

        selectedImageFile = file;
        imagePathField.setText(file.getName());
    }

    @FXML
    private void handleCreateSession() {
        if (editingSession != null) {
            updateEditingSession();
            return;
        }

        createNewSession();
    }

    private void createNewSession() {
        runSellerMutation(sellerId -> {
            CreateAuctionRequest request = SellerAuctionFormBuilder.buildCreateRequest(
                    sellerId,
                    productTypeCombo,
                    productNameField,
                    descriptionArea,
                    imagePathField,
                    startingPriceField,
                    stepPriceField,
                    endTimeField
            );

            return request == null ? null : sellerDashboardService.createAuction(request, selectedImageFile);
        });
    }

    private void updateEditingSession() {
        runSellerMutation(sellerId -> {
            CreateAuctionRequest request = SellerAuctionFormBuilder.buildUpdateRequest(
                    sellerId,
                    productTypeCombo,
                    productNameField,
                    descriptionArea,
                    imagePathField,
                    startingPriceField,
                    stepPriceField,
                    endTimeField
            );

            return request == null
                    ? null
                    : sellerDashboardService.updateSession(editingSession.id, sellerId, request, selectedImageFile);
        });
    }

    @FXML
    private void handleShowAllSessions() {
        loadMySessions();
    }

    @FXML
    private void handleShowPendingSessions() {
        loadSessionsByStatus(AuctionStatus.PENDING);
    }

    @FXML
    private void handleShowActiveSessions() {
        loadSessionsByStatus(AuctionStatus.ACTIVE);
    }

    @FXML
    private void handleShowRejectedSessions() {
        loadSessionsByStatus(AuctionStatus.REJECTED);
    }

    @FXML
    private void handleEditSelectedSession() {
        SessionItem selected = getPendingSelectedSession("sửa");

        if (selected == null) {
            return;
        }

        editingSession = selected;
        fillFormFromSession(selected);
        btnCreateOrUpdate.setText("Lưu thay đổi");

        if (sellerTabPane != null && tabCreateSession != null) {
            sellerTabPane.getSelectionModel().select(tabCreateSession);
        }

        AlertUtil.show(
                Alert.AlertType.INFORMATION,
                "Chế độ sửa",
                "Dữ liệu phiên đã được đưa vào form. Hãy chỉnh sửa rồi bấm Lưu thay đổi."
        );
    }

    @FXML
    private void handleCancelSelectedSession() {
        SessionItem selected = getPendingSelectedSession("hủy");

        if (selected == null) {
            return;
        }

        if (!confirmCancelSession(selected.id)) {
            return;
        }

        runSellerMutation(sellerId -> sellerDashboardService.cancelSession(selected.id, sellerId));
    }

    @FXML
    private void goBack(javafx.event.ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1024, 768);
    }

    private void fillFormFromSession(SessionItem session) {
        productNameField.setText(safeText(session.productName));
        productTypeCombo.setValue(safeText(session.productType));
        descriptionArea.setText(safeText(session.description));
        imagePathField.setText(safeText(session.imagePath));
        startingPriceField.setText(toEditableMoneyText(session.startingPrice));
        stepPriceField.setText(toEditableMoneyText(session.stepPrice));
        if (endTimeField != null) {
            endTimeField.setText(safeText(session.endTime));
        }
        selectedImageFile = null;
    }

    private void runSellerMutation(SellerMutation mutation) {
        Integer sellerId = getValidSellerId();

        if (sellerId == null) {
            return;
        }

        try {
            ApiResult<Void> api = mutation.run(sellerId);

            if (api != null) {
                handleMutationResult(api);
            }
        } catch (NumberFormatException e) {
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Giá khởi điểm và bước giá phải là số.");
        } catch (IllegalArgumentException e) {
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi dữ liệu", e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("Lỗi xử lý seller: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi thao tác", e.getMessage());
        } catch (IOException e) {
            logger.error("Lỗi kết nối hoặc đọc file: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ hoặc đọc file ảnh.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thao tác seller bị gián đoạn: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", "Thao tác bị gián đoạn. Vui lòng thử lại.");
        } catch (Exception e) {
            logger.error("Lỗi không xác định khi xử lý seller: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi hệ thống", safeErrorMessage(e));
        }
    }

    private void handleMutationResult(ApiResult<Void> api) {
        if (api.success) {
            clearForm();
            loadMySessions();
            AlertUtil.show(Alert.AlertType.INFORMATION, "Thành công", api.message);
            return;
        }

        logger.error("Lỗi API: {}", api.message);
        AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", api.message);
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

    private void loadSessionsByStatus(String status) {
        Integer sellerId = getValidSellerId();

        if (sellerId == null) {
            return;
        }

        try {
            List<SessionItem> sessions = sellerDashboardService.getMySessions(sellerId, status);
            renderSessions(sessions);

        } catch (Exception e) {
            logger.error("Lỗi không thể lọc phiên: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể lọc phiên từ server.");
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

    private SessionItem getSelectedSession() {
        int index = mySessionsList.getSelectionModel().getSelectedIndex();

        if (index < 0 || index >= displayedSessions.size()) {
            return null;
        }

        return displayedSessions.get(index);
    }

    private SessionItem getPendingSelectedSession(String actionName) {
        SessionItem selected = getSelectedSession();

        if (selected == null) {
            AlertUtil.show(
                    Alert.AlertType.WARNING,
                    "Chưa chọn phiên",
                    "Bạn hãy chọn một phiên để " + actionName + "."
            );
            return null;
        }

        if (!AuctionStatus.PENDING.equalsIgnoreCase(selected.status)) {
            AlertUtil.show(
                    Alert.AlertType.WARNING,
                    "Không thể " + actionName,
                    "Chỉ được " + actionName + " phiên đang ở trạng thái PENDING."
            );
            return null;
        }

        return selected;
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
        editingSession = null;
        selectedImageFile = null;

        productNameField.clear();
        descriptionArea.clear();
        imagePathField.clear();
        startingPriceField.clear();
        stepPriceField.clear();

        if (endTimeField != null) {
            endTimeField.clear();
            SellerAuctionFormBuilder.fillDefaultEndTime(endTimeField);
        }

        productTypeCombo.setValue("Electronics");
        resetSubmitButton();
    }

    private void resetSubmitButton() {
        if (btnCreateOrUpdate != null) {
            btnCreateOrUpdate.setText("Tạo phiên");
        }
    }

    private String safeErrorMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? "Có lỗi không xác định. Vui lòng xem log để biết chi tiết."
                : message;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String toEditableMoneyText(BigDecimal value) {
        if (value == null) {
            return "";
        }

        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0
                ? normalized.setScale(0).toPlainString()
                : normalized.toPlainString();
    }

    @FunctionalInterface
    private interface SellerMutation {
        ApiResult<Void> run(int sellerId) throws Exception;
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