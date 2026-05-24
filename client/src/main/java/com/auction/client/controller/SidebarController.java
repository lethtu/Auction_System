package com.auction.client.controller;

import com.auction.client.util.AlertUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private static final int APP_WIDTH = 1200;
    private static final int APP_HEIGHT = 800;

    @FXML
    private ScrollPane sidebarContainer;
    @FXML
    private VBox sidebarContent;
    @FXML
    private Button btnSidebarDashboard;
    @FXML
    private Button btnMyBids;
    @FXML
    private Button btnSelling;
    @FXML
    private Button btnSidebarWatchlist;
    @FXML
    private Button btnSupport;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnStartSelling;
    private Button currentActiveButton;

    public static boolean isSidebarCollapsed = false;
    private static boolean preferenceLoaded = false;
    private final Map<Button, String> sidebarButtonTextMap = new HashMap<>();

    public interface SidebarListener {
        default void onFilterWatchlist() {
        }

        default void onFilterWatchlist(ActionEvent event) {
            onFilterWatchlist();
        }

        default void onFilterMyBids() {
        }

        default void onFilterMyBids(ActionEvent event) {
            onFilterMyBids();
        }

        default void onFilterMySessions() {
        }

        default void onFilterMySessions(ActionEvent event) {
            onFilterMySessions();
        }

        default void onResetFilter() {
        }

        default void onResetFilter(ActionEvent event) {
            onResetFilter();
        }

        default void onShowCategories() {
        }

        default void onShowCategories(ActionEvent event) {
            onShowCategories();
        }
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
        isSidebarCollapsed = false;
        // Save default text
        for (javafx.scene.Node node : sidebarContent.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                sidebarButtonTextMap.put(btn, btn.getText());
            }
        }

        // Check if already a seller
        if (User.getRole() != null && User.getRole().equalsIgnoreCase("seller")) {
            if (btnStartSelling != null) {
                btnStartSelling.setVisible(false);
                btnStartSelling.setManaged(false);
                btnStartSelling.setDisable(true);
            }
            if (btnSelling != null) {
                btnSelling.setVisible(true);
                btnSelling.setManaged(true);
            }
        } else {
            if (btnStartSelling != null) {
                btnStartSelling.setVisible(true);
                btnStartSelling.setManaged(true);
                btnStartSelling.setDisable(false);
                btnStartSelling.setOnAction(this::handleStartSelling);
            }
            if (btnSelling != null) {
                btnSelling.setVisible(false);
                btnSelling.setManaged(false);
            }
        }

        if (!preferenceLoaded) {
            boolean persistedCollapse = java.util.prefs.Preferences
                    .userNodeForPackage(com.auction.client.util.SettingsDialog.class)
                    .getBoolean(com.auction.client.util.SettingsDialog.KEY_AUTO_COLLAPSE, false);
            isSidebarCollapsed = persistedCollapse;
            preferenceLoaded = true;
        }

        // Apply current static state by temporarily inverting it and calling toggleSidebar()
        boolean currentCollapsedState = isSidebarCollapsed;
        isSidebarCollapsed = !currentCollapsedState;
        toggleSidebar();

        Platform.runLater(() -> {
            if (sidebarContainer != null && sidebarContainer.getScene() != null) {
                sidebarContainer.getScene().widthProperty().addListener((obs, oldV, newV) -> {
                    if (newV.doubleValue() < 1100 && !isSidebarCollapsed) {
                        toggleSidebar();
                    }
                });
            }
        });
    }
    private void autoCollapse() {
        // Keep the full bordered sidebar visible after navigation.
        // Users can still collapse it manually with the hamburger button.
        ensureExpandedSidebarChrome();
    }

    private void ensureExpandedSidebarChrome() {
        if (sidebarContainer != null && !isSidebarCollapsed) {
            sidebarContainer.setMinWidth(200);
            sidebarContainer.setPrefWidth(200);
            sidebarContainer.setMaxWidth(200);
            double expandedHeight = getExpandedSidebarHeight();
            sidebarContainer.setMinHeight(expandedHeight);
            sidebarContainer.setPrefHeight(Double.MAX_VALUE);
            sidebarContainer.setMaxHeight(Double.MAX_VALUE);
        }
        if (sidebarContent != null && !isSidebarCollapsed) {
            sidebarContent.setPadding(new Insets(0, 0, 0, 0));
            sidebarContent.setAlignment(Pos.TOP_LEFT);
            sidebarContent.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-effect: null;");
        }
    }

    private double getExpandedSidebarHeight() {
        boolean showStartSelling = btnStartSelling != null && btnStartSelling.isVisible() && btnStartSelling.isManaged();
        return showStartSelling ? 388.0 : 336.0;
    }
    public void forceCollapse() {
        autoCollapse();
    }

    public void toggleSidebar() {
        isSidebarCollapsed = !isSidebarCollapsed;

        if (isSidebarCollapsed) {
            // Collapse
            sidebarContainer.getStyleClass().remove("app-sidebar-scroll");
            if (!sidebarContainer.getStyleClass().contains("app-sidebar-scroll-collapsed")) {
                sidebarContainer.getStyleClass().add("app-sidebar-scroll-collapsed");
            }
            sidebarContent.getStyleClass().remove("app-sidebar-content");
            if (!sidebarContent.getStyleClass().contains("app-sidebar-content-collapsed")) {
                sidebarContent.getStyleClass().add("app-sidebar-content-collapsed");
            }

            sidebarContainer.setMinWidth(70);
            sidebarContainer.setPrefWidth(70);
            sidebarContainer.setMaxWidth(70);
            double collapsedHeight = getExpandedSidebarHeight();
            sidebarContainer.setMinHeight(collapsedHeight);
            sidebarContainer.setPrefHeight(Double.MAX_VALUE);
            sidebarContainer.setMaxHeight(Double.MAX_VALUE);
            sidebarContent.setPadding(new Insets(0, 0, 0, 0));
            sidebarContent.setAlignment(Pos.TOP_CENTER);

            for (javafx.scene.Node node : sidebarContent.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    String tooltipText = sidebarButtonTextMap.get(btn);
                    if (tooltipText != null && !tooltipText.isEmpty()) {
                        Tooltip tooltip = new Tooltip(tooltipText);
                        tooltip.setStyle(
                                "-fx-background-color: -fx-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 6px 12px; -fx-font-size: 13px;");

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
                        btn.setStyle(
                                "-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: -fx-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 22px; -fx-padding: 0px; -fx-cursor: hand; -fx-effect: null;");
                    }
                } else if (node instanceof Label) {
                    node.setVisible(false);
                    node.setManaged(false);
                }
            }
            setActiveButton(currentActiveButton);
        } else {
            applyExpandedSidebar();
        }
    }

    private void applyExpandedSidebar() {
        sidebarContainer.getStyleClass().remove("app-sidebar-scroll-collapsed");
        if (!sidebarContainer.getStyleClass().contains("app-sidebar-scroll")) {
            sidebarContainer.getStyleClass().add("app-sidebar-scroll");
        }
        sidebarContent.getStyleClass().remove("app-sidebar-content-collapsed");
        if (!sidebarContent.getStyleClass().contains("app-sidebar-content")) {
            sidebarContent.getStyleClass().add("app-sidebar-content");
        }

        sidebarContainer.setMinWidth(200);
            sidebarContainer.setPrefWidth(200);
            sidebarContainer.setMaxWidth(200);
            double expandedHeight = getExpandedSidebarHeight();
            sidebarContainer.setMinHeight(expandedHeight);
            sidebarContainer.setPrefHeight(Double.MAX_VALUE);
            sidebarContainer.setMaxHeight(Double.MAX_VALUE);
        sidebarContent.setPadding(new Insets(0, 0, 0, 0));
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
                btn.setPrefHeight(btn == btnStartSelling ? 44 : 40);
                btn.setMinHeight(btn == btnStartSelling ? 44 : 40);
                btn.setMaxHeight(btn == btnStartSelling ? 44 : 40);
                btn.setAlignment(Pos.CENTER_LEFT);
                if (btn == btnStartSelling) {
                    btn.setStyle(
                            "-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: -fx-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 0 12px; -fx-cursor: hand; -fx-effect: null;");
                }
            } else if (node instanceof Label) {
                node.setVisible(true);
                node.setManaged(true);
            }
        }
        setActiveButton(currentActiveButton);
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
        if (button == null)
            return;

        String textColor = active ? "-fx-accent" : "-app-text-muted";
        String backgroundColor = active ? "-app-accent-opacity-16" : "transparent";
        String padding = isSidebarCollapsed ? "0px" : "7px 16px";
        String radius = isSidebarCollapsed ? "22px" : "20px";

        button.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: "
                + backgroundColor
                + "; -fx-text-fill: "
                + textColor
                + "; -fx-font-weight: bold; -fx-background-radius: " + radius + "; -fx-padding: " + padding
                + "; -fx-cursor: hand; -fx-effect: null;");
        if (!isSidebarCollapsed) {
            button.setMinHeight(40);
            button.setPrefHeight(40);
            button.setMaxHeight(40);
        }

        button.setEffect(null);
        button.getStyleClass().remove("glow");
        button.getStyleClass().remove("accent-glow");
        if (button.getGraphic() instanceof Label) {
            ((Label) button.getGraphic()).setStyle(
                    "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: "
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
                if (onBeforeNavigate != null)
                    onBeforeNavigate.run();
                MainController.initialShowWatchlist = false;
                MainController.initialHomeFilterMode = "ALL";
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", APP_WIDTH, APP_HEIGHT);
            } catch (IOException e) {
                logger.error("Error switching to MainTemplate: ", e);
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
                if (onBeforeNavigate != null)
                    onBeforeNavigate.run();
                MainController.initialShowWatchlist = true;
                MainController.initialHomeFilterMode = "WATCHLIST";
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", APP_WIDTH, APP_HEIGHT);
            } catch (IOException e) {
                logger.error("Error switching to MainTemplate: ", e);
            }
        }
    }

    @FXML
    public void handleStartSelling(ActionEvent event) {
        autoCollapse();
        try {
            if (onBeforeNavigate != null)
                onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "UpToSeller.fxml", APP_WIDTH, APP_HEIGHT);
        } catch (IOException e) {
            logger.error("Error switching to UpToSeller: ", e);
        }
    }

    @FXML
    public void handleMyBids(ActionEvent event) {
        autoCollapse();
        setActiveMyBids();
        try {
            Stage stage = resolveStage(event);
            int currentWidth = stage == null ? APP_WIDTH : Math.max(APP_WIDTH, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? APP_HEIGHT : Math.max(APP_HEIGHT, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null)
                onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "MyBids.fxml", currentWidth, currentHeight);
        } catch (IOException e) {
            logger.error("Error switching to MyBids.fxml: ", e);
            showInfo("My Bids", "Cannot open My Bids screen. Please try again.");
        }
    }

    @FXML
    public void handleSelling(ActionEvent event) {
        autoCollapse();
        setActiveSelling();
        try {
            Stage stage = resolveStage(event);
            int currentWidth = stage == null ? APP_WIDTH : Math.max(APP_WIDTH, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? APP_HEIGHT : Math.max(APP_HEIGHT, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null)
                onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", currentWidth, currentHeight);
        } catch (IOException e) {
            logger.error("Error switching to SellerDashboard: ", e);
            showInfo("Selling", "Cannot open Seller Dashboard. Please try again.");
        }
    }

    @FXML
    public void handleSupport(ActionEvent event) {
        autoCollapse();
        setActiveSupport();
        try {
            Stage stage = resolveStage(event);
            int currentWidth = stage == null ? APP_WIDTH : Math.max(APP_WIDTH, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? APP_HEIGHT : Math.max(APP_HEIGHT, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null)
                onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "Support.fxml", currentWidth, currentHeight);
        } catch (IOException e) {
            logger.error("Error switching to Support.fxml: ", e);
            showInfo("Support", "Cannot open support screen. Please try again.");
        }
    }

    @FXML
    public void handleSettings(ActionEvent event) {
        autoCollapse();
        setActiveSettings();
        try {
            Stage stage = resolveStage(event);
            int currentWidth = stage == null ? APP_WIDTH : Math.max(APP_WIDTH, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? APP_HEIGHT : Math.max(APP_HEIGHT, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null)
                onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "Settings.fxml", currentWidth, currentHeight);
        } catch (IOException e) {
            logger.error("Error switching to Settings.fxml: ", e);
            showInfo("Settings", "Cannot open settings screen. Please try again.");
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
        AlertUtil.showInfo(title, message);
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
