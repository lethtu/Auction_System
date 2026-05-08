package com.auction.client.controller;

import com.auction.client.dto.ApiResult;
import com.auction.client.model.AdminSessionRow;
import com.auction.client.model.AdminUserRow;
import com.auction.client.model.PendingSessionRow;
import com.auction.client.model.User;
import com.auction.client.service.AdminDashboardService;
import com.auction.client.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService = new AdminDashboardService();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

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

        colBanned.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean banned, boolean empty) {
                super.updateItem(banned, empty);
                setText(empty || banned == null ? null : (banned ? "Đã khóa" : "Hoạt động"));
            }
        });
    }

    private void setupSessionColumns() {
        colSessionId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colSessionProduct.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        colSessionSeller.setCellValueFactory(cellData -> cellData.getValue().sellerUsernameProperty());
        colSessionPrice.setCellValueFactory(cellData -> cellData.getValue().startingPriceProperty());
        colSessionStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        setupPriceColumn(colSessionPrice);
    }

    private <T> void setupPriceColumn(TableColumn<T, BigDecimal> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : currencyFormat.format(price) + " VND");
            }
        });
    }

    @FXML
    public void handleApprove() {
        PendingSessionRow selected = getSelectedItem(
                tablePending,
                "Chưa chọn phiên",
                "Hãy chọn một phiên để phê duyệt."
        );

        Integer adminId = getValidAdminId();

        if (selected == null || adminId == null) {
            return;
        }

        runAction(() -> adminDashboardService.approveSession(selected.getId(), adminId));
    }

    @FXML
    public void handleReject() {
        PendingSessionRow selected = getSelectedItem(
                tablePending,
                "Chưa chọn phiên",
                "Hãy chọn một phiên để từ chối."
        );

        Integer adminId = getValidAdminId();

        if (selected == null || adminId == null) {
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
        AdminUserRow selected = getSelectedItem(
                tableUsers,
                "Chưa chọn user",
                "Hãy chọn user cần khóa."
        );

        Integer adminId = getValidAdminId();

        if (selected == null || adminId == null) {
            return;
        }

        runAction(() -> adminDashboardService.banUser(selected.getId(), adminId));
    }

    @FXML
    public void handleCancelAuction() {
        AdminSessionRow selected = getSelectedItem(
                tableSessions,
                "Chưa chọn phiên",
                "Hãy chọn phiên cần hủy."
        );

        Integer adminId = getValidAdminId();

        if (selected == null || adminId == null) {
            return;
        }

        runAction(() -> adminDashboardService.cancelAuction(selected.getId(), adminId));
    }

    @FXML
    public void handleRefresh() {
        loadAllData();
    }

    private <T> T getSelectedItem(TableView<T> table, String title, String message) {
        T selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            AlertUtil.show(Alert.AlertType.WARNING, title, message);
        }

        return selected;
    }

    private Integer getValidAdminId() {
        Integer adminId = User.getId();

        if (adminId == null || adminId <= 0) {
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", "Không lấy được ID admin hiện tại.");
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
            AlertUtil.show(Alert.AlertType.WARNING, "Thiếu lý do", "Vui lòng nhập lý do từ chối.");
            return null;
        }

        return reason;
    }

    private void runAction(AdminAction action) {
        try {
            ApiResult<Void> api = action.run();
            handleActionResult(api);
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ.");
        }
    }

    private void handleActionResult(ApiResult<Void> api) {
        if (api.success) {
            AlertUtil.show(Alert.AlertType.INFORMATION, "Thành công", api.message);
            loadAllData();
            return;
        }

        AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", api.message);
    }

    private void loadAllData() {
        loadPendingSessions();
        loadUsers();
        loadSessions();
    }

    private void loadPendingSessions() {
        try {
            List<PendingSessionRow> rows = adminDashboardService.getPendingSessions();
            tablePending.setItems(FXCollections.observableArrayList(rows));
        } catch (Exception e) {
            showLoadError(e, "Không thể tải dữ liệu pending từ server.");
        }
    }

    private void loadUsers() {
        try {
            List<AdminUserRow> rows = adminDashboardService.getAllUsers();
            tableUsers.setItems(FXCollections.observableArrayList(rows));
        } catch (Exception e) {
            showLoadError(e, "Không thể tải danh sách user từ server.");
        }
    }

    private void loadSessions() {
        try {
            List<AdminSessionRow> rows = adminDashboardService.getAllSessions();
            tableSessions.setItems(FXCollections.observableArrayList(rows));
        } catch (Exception e) {
            showLoadError(e, "Không thể tải danh sách phiên từ server.");
        }
    }

    private void showLoadError(Exception e, String defaultMessage) {
        e.printStackTrace();

        String message = e.getMessage();

        AlertUtil.show(
                Alert.AlertType.ERROR,
                "Lỗi",
                message == null || message.isBlank() ? defaultMessage : message
        );
    }

    @FunctionalInterface
    private interface AdminAction {
        ApiResult<Void> run() throws Exception;
    }
}