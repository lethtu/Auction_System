package com.auction.client.controller;

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
import javafx.scene.layout.VBox;
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
    @FXML private Button btnCategories;
    @FXML private Button btnSidebarWatchlist;
    @FXML private Button btnSupport;
    @FXML private Button btnStartSelling;

    public static boolean isSidebarCollapsed = false;
    private final Map<Button, String> sidebarButtonTextMap = new HashMap<>();

    public interface SidebarListener {
        void onFilterWatchlist(ActionEvent event);
        void onResetFilter(ActionEvent event);
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
        if (btnSidebarWatchlist != null) {
            btnSidebarWatchlist.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: rgba(224, 64, 160, 0.15); -fx-text-fill: #e040a0; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarWatchlist.getGraphic() instanceof Label) {
                ((Label) btnSidebarWatchlist.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #e040a0;");
            }
        }
        if (btnSidebarDashboard != null) {
            btnSidebarDashboard.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarDashboard.getGraphic() instanceof Label) {
                ((Label) btnSidebarDashboard.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnMyBids != null) {
            btnMyBids.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnMyBids.getGraphic() instanceof Label) {
                ((Label) btnMyBids.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSelling != null) {
            btnSelling.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSelling.getGraphic() instanceof Label) {
                ((Label) btnSelling.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSupport != null) {
            btnSupport.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSupport.getGraphic() instanceof Label) {
                ((Label) btnSupport.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
    }

    public void setActiveDashboard() {
        if (btnSidebarDashboard != null) {
            btnSidebarDashboard.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: rgba(224, 64, 160, 0.15); -fx-text-fill: #e040a0; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarDashboard.getGraphic() instanceof Label) {
                ((Label) btnSidebarDashboard.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #e040a0;");
            }
        }
        if (btnSidebarWatchlist != null) {
            btnSidebarWatchlist.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarWatchlist.getGraphic() instanceof Label) {
                ((Label) btnSidebarWatchlist.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnMyBids != null) {
            btnMyBids.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnMyBids.getGraphic() instanceof Label) {
                ((Label) btnMyBids.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSelling != null) {
            btnSelling.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSelling.getGraphic() instanceof Label) {
                ((Label) btnSelling.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSupport != null) {
            btnSupport.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSupport.getGraphic() instanceof Label) {
                ((Label) btnSupport.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
    }

    public void setActiveMyBids() {
        if (btnMyBids != null) {
            btnMyBids.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: rgba(224, 64, 160, 0.15); -fx-text-fill: #e040a0; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnMyBids.getGraphic() instanceof Label) {
                ((Label) btnMyBids.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #e040a0;");
            }
        }
        if (btnSidebarDashboard != null) {
            btnSidebarDashboard.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarDashboard.getGraphic() instanceof Label) {
                ((Label) btnSidebarDashboard.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSidebarWatchlist != null) {
            btnSidebarWatchlist.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarWatchlist.getGraphic() instanceof Label) {
                ((Label) btnSidebarWatchlist.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSelling != null) {
            btnSelling.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSelling.getGraphic() instanceof Label) {
                ((Label) btnSelling.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSupport != null) {
            btnSupport.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSupport.getGraphic() instanceof Label) {
                ((Label) btnSupport.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
    }

    public void setActiveSupport() {
        if (btnSupport != null) {
            btnSupport.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: rgba(224, 64, 160, 0.15); -fx-text-fill: #e040a0; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSupport.getGraphic() instanceof Label) {
                ((Label) btnSupport.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #e040a0;");
            }
        }
        if (btnSidebarDashboard != null) {
            btnSidebarDashboard.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarDashboard.getGraphic() instanceof Label) {
                ((Label) btnSidebarDashboard.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSidebarWatchlist != null) {
            btnSidebarWatchlist.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarWatchlist.getGraphic() instanceof Label) {
                ((Label) btnSidebarWatchlist.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnMyBids != null) {
            btnMyBids.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnMyBids.getGraphic() instanceof Label) {
                ((Label) btnMyBids.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSelling != null) {
            btnSelling.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSelling.getGraphic() instanceof Label) {
                ((Label) btnSelling.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
    }

    public void setActiveSelling() {
        if (btnSelling != null) {
            btnSelling.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: rgba(224, 64, 160, 0.15); -fx-text-fill: #e040a0; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSelling.getGraphic() instanceof Label) {
                ((Label) btnSelling.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #e040a0;");
            }
        }
        if (btnSidebarDashboard != null) {
            btnSidebarDashboard.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarDashboard.getGraphic() instanceof Label) {
                ((Label) btnSidebarDashboard.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSidebarWatchlist != null) {
            btnSidebarWatchlist.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSidebarWatchlist.getGraphic() instanceof Label) {
                ((Label) btnSidebarWatchlist.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnMyBids != null) {
            btnMyBids.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnMyBids.getGraphic() instanceof Label) {
                ((Label) btnMyBids.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
        }
        if (btnSupport != null) {
            btnSupport.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: #604868; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            if (btnSupport.getGraphic() instanceof Label) {
                ((Label) btnSupport.getGraphic()).setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;");
            }
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
        try {
            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "MyBids.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang MyBids: ", e);
        }
    }

    @FXML
    public void handleSelling(ActionEvent event) {
        autoCollapse();
        try {
            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "SellerDashboard.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang SellerDashboard: ", e);
        }
    }

    @FXML
    public void handleCategories(ActionEvent event) {
        autoCollapse();
        handleDashboard(event);
    }

    @FXML
    public void handleSupport(ActionEvent event) {
        autoCollapse();
        try {
            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "Support.fxml", 1280, 800);
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang Support: ", e);
        }
    }
}
