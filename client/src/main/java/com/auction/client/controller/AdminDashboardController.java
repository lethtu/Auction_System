package com.auction.client.controller;

import com.auction.client.dto.ApiResult;
import com.auction.client.model.AdminSessionRow;
import com.auction.client.model.AdminUserRow;
import com.auction.client.model.PendingSessionRow;
import com.auction.client.model.User;
import com.auction.client.service.AdminDashboardService;
import com.auction.client.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminDashboardController {
    private static final String LOGIN_FXML = "Login.fxml";
    private static final int LOGIN_WIDTH = 1000;
    private static final int LOGIN_HEIGHT = 700;

    private static final String CURRENCY_SUFFIX = " VND";
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.05");
    private static final String USER_BANNED_TEXT = "Banned";
    private static final String USER_ACTIVE_TEXT = "Active";
    private static final String PRODUCT_VISIBLE_TEXT = "Visible";
    private static final String PRODUCT_HIDDEN_TEXT = "Hidden";
    private static final String ACTIVE_NAV_CLASS = "active-admin-nav";

    private static final String NO_SESSION_SELECTED_TITLE = "No session selected";
    private static final String NO_USER_SELECTED_TITLE = "No user selected";

    private static final String INVALID_ADMIN_ID_MESSAGE = "Cannot get current admin ID.";
    private static final String INVALID_API_RESULT_MESSAGE = "Server did not return a valid result.";

    private final AdminDashboardService adminDashboardService = new AdminDashboardService();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private final ExecutorService dataExecutor = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "admin-dashboard-load");
        thread.setDaemon(true);
        return thread;
    });

    @FXML private TabPane adminTabPane;
    @FXML private Tab tabPending;
    @FXML private Tab tabUsers;
    @FXML private Tab tabSessions;

    @FXML private Button btnPendingTab;
    @FXML private Button btnUsersTab;
    @FXML private Button btnSessionsTab;

    @FXML private Label lblPendingCount;
    @FXML private Label lblUserCount;
    @FXML private Label lblSessionCount;
    @FXML private Label lblTotalStartingPrice;

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
    @FXML private TableColumn<AdminSessionRow, Boolean> colProductVisible;

    @FXML
    public void initialize() {
        setupPendingColumns();
        setupUserColumns();
        setupSessionColumns();
        setupAdminTablePolish();
        setupAdminNavigationState();
        loadAllData();
    }


    private void setupAdminTablePolish() {
        polishTable(tablePending, "No sessions pending review.");
        polishTable(tableUsers, "No users to display.");
        polishTable(tableSessions, "No auction sessions to display.");
    }

    private void polishTable(TableView<?> table, String emptyMessage) {
        if (table == null) {
            return;
        }

        Label placeholder = new Label(emptyMessage);
        placeholder.getStyleClass().add("admin-table-placeholder");
        table.setPlaceholder(placeholder);
        table.setFixedCellSize(36);
    }

    private void setupAdminNavigationState() {
        if (adminTabPane == null) {
            return;
        }

        adminTabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTab, selectedTab) -> updateAdminNavigationState(selectedTab)
        );
        updateAdminNavigationState(adminTabPane.getSelectionModel().getSelectedItem());
    }

    private void updateAdminNavigationState(Tab selectedTab) {
        setNavButtonActive(btnPendingTab, selectedTab == tabPending);
        setNavButtonActive(btnUsersTab, selectedTab == tabUsers);
        setNavButtonActive(btnSessionsTab, selectedTab == tabSessions);
    }

    private void setNavButtonActive(Button button, boolean active) {
        if (button == null) {
            return;
        }

        List<String> styleClasses = button.getStyleClass();

        if (active && !styleClasses.contains(ACTIVE_NAV_CLASS)) {
            styleClasses.add(ACTIVE_NAV_CLASS);
        } else if (!active) {
            styleClasses.remove(ACTIVE_NAV_CLASS);
        }
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
        colProductVisible.setCellValueFactory(cellData -> cellData.getValue().productVisibleProperty());

        setupPriceColumn(colSessionPrice);
        setupProductVisibleColumn();
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

    private void setupProductVisibleColumn() {
        colProductVisible.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean visible, boolean empty) {
                super.updateItem(visible, empty);
                setText(empty || visible == null ? null : formatProductVisibleStatus(visible));
            }
        });
    }

    private String formatBannedStatus(boolean banned) {
        return banned ? USER_BANNED_TEXT : USER_ACTIVE_TEXT;
    }

    private String formatProductVisibleStatus(boolean visible) {
        return visible ? PRODUCT_VISIBLE_TEXT : PRODUCT_HIDDEN_TEXT;
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
                "Please select a session to approve.",
                (selected, adminId) -> adminDashboardService.approveSession(selected.getId(), adminId)
        );
    }

    @FXML
    public void handleReject() {
        PendingSessionRow selected = getSelectedItem(
                tablePending,
                NO_SESSION_SELECTED_TITLE,
                "Please select a session to reject."
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
                "Please select a user to ban.",
                (selected, adminId) -> adminDashboardService.banUser(selected.getId(), adminId)
        );
    }

    @FXML
    public void handleRestoreUser() {
        AdminUserRow selected = getSelectedItem(
                tableUsers,
                NO_USER_SELECTED_TITLE,
                "Please select a user to restore."
        );

        if (selected == null) {
            return;
        }

        if (!selected.isBanned()) {
            AlertUtil.showInfo("This account is already active, no need to restore.");
            return;
        }

        Integer adminId = getValidAdminId();

        if (adminId == null) {
            return;
        }

        runAction(() -> adminDashboardService.restoreUser(selected.getId(), adminId));
    }


    @FXML
    public void handleCancelAuction() {
        runSelectedAdminAction(
                tableSessions,
                NO_SESSION_SELECTED_TITLE,
                "Please select a session to cancel.",
                (selected, adminId) -> adminDashboardService.cancelAuction(selected.getId(), adminId)
        );
    }

    @FXML
    public void handleHideProduct() {
        runSelectedAdminAction(
                tableSessions,
                NO_SESSION_SELECTED_TITLE,
                "Please select a product to hide.",
                (selected, adminId) -> adminDashboardService.hideProduct(selected.getProductId(), adminId)
        );
    }

    @FXML
    public void handleShowProduct() {
        runSelectedAdminAction(
                tableSessions,
                NO_SESSION_SELECTED_TITLE,
                "Please select a product to show.",
                (selected, adminId) -> adminDashboardService.showProduct(selected.getProductId(), adminId)
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
            AlertUtil.showError(e, "Cannot log out.");
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
        String reason = AlertUtil.promptText("Reject Session", "Enter rejection reason:")
                .orElse("")
                .trim();

        if (reason.isEmpty()) {
            AlertUtil.showWarning("Missing reason", "Please enter a rejection reason.");
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
            AlertUtil.showError(e, "Cannot perform admin operation.");
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

    private void updateDashboardMetrics() {
        setMetricText(lblPendingCount, String.valueOf(itemCount(tablePending)));
        setMetricText(lblUserCount, String.valueOf(itemCount(tableUsers)));
        setMetricText(lblSessionCount, String.valueOf(itemCount(tableSessions)));
        setMetricText(lblTotalStartingPrice, formatMetricCurrency(calculateTotalPlatformFee()));
    }

    private void setMetricText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private int itemCount(TableView<?> table) {
        if (table == null || table.getItems() == null) {
            return 0;
        }

        return table.getItems().size();
    }

    private BigDecimal calculateTotalPlatformFee() {
        if (tableSessions == null || tableSessions.getItems() == null) {
            return BigDecimal.ZERO;
        }

        return tableSessions.getItems()
                .stream()
                .filter(this::isCompletedSellerSale)
                .map(AdminSessionRow::getCurrentPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(PLATFORM_FEE_RATE);
    }

    private boolean isCompletedSellerSale(AdminSessionRow row) {
        if (row == null || row.getStatus() == null) {
            return false;
        }

        String status = row.getStatus().trim().toUpperCase(Locale.ROOT);
        return "ENDED".equals(status)
                || "PAID".equals(status)
                || "SOLD".equals(status)
                || "COMPLETED".equals(status);
    }

    private String formatMetricCurrency(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return "0 VND";
        }

        BigDecimal billion = BigDecimal.valueOf(1_000_000_000L);
        BigDecimal million = BigDecimal.valueOf(1_000_000L);
        BigDecimal thousand = BigDecimal.valueOf(1_000L);

        if (price.abs().compareTo(billion) >= 0) {
            return compactMoney(price, billion, "B VND");
        }
        if (price.abs().compareTo(million) >= 0) {
            return compactMoney(price, million, "M VND");
        }
        if (price.abs().compareTo(thousand) >= 0) {
            return compactMoney(price, thousand, "K VND");
        }

        return formatCurrency(price);
    }

    private String compactMoney(BigDecimal price, BigDecimal unit, String suffix) {
        BigDecimal compact = price.divide(unit, 1, RoundingMode.HALF_UP).stripTrailingZeros();
        return compact.toPlainString().replace('.', ',') + suffix;
    }

    private void loadPendingSessions() {
        loadTableData(
                tablePending,
                adminDashboardService::getPendingSessions,
                "Cannot load pending data from server."
        );
    }

    private void loadUsers() {
        loadTableData(
                tableUsers,
                adminDashboardService::getAllUsers,
                "Cannot load user list from server."
        );
    }

    private void loadSessions() {
        loadTableData(
                tableSessions,
                adminDashboardService::getAllSessions,
                "Cannot load session list from server."
        );
    }

    private <T> void loadTableData(
            TableView<T> table,
            TableDataLoader<T> dataLoader,
            String errorMessage
    ) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return dataLoader.load();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, dataExecutor).whenComplete((rows, error) -> Platform.runLater(() -> {
            if (error != null) {
                Throwable cause = error.getCause() == null ? error : error.getCause();
                if (cause instanceof Exception exception) {
                    AlertUtil.showError(exception, errorMessage);
                } else {
                    AlertUtil.showError(errorMessage, cause.getMessage() == null ? errorMessage : cause.getMessage());
                }
                return;
            }
            table.setItems(FXCollections.observableArrayList(rows));
            updateDashboardMetrics();
        }));
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

    @FXML
    private void handleMinimize(javafx.event.ActionEvent event) {
        SceneSwitcher.handleMinimize(event);
    }

    @FXML
    private void handleMaximize(javafx.event.ActionEvent event) {
        SceneSwitcher.handleMaximize(event);
    }

    @FXML
    private void handleClose(javafx.event.ActionEvent event) {
        SceneSwitcher.handleClose(event);
    }
}
