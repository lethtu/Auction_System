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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(SellerDashboardController.class);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String MAIN_TEMPLATE_FXML = "MainTemplate.fxml";
    private static final int MAIN_TEMPLATE_WIDTH = 1024;
    private static final int MAIN_TEMPLATE_HEIGHT = 768;

    private static final String DEFAULT_PRODUCT_TYPE = "Electronics";
    private static final String DEFAULT_START_TIME = "00:00";
    private static final String DEFAULT_END_TIME = "23:59";
    private static final int DEFAULT_END_DATE_PLUS_DAYS = 7;

    private static final String CREATE_BUTTON_TEXT = "Tạo phiên";
    private static final String UPDATE_BUTTON_TEXT = "Lưu thay đổi";

    private static final String ERROR_TITLE = "Lỗi";
    private static final String NETWORK_ERROR_TITLE = "Lỗi mạng";
    private static final String DATA_ERROR_TITLE = "Lỗi dữ liệu";
    private static final String SUCCESS_TITLE = "Thành công";

    private static final List<String> PRODUCT_TYPES = List.of(DEFAULT_PRODUCT_TYPE, "Art", "Vehicle");

    @FXML private TabPane sellerTabPane;
    @FXML private Tab tabMySessions;
    @FXML private Tab tabCreateSession;
    @FXML private Tab tabStats;

    @FXML private ListView<String> mySessionsList;

    @FXML private ComboBox<String> productTypeCombo;
    @FXML private TextField productNameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField imagePathField;
    @FXML private TextField startingPriceField;
    @FXML private TextField stepPriceField;
    @FXML private TextField reservePriceField;
    @FXML private TextField endTimeField;

    @FXML private DatePicker datePickerStart;
    @FXML private TextField txtStartTime;
    @FXML private DatePicker datePickerEnd;
    @FXML private TextField txtEndTime;

    @FXML private TextArea statsArea;
    @FXML private Button btnCreateOrUpdate;

    private final SellerDashboardService sellerDashboardService = new SellerDashboardService();
    private final List<SessionItem> allSessions = new ArrayList<>();
    private final List<SessionItem> displayedSessions = new ArrayList<>();

    private SessionItem editingSession;
    private File selectedImageFile;

    @FXML
    public void initialize() {
        setupProductTypeCombo();
        initializeDateInputs();
        fillLegacyEndTimeField();
        loadMySessions();
        resetSubmitButton();
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = createImageFileChooser();
        File file = fileChooser.showOpenDialog(getCurrentWindow());

        if (file == null) {
            return;
        }

        selectedImageFile = file;
        imagePathField.setText(file.getName());
    }

    @FXML
    private void handleCreateSession() {
        if (isEditingSession()) {
            updateEditingSession();
            return;
        }

        createNewSession();
    }

    @FXML
    private void showMySessionsTab() {
        selectTab(tabMySessions);
    }

    @FXML
    private void showCreateAuctionTab() {
        selectTab(tabCreateSession);
    }

    @FXML
    private void showStatsTab() {
        selectTab(tabStats);
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

        enterEditMode(selected);
    }

    @FXML
    private void handleCancelSelectedSession() {
        SessionItem selected = getPendingSelectedSession("hủy");

        if (selected == null || !confirmCancelSession(selected.id)) {
            return;
        }

        runSellerMutation(sellerId -> sellerDashboardService.cancelSession(selected.id, sellerId));
    }

    @FXML
    private void goBack(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, MAIN_TEMPLATE_FXML, MAIN_TEMPLATE_WIDTH, MAIN_TEMPLATE_HEIGHT);
        } catch (IOException e) {
            logger.error("Không thể quay lại màn chính", e);
            AlertUtil.show(Alert.AlertType.ERROR, ERROR_TITLE, "Không thể quay lại màn chính.");
        }
    }

    private void setupProductTypeCombo() {
        productTypeCombo.setItems(FXCollections.observableArrayList(PRODUCT_TYPES));
        productTypeCombo.setValue(DEFAULT_PRODUCT_TYPE);
    }

    private FileChooser createImageFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        return fileChooser;
    }

    private Window getCurrentWindow() {
        if (btnCreateOrUpdate == null || btnCreateOrUpdate.getScene() == null) {
            return null;
        }

        return btnCreateOrUpdate.getScene().getWindow();
    }

    private boolean isEditingSession() {
        return editingSession != null;
    }

    private void createNewSession() {
        runSellerMutation(sellerId -> {
            CreateAuctionRequest request = buildCreateRequest(sellerId);
            return request == null
                    ? null
                    : sellerDashboardService.createAuction(request, selectedImageFile);
        });
    }

    private void updateEditingSession() {
        runSellerMutation(sellerId -> {
            CreateAuctionRequest request = buildUpdateRequest(sellerId);
            return request == null
                    ? null
                    : sellerDashboardService.updateSession(editingSession.id, sellerId, request, selectedImageFile);
        });
    }

    private CreateAuctionRequest buildCreateRequest(int sellerId) {
        return SellerAuctionFormBuilder.buildCreateRequest(
                sellerId,
                productTypeCombo,
                productNameField,
                descriptionArea,
                imagePathField,
                startingPriceField,
                stepPriceField,
                reservePriceField,
                buildStartDateTimeText(),
                buildEndDateTimeText()
        );
    }

    private CreateAuctionRequest buildUpdateRequest(int sellerId) {
        return SellerAuctionFormBuilder.buildUpdateRequest(
                sellerId,
                productTypeCombo,
                productNameField,
                descriptionArea,
                imagePathField,
                startingPriceField,
                stepPriceField,
                reservePriceField,
                buildStartDateTimeText(),
                buildEndDateTimeText()
        );
    }

    private void enterEditMode(SessionItem selected) {
        editingSession = selected;
        selectedImageFile = null;

        fillFormFromSession(selected);
        btnCreateOrUpdate.setText(UPDATE_BUTTON_TEXT);
        selectTab(tabCreateSession);

        AlertUtil.show(
                Alert.AlertType.INFORMATION,
                "Chế độ sửa",
                "Dữ liệu phiên đã được đưa vào form. Hãy chỉnh sửa rồi bấm Lưu thay đổi."
        );
    }

    private void fillFormFromSession(SessionItem session) {
        productNameField.setText(safeText(session.productName));
        productTypeCombo.setValue(getProductTypeOrDefault(session.productType));
        descriptionArea.setText(safeText(session.description));
        imagePathField.setText(safeText(session.imagePath));

        startingPriceField.setText(toEditableMoneyText(session.startingPrice));
        stepPriceField.setText(toEditableMoneyText(session.stepPrice));
        setTextIfPresent(reservePriceField, toEditableMoneyText(session.reservePrice));
        setTextIfPresent(endTimeField, safeText(session.endTime));

        fillEndDateTimeInputs(session.endTime);
    }

    private String getProductTypeOrDefault(String productType) {
        return productType == null || productType.isBlank()
                ? DEFAULT_PRODUCT_TYPE
                : productType;
    }

    private void runSellerMutation(SellerMutation mutation) {
        Integer sellerId = getValidSellerId();

        if (sellerId == null) {
            return;
        }

        try {
            ApiResult<Void> api = mutation.run(sellerId);

            if (api == null) {
                return;
            }

            handleMutationResult(api);
        } catch (NumberFormatException e) {
            AlertUtil.show(Alert.AlertType.ERROR, DATA_ERROR_TITLE, "Giá khởi điểm, bước giá và giá sàn phải là số.");
        } catch (IllegalArgumentException e) {
            AlertUtil.show(Alert.AlertType.ERROR, DATA_ERROR_TITLE, safeErrorMessage(e));
        } catch (IllegalStateException e) {
            logger.error("Lỗi xử lý seller", e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi thao tác", safeErrorMessage(e));
        } catch (IOException e) {
            logger.error("Lỗi kết nối hoặc đọc file", e);
            AlertUtil.show(Alert.AlertType.ERROR, NETWORK_ERROR_TITLE, "Không thể kết nối đến máy chủ hoặc đọc file ảnh.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thao tác seller bị gián đoạn", e);
            AlertUtil.show(Alert.AlertType.ERROR, ERROR_TITLE, "Thao tác bị gián đoạn. Vui lòng thử lại.");
        } catch (Exception e) {
            logger.error("Lỗi không xác định khi xử lý seller", e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi hệ thống", safeErrorMessage(e));
        }
    }

    private void handleMutationResult(ApiResult<Void> api) {
        if (api.success) {
            clearForm();
            loadMySessions();
            AlertUtil.show(Alert.AlertType.INFORMATION, SUCCESS_TITLE, safeApiMessage(api.message, "Thao tác thành công."));
            return;
        }

        logger.error("Lỗi API: {}", api.message);
        AlertUtil.show(Alert.AlertType.ERROR, ERROR_TITLE, safeApiMessage(api.message, "Thao tác thất bại."));
    }

    private void loadMySessions() {
        loadSessions(null, true, "Không thể tải dữ liệu seller từ server.");
    }

    private void loadSessionsByStatus(String status) {
        loadSessions(status, false, "Không thể lọc phiên từ server.");
    }

    private void loadSessions(String status, boolean refreshAllSessions, String errorMessage) {
        Integer sellerId = getValidSellerId();

        if (sellerId == null) {
            return;
        }

        try {
            List<SessionItem> sessions = status == null
                    ? sellerDashboardService.getMySessions(sellerId)
                    : sellerDashboardService.getMySessions(sellerId, status);

            if (refreshAllSessions) {
                allSessions.clear();
                allSessions.addAll(sessions);
                updateStats();
            }

            renderSessions(sessions);
        } catch (Exception e) {
            logger.error(errorMessage, e);
            AlertUtil.show(Alert.AlertType.ERROR, NETWORK_ERROR_TITLE, errorMessage);
        }
    }

    private void renderSessions(List<SessionItem> sessions) {
        displayedSessions.clear();

        if (sessions == null || sessions.isEmpty()) {
            mySessionsList.setItems(FXCollections.observableArrayList());
            return;
        }

        displayedSessions.addAll(sessions);

        List<String> renderedSessions = new ArrayList<>();
        for (int i = 0; i < sessions.size(); i++) {
            renderedSessions.add(sessions.get(i).toDisplayText(i + 1));
        }

        mySessionsList.setItems(FXCollections.observableArrayList(renderedSessions));
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

        if (!isPendingSession(selected)) {
            AlertUtil.show(
                    Alert.AlertType.WARNING,
                    "Không thể " + actionName,
                    "Chỉ được " + actionName + " phiên đang ở trạng thái PENDING."
            );
            return null;
        }

        return selected;
    }

    private boolean isPendingSession(SessionItem session) {
        return session != null && AuctionStatus.PENDING.equalsIgnoreCase(session.status);
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

    private void selectTab(Tab tab) {
        if (sellerTabPane != null && tab != null) {
            sellerTabPane.getSelectionModel().select(tab);
        }
    }

    private void initializeDateInputs() {
        setDefaultStartTimeIfBlank();
        setDefaultEndDateIfMissing();
        setDefaultEndTimeIfBlank();
    }

    private void setDefaultStartTimeIfBlank() {
        if (txtStartTime != null && txtStartTime.getText().isBlank()) {
            txtStartTime.setText(DEFAULT_START_TIME);
        }
    }

    private void setDefaultEndDateIfMissing() {
        if (datePickerEnd != null && datePickerEnd.getValue() == null) {
            datePickerEnd.setValue(LocalDate.now().plusDays(DEFAULT_END_DATE_PLUS_DAYS));
        }
    }

    private void setDefaultEndTimeIfBlank() {
        if (txtEndTime != null && txtEndTime.getText().isBlank()) {
            txtEndTime.setText(DEFAULT_END_TIME);
        }
    }

    private void fillLegacyEndTimeField() {
        if (endTimeField != null) {
            SellerAuctionFormBuilder.fillDefaultEndTime(endTimeField);
        }
    }

    private String buildStartDateTimeText() {
        if (datePickerStart == null || datePickerStart.getValue() == null) {
            return "";
        }

        return buildDateTimeText(datePickerStart.getValue(), txtStartTime, DEFAULT_START_TIME, "thời gian bắt đầu");
    }

    private String buildEndDateTimeText() {
        if (datePickerEnd == null || datePickerEnd.getValue() == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời gian kết thúc.");
        }

        return buildDateTimeText(datePickerEnd.getValue(), txtEndTime, DEFAULT_END_TIME, "thời gian kết thúc");
    }

    private String buildDateTimeText(LocalDate date, TextField timeField, String defaultTime, String fieldName) {
        String timeText = timeField == null || timeField.getText().isBlank()
                ? defaultTime
                : timeField.getText().trim();

        try {
            LocalTime time = LocalTime.parse(timeText, TIME_FORMAT);
            return LocalDateTime.of(date, time).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Định dạng " + fieldName + " phải là HH:mm, ví dụ 23:59.");
        }
    }

    private void fillEndDateTimeInputs(String endTime) {
        if (datePickerEnd == null || txtEndTime == null || endTime == null || endTime.isBlank()) {
            return;
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(endTime);
            datePickerEnd.setValue(dateTime.toLocalDate());
            txtEndTime.setText(dateTime.toLocalTime().format(TIME_FORMAT));
        } catch (DateTimeParseException e) {
            logger.warn("Không parse được endTime: {}", endTime);
        }
    }

    private void clearForm() {
        editingSession = null;
        selectedImageFile = null;

        clearTextInputs();
        resetDateInputs();
        productTypeCombo.setValue(DEFAULT_PRODUCT_TYPE);
        resetSubmitButton();
    }

    private void clearTextInputs() {
        productNameField.clear();
        descriptionArea.clear();
        imagePathField.clear();
        startingPriceField.clear();
        stepPriceField.clear();

        clearIfPresent(reservePriceField);
        clearIfPresent(endTimeField);

        fillLegacyEndTimeField();
    }

    private void resetDateInputs() {
        if (datePickerStart != null) {
            datePickerStart.setValue(null);
        }

        setTextIfPresent(txtStartTime, DEFAULT_START_TIME);

        if (datePickerEnd != null) {
            datePickerEnd.setValue(LocalDate.now().plusDays(DEFAULT_END_DATE_PLUS_DAYS));
        }

        setTextIfPresent(txtEndTime, DEFAULT_END_TIME);
    }

    private void resetSubmitButton() {
        if (btnCreateOrUpdate != null) {
            btnCreateOrUpdate.setText(CREATE_BUTTON_TEXT);
        }
    }

    private Integer getValidSellerId() {
        Integer sellerId = User.getId();

        if (sellerId == null || sellerId <= 0) {
            logger.error("Không lấy được sellerId từ session");
            AlertUtil.show(Alert.AlertType.ERROR, ERROR_TITLE, "Không lấy được sellerId từ session.");
            return null;
        }

        return sellerId;
    }

    private String safeErrorMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? "Có lỗi không xác định. Vui lòng xem log để biết chi tiết."
                : message;
    }

    private String safeApiMessage(String message, String defaultMessage) {
        return message == null || message.isBlank() ? defaultMessage : message;
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

    private void setTextIfPresent(TextInputControl control, String text) {
        if (control != null) {
            control.setText(text);
        }
    }

    private void clearIfPresent(TextInputControl control) {
        if (control != null) {
            control.clear();
        }
    }

    @FunctionalInterface
    private interface SellerMutation {
        ApiResult<Void> run(int sellerId) throws Exception;
    }
}