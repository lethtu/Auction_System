package com.auction.client.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SidebarController {
    private static final Logger logger = LoggerFactory.getLogger(SidebarController.class);

    @FXML private ScrollPane sidebarContainer;
    @FXML private VBox sidebarContent;
    @FXML private Button btnSidebarDashboard;
    @FXML private Button btnMyBids;
    @FXML private Button btnSelling;
    @FXML private Button btnCategories;
    @FXML private Button btnSidebarWatchlist;
    @FXML private Button btnSupport;
    @FXML private Button btnStartSelling;

    public static boolean isSidebarCollapsed = false;
    private final Map<Button, String> sidebarButtonTextMap = new HashMap<>();

    public interface SidebarListener {
        void onFilterWatchlist();
        void onFilterMyBids();
        void onFilterMySessions();
        void onResetFilter();
        void onShowCategories();
    }

    private SidebarListener listener;
    private Runnable onBeforeNavigate;

    public void setSidebarListener(SidebarListener listener) {
        this.listener = listener;
    }

    public void setOnBeforeNavigate(Runnable onBeforeNavigate) {
        this.onBeforeNavigate = onBeforeNavigate;
    }

    @FXML
    public void initialize() {
        // Lưu lại text mặc định
        for (javafx.scene.Node node : sidebarContent.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                sidebarButtonTextMap.put(btn, btn.getText());
            }
        }
        if (isSidebarCollapsed) {
            isSidebarCollapsed = false;
            toggleSidebar();
        }
        Platform.runLater(() -> {
            if (sidebarContainer.getScene() != null) {
                sidebarContainer.getScene().widthProperty().addListener((obs, oldV, newV) -> {
                    if (newV.doubleValue() < 1100 && !isSidebarCollapsed) {
                        toggleSidebar();
                    }
                });
            }
        });
    }

    private void autoCollapse() {
        if (!isSidebarCollapsed) {
            toggleSidebar();
        }
    }

    public void forceCollapse() {
        autoCollapse();
    }

    public void toggleSidebar() {
        isSidebarCollapsed = !isSidebarCollapsed;

        if (isSidebarCollapsed) {
            // Collapse
            sidebarContainer.setMinWidth(70);
            sidebarContainer.setPrefWidth(70);
            sidebarContainer.setMaxWidth(70);
            sidebarContent.setPadding(new Insets(24, 0, 24, 0));
            sidebarContent.setAlignment(Pos.TOP_CENTER);

            for (javafx.scene.Node node : sidebarContent.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    String tooltipText = sidebarButtonTextMap.get(btn);
                    if (tooltipText != null && !tooltipText.isEmpty()) {
                        Tooltip tooltip = new Tooltip(tooltipText);
                        tooltip.setStyle("-fx-background-color: #e040a0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 6px 12px; -fx-font-size: 13px;");
                        
                        PauseTransition pause = new PauseTransition(Duration.millis(300));
                        pause.setOnFinished(e -> {
                            if (btn.isHover()) {
                                Bounds bounds = btn.localToScreen(btn.getBoundsInLocal());
                                tooltip.show(btn, bounds.getMaxX() + 15, bounds.getMinY() + btn.getHeight() / 2 - 18);
                            }
                        });

                        btn.setOnMouseEntered(e -> pause.playFromStart());
                        btn.setOnMouseExited(e -> {
                            pause.stop();
                            tooltip.hide();
                        });
                    }

                    btn.setTooltip(null);
                    btn.setText("");
                    btn.setPrefWidth(50);
                    btn.setMinWidth(50);
                    btn.setAlignment(Pos.CENTER);
                    if (btn.getGraphic() != null) {
                        btn.getGraphic().setTranslateX(0);
                    }
                } else if (node instanceof Label) {
                    node.setVisible(false);
                    node.setManaged(false);
                }
            }
        } else {
            // Expand
            sidebarContainer.setMinWidth(200);
            sidebarContainer.setPrefWidth(200);
            sidebarContainer.setMaxWidth(200);
            sidebarContent.setPadding(new Insets(24, 8, 24, 8));
            sidebarContent.setAlignment(Pos.TOP_LEFT);

            for (javafx.scene.Node node : sidebarContent.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    btn.setTooltip(null);
                    btn.setOnMouseEntered(null);
                    btn.setOnMouseExited(null);
                    String originalText = sidebarButtonTextMap.getOrDefault(btn, "");
                    btn.setText(originalText);
                    btn.setPrefWidth(165);
                    btn.setMinWidth(165);
                    btn.setAlignment(Pos.CENTER_LEFT);
                } else if (node instanceof Label) {
                    node.setVisible(true);
                    node.setManaged(true);
                }
            }
        }
    }

    public void setActiveWatchlist() {
        setActiveButton(btnSidebarWatchlist);
    }

    public void setActiveDashboard() {
        setActiveButton(btnSidebarDashboard);
    }

    public void setActiveMyBids() {
        setActiveButton(btnMyBids);
    }

    public void setActiveSelling() {
        setActiveButton(btnSelling);
    }

    private void setActiveButton(Button activeButton) {
        applySidebarButtonStyle(btnSidebarDashboard, btnSidebarDashboard == activeButton);
        applySidebarButtonStyle(btnMyBids, btnMyBids == activeButton);
        applySidebarButtonStyle(btnSelling, btnSelling == activeButton);
        applySidebarButtonStyle(btnCategories, btnCategories == activeButton);
        applySidebarButtonStyle(btnSidebarWatchlist, btnSidebarWatchlist == activeButton);
        applySidebarButtonStyle(btnSupport, btnSupport == activeButton);
    }

    private void applySidebarButtonStyle(Button button, boolean active) {
        if (button == null) return;

        String textColor = active ? "#e040a0" : "#604868";
        String backgroundColor = active ? "rgba(224, 64, 160, 0.15)" : "transparent";
        button.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");

        if (button.getGraphic() instanceof Label) {
            ((Label) button.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: " + textColor + ";");
        }
    }

    @FXML
    public void handleDashboard(ActionEvent event) {
        autoCollapse();
        setActiveDashboard();
        if (listener != null) {
            listener.onResetFilter();
        } else {
            try {
                if (onBeforeNavigate != null) onBeforeNavigate.run();
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Lỗi chuyển cảnh về MainTemplate: ", e);
            }
        }
    }

    @FXML
    public void handleWatchlist(ActionEvent event) {
        autoCollapse();
        setActiveWatchlist();
        if (listener != null) {
            listener.onFilterWatchlist();
        } else {
            try {
                if (onBeforeNavigate != null) onBeforeNavigate.run();
                MainController.initialShowWatchlist = true;
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Lỗi chuyển cảnh về MainTemplate: ", e);
            }
        }
    }

    @FXML
    public void handleStartSelling(ActionEvent event) {
        autoCollapse();
        try {
            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "UpToSeller.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang UpToSeller: ", e);
        }
    }

    @FXML
    public void handleMyBids(ActionEvent event) {
        autoCollapse();
        setActiveMyBids();
        if (listener != null) {
            listener.onFilterMyBids();
        } else {
            try {
                if (onBeforeNavigate != null) onBeforeNavigate.run();
                MainController.initialHomeFilterMode = "MY_BIDS";
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Lỗi chuyển cảnh về MainTemplate để xem My Bids: ", e);
            }
        }
    }

    @FXML
    public void handleSelling(ActionEvent event) {
        autoCollapse();
        setActiveSelling();
        try {
            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1024, 768);
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang SellerDashboard: ", e);
            showInfo("Selling", "Không thể mở Seller Dashboard. Vui lòng thử lại.");
        }
    }

    @FXML
    public void handleCategories(ActionEvent event) {
        autoCollapse();
        setActiveButton(btnCategories);
        if (listener != null) {
            listener.onShowCategories();
        } else {
            try {
                if (onBeforeNavigate != null) onBeforeNavigate.run();
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Lỗi chuyển cảnh về MainTemplate để chọn danh mục: ", e);
            }
        }
    }

    @FXML
    public void handleSupport(ActionEvent event) {
        autoCollapse();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Support");
        dialog.setHeaderText("Gửi yêu cầu hỗ trợ");
        dialog.setContentText("Mô tả vấn đề bạn cần hỗ trợ:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String content = result.get() == null ? "" : result.get().trim();
        if (content.isBlank()) {
            showInfo("Support", "Bạn chưa nhập nội dung hỗ trợ.");
            return;
        }

        logger.info("Support request from UI: {}", content);
        showInfo("Support", "Yêu cầu hỗ trợ đã được ghi nhận trong bản demo.");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
