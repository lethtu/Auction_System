package com.auction.client.controller;

import com.auction.client.dto.ApiResult;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    private final AdminDashboardService adminDashboardService = new AdminDashboardService();

    @FXML
    private TableView<PendingSessionRow> tablePending;

    @FXML
    private TableColumn<PendingSessionRow, Integer> colId;

    @FXML
    private TableColumn<PendingSessionRow, String> colProduct;

    @FXML
    private TableColumn<PendingSessionRow, BigDecimal> colPrice;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadPendingSessions();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colProduct.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        colPrice.setCellValueFactory(cellData -> cellData.getValue().startingPriceProperty());

        setupPriceColumn();
    }

    private void setupPriceColumn() {
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : currencyFormat.format(price) + " VND");
            }
        });
    }

    @FXML
    public void handleApprove() {
        PendingSessionRow selected = getSelectedPendingSession();
        if (selected == null) {
            return;
        }

        Integer adminId = getValidAdminId();
        if (adminId == null) {
            return;
        }

        approveSelectedSession(selected, adminId);
    }

    private PendingSessionRow getSelectedPendingSession() {
        PendingSessionRow selected = tablePending.getSelectionModel().getSelectedItem();

        if (selected == null) {
            AlertUtil.show(Alert.AlertType.WARNING, "Chưa chọn phiên", "Hãy chọn một phiên để phê duyệt.");
            return null;
        }

        return selected;
    }

    private Integer getValidAdminId() {
        Integer adminId = User.getId();

        if (adminId == null || adminId <= 0) {
            logger.info("Không lấy được ID admin hiện tại");
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", "Không lấy được ID admin hiện tại.");
            return null;
        }

        return adminId;
    }

    private void approveSelectedSession(PendingSessionRow selected, int adminId) {
        try {
            ApiResult api = adminDashboardService.approveSession(selected.getId(), adminId);
            handleApproveResult(api);

        } catch (Exception e) {
            logger.error("Lỗi không kết nối được đến máy chủ: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ.");
        }
    }

    private void handleApproveResult(ApiResult api) {
        if (api.success) {
            AlertUtil.show(Alert.AlertType.INFORMATION, "Thành công", api.message);
            loadPendingSessions();
            return;
        }

        logger.info("Lỗi khi gọi API: {}", api.message);
        AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", api.message);
    }

    private void loadPendingSessions() {
        try {
            List<PendingSessionRow> rows = adminDashboardService.getPendingSessions();
            tablePending.setItems(FXCollections.observableArrayList(rows));

        } catch (Exception e) {
            logger.error("Lỗi không thể tải dữ liệu từ server: {}", e.getMessage(), e);
            AlertUtil.show(Alert.AlertType.ERROR, "Lỗi", getErrorMessage(e));
        }
    }

    private String getErrorMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? "Không thể tải dữ liệu pending từ server."
                : e.getMessage();
    }
}