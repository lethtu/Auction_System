package com.auction.client.controller;

import com.auction.client.dto.ApiResult;
import com.auction.client.model.AdminSessionRow;
import com.auction.client.model.AdminUserRow;
import com.auction.client.model.PendingSessionRow;
import com.auction.client.model.User;
import com.auction.client.service.AdminDashboardService;
import com.auction.client.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminDashboardController {
    private static final String LOGIN_FXML = "Login.fxml";
    private static final int LOGIN_WIDTH = 400;
    private static final int LOGIN_HEIGHT = 500;

    private static final String CURRENCY_SUFFIX = " VND";
    private static final String USER_BANNED_TEXT = "Đã khóa";
    private static final String USER_ACTIVE_TEXT = "Hoạt động";

    private static final String NO_SESSION_SELECTED_TITLE = "Chưa chọn phiên";
    private static final String NO_USER_SELECTED_TITLE = "Chưa chọn user";

    private static final String INVALID_ADMIN_ID_MESSAGE = "Không lấy được ID admin hiện tại.";
    private static final String INVALID_API_RESULT_MESSAGE = "Server không trả về kết quả hợp lệ.";

    private final AdminDashboardService adminDashboardService = new AdminDashboardService();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

    @FXML private TabPane adminTabPane;
    @FXML private Tab tabPending;
    @FXML private Tab tabUsers;
    @FXML private Tab tabSessions;

    @FXML private TableView<PendingSessionRow> tablePending;
    @FXML private TableColumn<PendingSessionRow, Integer> colId;
    @FXML private TableColumn<PendingSessionRow, String> colProduct;
    @FXML private TableColumn<PendingSessionRow, BigDecimal> colPrice;

    @FXML private TableView<AdminUserRow> tableUsers;
    @FXML private TableColumn<AdminUserRow, Integer> colUserId;
    @FXML private TableColumn<AdminUserRow, String> colUsername;
    @FXML private TableColumn<AdminUserRow, String> colFullname;
    @FXML private TableColumn<AdminUserRow, String> colEmail;
    @FXML private TableColumn<AdminUserRow, String> colRole;
    @FXML private TableColumn<AdminUserRow, Boolean> colBanned;

    @FXML private TableView<AdminSessionRow> tableSessions;
    @FXML private TableColumn<AdminSessionRow, Integer> colSessionId;
    @FXML private TableColumn<AdminSessionRow, String> colSessionProduct;
    @FXML private TableColumn<AdminSessionRow, String> colSessionSeller;
    @FXML private TableColumn<AdminSessionRow, BigDecimal> colSessionPrice;
    @FXML private TableColumn<AdminSessionRow, String> colSessionStatus;

    @FXML
    public void initialize() {
        setupPendingColumns();
        setupUserColumns();
        setupSessionColumns();
        loadAllData();
    }

    private void setupPendingColumns() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colProduct.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        colPrice.setCellValueFactory(cellData -> cellData.getValue().startingPriceProperty());

        setupPriceColumn(colPrice);
    }

    private void setupUserColumns() {
        colUserId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colUsername.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        colFullname.setCellValueFactory(cellData -> cellData.getValue().fullnameProperty());
        colEmail.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        colRole.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
        colBanned.setCellValueFactory(cellData -> cellData.getValue().bannedProperty());

        setupBannedColumn();
    }

    private void setupSessionColumns() {
        colSessionId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colSessionProduct.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        colSessionSeller.setCellValueFactory(cellData -> cellData.getValue().sellerUsernameProperty());
        colSessionPrice.setCellValueFactory(cellData -> cellData.getValue().startingPriceProperty());
        colSessionStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        setupPriceColumn(colSessionPrice);
    }

    private void setupBannedColumn() {
        colBanned.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean banned, boolean empty) {
                super.updateItem(banned, empty);
                setText(empty || banned == null ? null : formatBannedStatus(banned));
            }
        });
    }

    private String formatBannedStatus(boolean banned) {
        return banned ? USER_BANNED_TEXT : USER_ACTIVE_TEXT;
    }

    private <T> void setupPriceColumn(TableColumn<T, BigDecimal> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : formatCurrency(price));
            }
        });
    }

    private String formatCurrency(BigDecimal price) {
        return currencyFormat.format(price) + CURRENCY_SUFFIX;
    }

    @FXML
    public void handleApprove() {
        runSelectedAdminAction(
                tablePending,
                NO_SESSION_SELECTED_TITLE,
                "Hãy chọn một phiên để phê duyệt.",
                (selected, adminId) -> adminDashboardService.approveSession(selected.getId(), adminId)
        );
    }

    @FXML
    public void handleReject() {
        PendingSessionRow selected = getSelectedItem(
                tablePending,
                NO_SESSION_SELECTED_TITLE,
                "Hãy chọn một phiên để từ chối."
        );

        if (selected == null) {
            return;
        }

        Integer adminId = getValidAdminId();

        if (adminId == null) {
            return;
        }

        String reason = askRejectReason();

        if (reason == null) {
            return;
        }

        runAction(() -> adminDashboardService.rejectSession(selected.getId(), adminId, reason));
    }

    @FXML
    public void handleBanUser() {
        runSelectedAdminAction(
                tableUsers,
                NO_USER_SELECTED_TITLE,
                "Hãy chọn user cần khóa.",
                (selected, adminId) -> adminDashboardService.banUser(selected.getId(), adminId)
        );
    }

    @FXML
    public void handleCancelAuction() {
        runSelectedAdminAction(
                tableSessions,
                NO_SESSION_SELECTED_TITLE,
                "Hãy chọn phiên cần hủy.",
                (selected, adminId) -> adminDashboardService.cancelAuction(selected.getId(), adminId)
        );
    }

    @FXML
    public void showPendingTab() {
        selectTab(tabPending);
    }

    @FXML
    public void showUsersTab() {
        selectTab(tabUsers);
    }

    @FXML
    public void showSessionsTab() {
        selectTab(tabSessions);
    }

    @FXML
    public void handleRefresh() {
        loadAllData();
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            User.clearSession();
            SceneSwitcher.switchScene(event, LOGIN_FXML, LOGIN_WIDTH, LOGIN_HEIGHT);
        } catch (Exception e) {
            AlertUtil.showError(e, "Không thể đăng xuất.");
        }
    }

    private <T> void runSelectedAdminAction(
            TableView<T> table,
            String noSelectionTitle,
            String noSelectionMessage,
            SelectedAdminAction<T> action
    ) {
        T selected = getSelectedItem(table, noSelectionTitle, noSelectionMessage);

        if (selected == null) {
            return;
        }

        Integer adminId = getValidAdminId();

        if (adminId == null) {
            return;
        }

        runAction(() -> action.run(selected, adminId));
    }

    private void selectTab(Tab tab) {
        if (adminTabPane != null && tab != null) {
            adminTabPane.getSelectionModel().select(tab);
        }
    }

    private <T> T getSelectedItem(TableView<T> table, String title, String message) {
        T selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            AlertUtil.showWarning(title, message);
        }

        return selected;
    }

    private Integer getValidAdminId() {
        Integer adminId = User.getId();

        if (adminId == null || adminId <= 0) {
            AlertUtil.showError(INVALID_ADMIN_ID_MESSAGE);
            return null;
        }

        return adminId;
    }

    private String askRejectReason() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Từ chối phiên");
        dialog.setHeaderText(null);
        dialog.setContentText("Nhập lý do từ chối:");

        String reason = dialog.showAndWait().orElse("").trim();

        if (reason.isEmpty()) {
            AlertUtil.showWarning("Thiếu lý do", "Vui lòng nhập lý do từ chối.");
            return null;
        }

        return reason;
    }

    private void runAction(AdminAction action) {
        try {
            ApiResult<Void> api = action.run();
            handleActionResult(api);
        } catch (IllegalStateException e) {
            AlertUtil.showError(e.getMessage());
        } catch (Exception e) {
            AlertUtil.showError(e, "Không thể thực hiện thao tác admin.");
        }
    }

    private void handleActionResult(ApiResult<Void> api) {
        if (api == null) {
            AlertUtil.showError(INVALID_API_RESULT_MESSAGE);
            return;
        }

        if (api.success) {
            AlertUtil.showInfo(api.message);
            loadAllData();
            return;
        }

        AlertUtil.showError(api.message);
    }

    private void loadAllData() {
        loadPendingSessions();
        loadUsers();
        loadSessions();
    }

    private void loadPendingSessions() {
        loadTableData(
                tablePending,
                adminDashboardService::getPendingSessions,
                "Không thể tải dữ liệu pending từ server."
        );
    }

    private void loadUsers() {
        loadTableData(
                tableUsers,
                adminDashboardService::getAllUsers,
                "Không thể tải danh sách user từ server."
        );
    }

    private void loadSessions() {
        loadTableData(
                tableSessions,
                adminDashboardService::getAllSessions,
                "Không thể tải danh sách phiên từ server."
        );
    }

    private <T> void loadTableData(
            TableView<T> table,
            TableDataLoader<T> dataLoader,
            String errorMessage
    ) {
        try {
            List<T> rows = dataLoader.load();
            table.setItems(FXCollections.observableArrayList(rows));
        } catch (Exception e) {
            AlertUtil.showError(e, errorMessage);
        }
    }

    @FunctionalInterface
    private interface AdminAction {
        ApiResult<Void> run() throws Exception;
    }

    @FunctionalInterface
    private interface SelectedAdminAction<T> {
        ApiResult<Void> run(T selected, int adminId) throws Exception;
    }

    @FunctionalInterface
    private interface TableDataLoader<T> {
        List<T> load() throws Exception;
    }
}