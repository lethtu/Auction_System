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
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.client.model.User;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SidebarController {
    private static final Logger logger = LoggerFactory.getLogger(SidebarController.class);

    @FXML private ScrollPane sidebarContainer;
    @FXML private VBox sidebarContent;
    @FXML private Button btnSidebarDashboard;
    @FXML private Button btnMyBids;
    @FXML private Button btnSelling;
    @FXML private Button btnSidebarWatchlist;
    @FXML private Button btnSupport;
    @FXML private Button btnSettings;
    @FXML private Button btnStartSelling;
    private Button currentActiveButton;

    public static boolean isSidebarCollapsed = false;
    private final Map<Button, String> sidebarButtonTextMap = new HashMap<>();

    public interface SidebarListener {
        default void onFilterWatchlist() {}
        default void onFilterWatchlist(ActionEvent event) { onFilterWatchlist(); }

        default void onFilterMyBids() {}
        default void onFilterMyBids(ActionEvent event) { onFilterMyBids(); }

        default void onFilterMySessions() {}
        default void onFilterMySessions(ActionEvent event) { onFilterMySessions(); }

        default void onResetFilter() {}
        default void onResetFilter(ActionEvent event) { onResetFilter(); }

        default void onShowCategories() {}
        default void onShowCategories(ActionEvent event) { onShowCategories(); }
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

        // Kiểm tra xem đã là seller chưa
        if (User.getRole() != null && User.getRole().equalsIgnoreCase("seller")) {
            if (btnStartSelling != null) {
                btnStartSelling.setVisible(false);
                btnStartSelling.setManaged(false);
            }
            if (btnSelling != null) {
                btnSelling.setVisible(true);
                btnSelling.setManaged(true);
            }
        } else {
            if (btnStartSelling != null) {
                btnStartSelling.setVisible(true);
                btnStartSelling.setManaged(true);
            }
            if (btnSelling != null) {
                btnSelling.setVisible(false);
                btnSelling.setManaged(false);
            }
        }

        boolean persistedCollapse = java.util.prefs.Preferences.userNodeForPackage(com.auction.client.util.SettingsDialog.class)
                .getBoolean(com.auction.client.util.SettingsDialog.KEY_AUTO_COLLAPSE, false);
        if (persistedCollapse) {
            isSidebarCollapsed = false;
            toggleSidebar();
        } else if (isSidebarCollapsed) {
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
                    btn.setPrefWidth(44);
                    btn.setMinWidth(44);
                    btn.setMaxWidth(44);
                    btn.setPrefHeight(44);
                    btn.setMinHeight(44);
                    btn.setMaxHeight(44);
                    btn.setAlignment(Pos.CENTER);
                    if (btn.getGraphic() != null) {
                        btn.getGraphic().setTranslateX(0);
                    }
                    if (btn == btnStartSelling) {
                        btn.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: #e040a0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 22px; -fx-padding: 0px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.3), 16, 0, 0, 4);");
                    }
                } else if (node instanceof Label) {
                    node.setVisible(false);
                    node.setManaged(false);
                }
            }
            setActiveButton(currentActiveButton);
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
                    btn.setMaxWidth(165);
                    btn.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                    btn.setMinHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                    btn.setMaxHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                    btn.setAlignment(Pos.CENTER_LEFT);
                    if (btn == btnStartSelling) {
                        btn.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: #e040a0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 12px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.3), 16, 0, 0, 4);");
                    }
                } else if (node instanceof Label) {
                    node.setVisible(true);
                    node.setManaged(true);
                }
            }
            setActiveButton(currentActiveButton);
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

    public void setActiveSupport() {
        setActiveButton(btnSupport);
    }

    public void setActiveSettings() {
        setActiveButton(btnSettings);
    }

    private void setActiveButton(Button activeButton) {
        this.currentActiveButton = activeButton;
        applySidebarButtonStyle(btnSidebarDashboard, btnSidebarDashboard == activeButton);
        applySidebarButtonStyle(btnMyBids, btnMyBids == activeButton);
        applySidebarButtonStyle(btnSelling, btnSelling == activeButton);
        applySidebarButtonStyle(btnSidebarWatchlist, btnSidebarWatchlist == activeButton);
        applySidebarButtonStyle(btnSupport, btnSupport == activeButton);
        applySidebarButtonStyle(btnSettings, btnSettings == activeButton);
    }

    private void applySidebarButtonStyle(Button button, boolean active) {
        if (button == null) return;

        String textColor = active ? "#e040a0" : "#604868";
        String backgroundColor = active ? "rgba(224, 64, 160, 0.15)" : "transparent";
        String padding = isSidebarCollapsed ? "0px" : "7px 16px";
        String radius = isSidebarCollapsed ? "22px" : "20px";

        button.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: "
                + backgroundColor
                + "; -fx-text-fill: "
                + textColor
                + "; -fx-font-weight: bold; -fx-background-radius: " + radius + "; -fx-padding: " + padding + "; -fx-cursor: hand;");

        if (button.getGraphic() instanceof Label) {
            ((Label) button.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: "
                    + textColor
                    + ";");
        }
    }

    @FXML
    public void handleDashboard(ActionEvent event) {
        autoCollapse();
        setActiveDashboard();
        if (listener != null) {
            listener.onResetFilter(event);
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
            listener.onFilterWatchlist(event);
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
        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            int currentWidth = stage == null ? 1280 : Math.max(1280, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? 800 : Math.max(800, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "MyBids.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang MyBids.fxml: ", e);
            showInfo("My Bids", "Không thể mở màn My Bids. Vui lòng thử lại.");
        }
    }

    @FXML
    public void handleSelling(ActionEvent event) {
        autoCollapse();
        setActiveSelling();
        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            int currentWidth = stage == null ? 1280 : Math.max(1280, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? 800 : Math.max(800, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang SellerDashboard: ", e);
            showInfo("Selling", "Không thể mở Seller Dashboard. Vui lòng thử lại.");
        }
    }

        @FXML
    public void handleSupport(ActionEvent event) {
        autoCollapse();
        setActiveSupport();
        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            int currentWidth = stage == null ? 1280 : Math.max(1280, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? 800 : Math.max(800, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "Support.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang Support.fxml: ", e);
            showInfo("Support", "Không thể mở màn hỗ trợ. Vui lòng thử lại.");
        }
    }

    @FXML
    public void handleSettings(ActionEvent event) {
        autoCollapse();
        setActiveSettings();
        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            int currentWidth = stage == null ? 1280 : Math.max(1280, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? 800 : Math.max(800, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "Settings.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang Settings.fxml: ", e);
            showInfo("Settings", "Không thể mở màn cài đặt. Vui lòng thử lại.");
        }
    }


    private Stage resolveStage(ActionEvent event) {
        if (event == null || !(event.getSource() instanceof Node)) {
            return null;
        }
        Node source = (Node) event.getSource();
        if (source.getScene() == null || source.getScene().getWindow() == null) {
            return null;
        }
        return (Stage) source.getScene().getWindow();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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