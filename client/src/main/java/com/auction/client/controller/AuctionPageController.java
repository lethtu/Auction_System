package com.auction.client.controller;

import com.auction.client.Config;
import com.auction.client.util.BalanceDisplayBinder;
import com.auction.client.util.ImageUrlUtil;
import com.auction.client.util.MoneyFormatUtil;
import com.auction.client.util.BidStepPolicy;
import com.auction.client.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.util.NotificationBellBinder;
import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import com.auction.client.model.notification.NotificationSeverity;
import com.auction.client.service.NotificationCenterService;
import com.auction.client.service.SettingsService;
import com.auction.client.util.CacheManager;
import com.auction.client.util.GltfImporterJFX;
import javafx.scene.Node;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;

public class AuctionPageController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionPageController.class);

    @FXML
    private MenuButton userMenuButton;
    @FXML
    private StackPane topBarAvatarPane;
    @FXML
    private TopbarController topbarController;

    private static final String AUTOBID_PREFIX = "AUTOBID:";

    private static final String BASE_ALERT_STYLE = "-fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: bold; "
            +
            "-fx-padding: 10px 16px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-width: 1px; " +
            "-fx-alignment: center; -fx-text-alignment: center;";

    private static final String ERROR_STYLE = BASE_ALERT_STYLE +
            "-fx-background-color: #fce8e6; -fx-border-color: #fad2cf; -fx-text-fill: #c5221f;";

    private static final String SUCCESS_STYLE = BASE_ALERT_STYLE +
            "-fx-background-color: #e6f9ed; -fx-border-color: #b3ecc4; -fx-text-fill: #137333;";

    private static final String INFO_STYLE = BASE_ALERT_STYLE +
            "-fx-background-color: #e8f0fe; -fx-border-color: #d2e3fc; -fx-text-fill: #1a73e8;";

    private static final String WARNING_STYLE = BASE_ALERT_STYLE +
            "-fx-background-color: #fef7e0; -fx-border-color: #feebc8; -fx-text-fill: #b06000;";

    private static final String EXTENSION_STYLE = BASE_ALERT_STYLE +
            "-fx-background-color: #fdf2e9; -fx-border-color: #fcd7b6; -fx-text-fill: #b25900;";

    private static final String RESERVE_BADGE_BASE_STYLE =
            "-fx-font-family: 'DM Sans'; -fx-font-size: 11px; -fx-font-weight: 900; "
                    + "-fx-padding: 4 10; -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-width: 1;";

    private static final String RESERVE_MET_STYLE = RESERVE_BADGE_BASE_STYLE
            + "-fx-background-color: rgba(19, 115, 51, 0.14); "
            + "-fx-border-color: rgba(19, 115, 51, 0.32); "
            + "-fx-text-fill: #38a169;";

    private static final String UNDER_RESERVE_STYLE = RESERVE_BADGE_BASE_STYLE
            + "-fx-background-color: rgba(229, 62, 62, 0.14); "
            + "-fx-border-color: rgba(229, 62, 62, 0.34); "
            + "-fx-text-fill: #e53e3e;";

    @FXML
    private Label productNameLabel;
    @FXML
    private Label currentPriceLabel;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Button placeBidBtn;
    @FXML
    private Button btnAutoBid;
    @FXML
    private HBox quickBidBox;
    @FXML
    private Button btnQuickBidOne;
    @FXML
    private Button btnQuickBidTwo;
    @FXML
    private Button btnQuickBidFive;
    @FXML
    private Label messageLabel;
    @FXML
    private Label remainingTimeLabel;
    @FXML
    private Label startPriceLabel;
    @FXML
    private ImageView productImageView;
    @FXML
    private StackPane productMediaFrame;
    @FXML
    private Button btnToggle3D;
    @FXML
    private StackPane model3DContainer;
    private boolean is3DMode = false;
    private String itemUuid = null;
    private String productImagePath = null;
    @FXML
    private SidebarController sidebarController;
    @FXML
    private VBox sideBar;

    @FXML
    private Button btnNotificationBell;
    @FXML
    private Label notificationBadge;
    @FXML
    private Button btnSettings;

    @FXML
    private Label mainMenuLabel;
    @FXML
    private Label dashboardText;
    @FXML
    private Label liveAuctionsText;
    @FXML
    private Label myBidsText;
    @FXML
    private Label sellingText;
    @FXML
    private Label discoverLabel;
    @FXML
    private Label categoriesText;
    @FXML
    private Label activeBidsText;
    @FXML
    private Label watchlistText;
    @FXML
    private Label endedSoonText;
    @FXML
    private Label otherLabel;
    @FXML
    private Label supportText;
    @FXML
    private Label startSellingText;

    @FXML
    private Label endingInTitleLabel;
    @FXML
    private Label startPriceTitleLabel;
    @FXML
    private Label highestBidTitleLabel;
    @FXML
    private Label minBidIncrementLabel;
    @FXML
    private Label highestBidderLabel;
    @FXML
    private Label reserveStatusLabel;
    @FXML
    private Label totalBidsLabel;
    @FXML
    private Label watchingLabel;
    @FXML
    private Label itemDescriptionLabel;
    @FXML
    private Label minIncrementLabel;
    @FXML
    private Label bidAmountTitleLabel;
    @FXML
    private Label availableBalanceValue;
    @FXML
    private Button availableBalanceToggle;
    @FXML
    private VBox chartContainer;
    @FXML
    private VBox bidHistoryContainer;
    @FXML
    private javafx.scene.layout.GridPane mainContentGrid;

    private static final String MAIN_TEMPLATE_FXML = "MainTemplate.fxml";
    private static final String JOIN_PREFIX = "JOIN:";
    private static final String BID_PREFIX = "BID:";
    private static final String NOTICE_PREFIX = "NOTICE:";
    private static final String RESPONSE_PREFIX = "RESPONSE:";
    private static final String ROOM_COUNT_PREFIX = "ROOM_COUNT:";

    private static final String MONEY_SYMBOL = "\u20ab";
    private static final String MONEY_PREFIX = MONEY_SYMBOL + " ";
    private static final String DEFAULT_PRODUCT_NAME = "Unknown Product";
    private static final String DEFAULT_DESCRIPTION = "No product description available.";
    private static final String DEFAULT_HIGHEST_BIDDER = "No bidder yet";
    private static final String PROCESSING_MESSAGE = "Processing request...";
    private static final String OWN_AUCTION_BID_MESSAGE = "You cannot bid on your own auction.";

    private static final int EXPANDED_SIDEBAR_WIDTH = 200;
    private static final int COLLAPSED_SIDEBAR_WIDTH = 70;
    private static final int BID_TIMEOUT_SECONDS = 15;

    private final java.util.List<com.auction.client.model.BidChartPoint> allBidPoints = new java.util.ArrayList<>();
    private final java.util.Set<Integer> seenBidIds = new java.util.HashSet<>();
    private final java.util.Set<String> seenCompositeKeys = new java.util.HashSet<>();
    private static final int MINI_CHART_POINTS = 4;
    private static final int MAX_CHART_POINTS = 50;
    private static final double[] ACTIVITY_OPACITY = { 1.0, 0.7, 0.5, 0.35, 0.25 };
    private javafx.stage.Stage fullHistoryPopup = null;

    private java.net.http.WebSocket webSocket;

    private int currentSessionId;
    private boolean endingSoonNotified = false;
    private BigDecimal currentPrice = BigDecimal.ZERO;
    private BigDecimal stepPrice = BigDecimal.ZERO;
    private BigDecimal startingPrice = BigDecimal.ZERO;
    private BigDecimal reservePrice = BigDecimal.ZERO;
    private Integer highestBidderId;
    private Integer sellerId;
    private boolean auctionOpenForBidding = true;
    private int bidCount;
    private int watchingCount;
    private BigDecimal myLastBidAmount = null;
    private boolean bidRequestInFlight = false;
    private Integer pendingBidAuctionId = null;
    private Integer pendingBidderId = null;
    private BigDecimal pendingBidAmount = null;

    private Timeline timeline;
    private Timeline bidTimeout;
    private boolean bidErrorSoundPlayedForCurrentAttempt = false;

    @FXML
    public void initialize() {
        createUserOption("Avatar");
        initDefaultView();
        setupQuickBidControls();
        BalanceDisplayBinder.bindAvailableBalance(availableBalanceValue, availableBalanceToggle);
        fetchLatestUserBalance();

        // The media frame owns the size. Switching from the 2D ImageView to the 3D
        // container must never allow the product card to shrink or change shape.
        if (productImageView != null && productMediaFrame != null) {
            productImageView.setPreserveRatio(true);
            productImageView.fitWidthProperty().bind(productMediaFrame.widthProperty().subtract(16));
        }

        if (model3DContainer != null && productMediaFrame != null) {
            model3DContainer.setManaged(false);
            model3DContainer.prefWidthProperty().bind(productMediaFrame.widthProperty());
            model3DContainer.prefHeightProperty().bind(productMediaFrame.heightProperty());
            model3DContainer.minWidthProperty().bind(productMediaFrame.widthProperty());
            model3DContainer.minHeightProperty().bind(productMediaFrame.heightProperty());
            model3DContainer.maxWidthProperty().bind(productMediaFrame.widthProperty());
            model3DContainer.maxHeightProperty().bind(productMediaFrame.heightProperty());

            // Keep 3D content clipped to exactly the same rounded rectangular media frame as 2D.
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.setArcWidth(64.0);
            clip.setArcHeight(64.0);
            clip.widthProperty().bind(model3DContainer.widthProperty());
            clip.heightProperty().bind(model3DContainer.heightProperty());
            model3DContainer.setClip(clip);

            model3DContainer.getChildren().addListener((javafx.collections.ListChangeListener.Change<? extends Node> change) -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        for (Node node : change.getAddedSubList()) {
                            if (node instanceof javafx.scene.SubScene) {
                                javafx.scene.SubScene subScene = (javafx.scene.SubScene) node;
                                subScene.widthProperty().bind(model3DContainer.widthProperty());
                                subScene.heightProperty().bind(model3DContainer.heightProperty());
                            }
                        }
                    }
                }
            });
        }

        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }

        if (btnSettings != null) {
            btnSettings.setOnAction(e -> {
                try {
                    disconnectSocket();
                    com.auction.client.controller.SceneSwitcher.switchScene(e, "Settings.fxml", 1280, 800);
                } catch (IOException ex) {
                    logger.error("Error switching to Settings.fxml: ", ex);
                }
            });
        }

        Platform.runLater(() -> updateTopBarAvatar(User.getAvatarUrl()));

        if (topbarController != null) {
            topbarController.setSearchVisible(false);
            if (sidebarController != null) {
                topbarController.setSidebarController(sidebarController);
            }
        }

        if (sidebarController != null) {
            sidebarController.setOnBeforeNavigate(this::disconnectSocket);
            sidebarController.forceCollapse();
        }

        // Auto-disconnect socket when view is unloaded/removed from scene
        if (productNameLabel != null) {
            productNameLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    disconnectSocket();
                }
            });
        }

        setupResponsiveFontListeners();

        // Setup messageLabel to behave as a rich modern alert banner
        if (messageLabel != null) {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
            messageLabel.textProperty().addListener((obs, oldVal, newVal) -> {
                boolean hasText = newVal != null && !newVal.trim().isEmpty();
                messageLabel.setVisible(hasText);
                messageLabel.setManaged(hasText);

                if (hasText) {
                    String style = messageLabel.getStyle();
                    String iconCode = "";
                    String iconColor = "";
                    if (style != null) {
                        if (style.contains("#137333")) {
                            iconCode = "\uE86C"; // check_circle
                            iconColor = "#137333";
                        } else if (style.contains("#c5221f")) {
                            iconCode = "\uE000"; // error
                            iconColor = "#c5221f";
                        } else if (style.contains("#1a73e8")) {
                            iconCode = "\uE88E"; // info
                            iconColor = "#1a73e8";
                        } else if (style.contains("#b06000") || style.contains("#b25900")) {
                            iconCode = "\uE002"; // warning
                            iconColor = style.contains("#b25900") ? "#b25900" : "#b06000";
                        }
                    }
                    if (!iconCode.isEmpty()) {
                        Label iconLabel = new Label(iconCode);
                        iconLabel.setStyle(
                                "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 16px; -fx-text-fill: "
                                        + iconColor + "; -fx-padding: 0 6 0 0;");
                        messageLabel.setGraphic(iconLabel);
                    } else {
                        messageLabel.setGraphic(null);
                    }
                } else {
                    messageLabel.setGraphic(null);
                }
            });
        }
    }

    public void setItem(JSONObject sessionObj, JSONObject itemObj) {
        endingSoonNotified = false;
        if (sessionObj == null)
            return;
        this.currentSessionId = sessionObj.optInt("id", 0);
        applySessionData(sessionObj, itemObj);
        initBidTrajectoryCard();
        connectToServer();
        loadBidHistoryFromServer();
        refreshSessionFromServer();
    }

    private void createUserOption(String text) {
        MenuItem accountItem = new MenuItem("My Account");
        MenuItem depositMoney = new MenuItem("Deposit");
        MenuItem logoutItem = new MenuItem("Logout");

        accountItem.setOnAction(event -> {
            try {
                MainController.initialShowAccount = true;
                SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Error switching to account page: ", e);
            }
        });

        depositMoney.setOnAction(event -> {
            try {
                SceneSwitcher.switchScene(event, "Deposit.fxml", 1280, 800);
            } catch (IOException e) {
                logger.error("Error switching to deposit page: ", e);
            }
        });

        logoutItem.setOnAction(event -> {
            try {
                handleLogout(event);
            } catch (IOException e) {
                logger.error("Error switching to Login screen!", e);
            }
        });

        if (userMenuButton != null) {
            userMenuButton.getItems().addAll(accountItem, depositMoney, new SeparatorMenuItem(), logoutItem);
        }
    }

    public void handleLogout(ActionEvent event) throws IOException {
        User.clearSession();
        SceneSwitcher.switchScene(event, "Login.fxml", 1100, 700);
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        if (!isUserLoggedIn()) {
            showError("Please log in to bid!");
            return;
        }
        if (isSellerOfCurrentAuction()) {
            showError(OWN_AUCTION_BID_MESSAGE);
            return;
        }

        BigDecimal bidAmount = getValidBidAmount();
        if (bidAmount == null) {
            return;
        }

        if (hasPendingLocalBidRequest()) {
            showInfo("Your previous bid is still being processed. Please wait a moment.");
            return;
        }

        if (!isSocketReady()) {
            reconnectBidSocket();
            showError("Bid server is reconnecting. Please try again in a moment.");
            return;
        }

        if (!confirmBidIfNeeded(bidAmount)) {
            return;
        }

        logger.info("Place Bid clicked: auctionId={}, bidderId={}, amount={}, socketReady={}", currentSessionId,
                User.getId(), bidAmount, isSocketReady());

        myLastBidAmount = bidAmount;
        markLocalBidRequestInFlight(bidAmount);
        if (sendBidRequest(bidAmount)) {
            showBidProcessing();
        } else {
            clearLocalBidRequest("send failed");
        }
    }

    private void setupQuickBidControls() {
        if (quickBidBox != null) {
            quickBidBox.setVisible(true);
            quickBidBox.setManaged(true);
        }
        installQuickBidTooltip(btnQuickBidOne, 1);
        installQuickBidTooltip(btnQuickBidTwo, 2);
        installQuickBidTooltip(btnQuickBidFive, 5);
    }

    private void installQuickBidTooltip(Button button, int multiplier) {
        if (button != null) {
            button.setTooltip(new Tooltip("Set bid to current price plus " + multiplier + " increment"
                    + (multiplier == 1 ? "" : "s")));
        }
    }

    @FXML
    private void handleQuickBidOne(ActionEvent event) {
        applyQuickBid(1);
    }

    @FXML
    private void handleQuickBidTwo(ActionEvent event) {
        applyQuickBid(2);
    }

    @FXML
    private void handleQuickBidFive(ActionEvent event) {
        applyQuickBid(5);
    }

    private void applyQuickBid(int multiplier) {
        BigDecimal increment = getEffectiveStepPrice();
        BigDecimal amount = currentPrice.add(increment.multiply(BigDecimal.valueOf(multiplier)));
        bidAmountField.setText(formatPrice(amount));
    }

    private boolean confirmBidIfNeeded(BigDecimal bidAmount) {
        SettingsService settings = SettingsService.getInstance();
        long warningThreshold = settings.getHighBidWarningThreshold();
        boolean highBid = warningThreshold > 0
                && bidAmount.compareTo(BigDecimal.valueOf(warningThreshold)) >= 0;

        if (!settings.isConfirmBeforeBid() && !highBid) {
            return true;
        }

        String message = "Place a bid of " + formatVnd(bidAmount) + " on this auction?";
        if (highBid) {
            message = "This bid is at or above your high bid warning threshold of "
                    + formatVnd(BigDecimal.valueOf(warningThreshold)) + ".\n\n" + message;
        }

        return com.auction.client.util.AlertUtil.showBidConfirmation(
                highBid ? "High Bid Warning" : "Confirm Bid",
                message,
                highBid);
    }

    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        if (sidebarController != null) {
            sidebarController.toggleSidebar();
        } else if (sideBar != null) {
            boolean shouldExpand = sideBar.getPrefWidth() <= COLLAPSED_SIDEBAR_WIDTH;
            setSidebarExpanded(shouldExpand);
        }
    }

    @FXML
    public void handleAutoBidConfig(ActionEvent event) {
        if (!isUserLoggedIn()) {
            showError("Please log in to configure Auto-bid!");
            return;
        }
        if (isSellerOfCurrentAuction()) {
            showError(OWN_AUCTION_BID_MESSAGE);
            return;
        }

        if (!isSocketReady()) {
            showError("Not connected to Socket server!");
            return;
        }

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        VBox root = new VBox();
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        root.setPrefWidth(460);

        VBox mainCard = new VBox(15);
        mainCard.setStyle(
                "-fx-padding: 24;"
                        + "-fx-background-color: -app-card;"
                        + "-fx-background-radius: 18;"
                        + "-fx-border-color: -app-border;"
                        + "-fx-border-radius: 18;"
                        + "-fx-border-width: 1.5;");
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.20));
        shadow.setRadius(24);
        shadow.setOffsetY(10);
        mainCard.setEffect(shadow);

        javafx.scene.layout.HBox titleBar = new javafx.scene.layout.HBox(8);
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleBar.setMinHeight(34);
        titleBar.setPrefHeight(34);
        titleBar.setStyle("-fx-padding: 0 0 10 0; -fx-cursor: move;");

        Label titleLbl = new Label("Auto-bidding Configuration");
        titleLbl.setStyle(
                "-fx-font-family: 'DM Sans';"
                        + "-fx-font-size: 16px;"
                        + "-fx-font-weight: 900;"
                        + "-fx-text-fill: -app-text;");

        javafx.scene.layout.Region titleSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(titleSpacer, javafx.scene.layout.Priority.ALWAYS);

        String windowButtonBaseStyle =
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: -app-text;"
                        + "-fx-font-family: 'DM Sans';"
                        + "-fx-font-weight: 900;"
                        + "-fx-font-size: 13px;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 0;"
                        + "-fx-cursor: hand;";
        String windowButtonHoverStyle =
                "-fx-background-color: -app-surface-2;"
                        + "-fx-text-fill: -app-text;"
                        + "-fx-font-family: 'DM Sans';"
                        + "-fx-font-weight: 900;"
                        + "-fx-font-size: 13px;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 0;"
                        + "-fx-cursor: hand;";
        String closeButtonHoverStyle =
                "-fx-background-color: #ef4444;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-family: 'DM Sans';"
                        + "-fx-font-weight: 900;"
                        + "-fx-font-size: 13px;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 0;"
                        + "-fx-cursor: hand;";

        String macButtonBaseStyle =
                "-fx-background-radius: 999;" +
                "-fx-border-radius: 999;" +
                "-fx-min-width: 14px;" +
                "-fx-min-height: 14px;" +
                "-fx-pref-width: 14px;" +
                "-fx-pref-height: 14px;" +
                "-fx-max-width: 14px;" +
                "-fx-max-height: 14px;" +
                "-fx-padding: 0;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: rgba(255,255,255,0.18);" +
                "-fx-border-width: 1px;" +
                "-fx-font-size: 9px;" +
                "-fx-font-weight: 900;";

        Button minBtn = new Button();
        minBtn.setFocusTraversable(false);
        minBtn.setText("");
        minBtn.setStyle(macButtonBaseStyle + "-fx-background-color: #febc2e;");
        minBtn.setOnAction(ev -> dialog.setIconified(true));
        minBtn.setOnMouseEntered(ev -> {
            minBtn.setText("\u2013");
            minBtn.setStyle(macButtonBaseStyle
                    + "-fx-background-color: #febc2e;"
                    + "-fx-text-fill: #6a4700;");
        });
        minBtn.setOnMouseExited(ev -> {
            minBtn.setText("");
            minBtn.setStyle(macButtonBaseStyle + "-fx-background-color: #febc2e;");
        });

        Button maxBtn = new Button();
        maxBtn.setFocusTraversable(false);
        maxBtn.setText("");
        maxBtn.setStyle(macButtonBaseStyle + "-fx-background-color: #28c840;");
        maxBtn.setOnAction(ev -> dialog.setMaximized(!dialog.isMaximized()));
        maxBtn.setOnMouseEntered(ev -> {
            maxBtn.setText("+");
            maxBtn.setStyle(macButtonBaseStyle
                    + "-fx-background-color: #28c840;"
                    + "-fx-text-fill: #0d4f18;");
        });
        maxBtn.setOnMouseExited(ev -> {
            maxBtn.setText("");
            maxBtn.setStyle(macButtonBaseStyle + "-fx-background-color: #28c840;");
        });

        Button closeBtn = new Button();
        closeBtn.setFocusTraversable(false);
        closeBtn.setText("");
        closeBtn.setStyle(macButtonBaseStyle + "-fx-background-color: #ff5f57;");
        closeBtn.setOnAction(ev -> dialog.close());
        closeBtn.setOnMouseEntered(ev -> {
            closeBtn.setText("\u00d7");
            closeBtn.setStyle(macButtonBaseStyle
                    + "-fx-background-color: #ff5f57;"
                    + "-fx-text-fill: #5b0a0a;");
        });
        closeBtn.setOnMouseExited(ev -> {
            closeBtn.setText("");
            closeBtn.setStyle(macButtonBaseStyle + "-fx-background-color: #ff5f57;");
        });

        javafx.scene.layout.HBox windowControls = new javafx.scene.layout.HBox(8, minBtn, maxBtn, closeBtn);
        windowControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        titleBar.getChildren().addAll(titleLbl, titleSpacer, windowControls);

        final double[] xOffset = { 0 };
        final double[] yOffset = { 0 };
        titleBar.setOnMousePressed(ev -> {
            xOffset[0] = ev.getSceneX();
            yOffset[0] = ev.getSceneY();
        });
        titleBar.setOnMouseDragged(ev -> {
            dialog.setX(ev.getScreenX() - xOffset[0]);
            dialog.setY(ev.getScreenY() - yOffset[0]);
        });

        Label subtitleLabel = new Label("The system will automatically bid when someone outbids you.");
        subtitleLabel.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-family: 'DM Sans';"
                        + "-fx-text-fill: -app-text-muted;");
        subtitleLabel.setWrapText(true);

        Label priceBadge = new Label("Current price: " + MONEY_PREFIX + formatPrice(currentPrice));
        priceBadge.setStyle(
                "-fx-background-color: -app-accent-opacity-08;"
                        + "-fx-background-radius: 8px;"
                        + "-fx-padding: 10px 14px;"
                        + "-fx-font-family: 'DM Sans';"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: -fx-accent;"
                        + "-fx-border-color: -app-accent-opacity-30;"
                        + "-fx-border-radius: 8px;"
                        + "-fx-border-width: 1px;");
        priceBadge.setMaxWidth(Double.MAX_VALUE);

        Label maxBidLabel = new Label("Max Bid");
        maxBidLabel.setStyle(
                "-fx-font-family: 'DM Sans';"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: -app-text;");
        Label maxBidHint = new Label("Maximum price you are willing to pay");
        maxBidHint.setStyle(
                "-fx-font-family: 'DM Sans';"
                        + "-fx-font-size: 12px;"
                        + "-fx-text-fill: -app-text-muted;");

        String inputBaseStyle =
                "-fx-background-color: -app-input-bg;"
                        + "-fx-text-fill: -app-input-text;"
                        + "-fx-prompt-text-fill: -app-text-muted;"
                        + "-fx-border-color: -app-border;"
                        + "-fx-border-radius: 8px;"
                        + "-fx-background-radius: 8px;"
                        + "-fx-padding: 10px 14px;"
                        + "-fx-font-family: 'DM Sans';"
                        + "-fx-font-size: 14px;"
                        + "-fx-pref-height: 40px;";
        String inputFocusStyle = inputBaseStyle + "-fx-border-color: -fx-accent;";

        TextField maxBidField = new TextField();
        maxBidField.setPromptText("VD: 5000000");
        maxBidField.setStyle(inputBaseStyle);
        maxBidField.focusedProperty().addListener((obs, wasFocused, isFocused) ->
                maxBidField.setStyle(isFocused ? inputFocusStyle : inputBaseStyle));

        Label incLabel = new Label("Auto Increment");
        incLabel.setStyle(
                "-fx-font-family: 'DM Sans';"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: -app-text;");
        BigDecimal minimumIncrement = getEffectiveStepPrice();
        Label incHint = new Label("Minimum at current price: " + MONEY_PREFIX + formatPrice(minimumIncrement));
        incHint.setStyle(
                "-fx-font-family: 'DM Sans';"
                        + "-fx-font-size: 12px;"
                        + "-fx-text-fill: -app-text-muted;");

        TextField incrementField = new TextField();
        incrementField.setPromptText("VD: " + formatPrice(minimumIncrement));
        incrementField.setText(formatPrice(minimumIncrement));
        incrementField.setStyle(inputBaseStyle);
        incrementField.focusedProperty().addListener((obs, wasFocused, isFocused) ->
                incrementField.setStyle(isFocused ? inputFocusStyle : inputBaseStyle));

        VBox maxBidGroup = new VBox(4, maxBidLabel, maxBidHint, maxBidField);
        VBox incGroup = new VBox(4, incLabel, incHint, incrementField);

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(12);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.setStyle("-fx-padding: 10 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: -app-surface-2;"
                        + "-fx-text-fill: -app-text;"
                        + "-fx-font-family: 'DM Sans';"
                        + "-fx-font-weight: bold;"
                        + "-fx-font-size: 14px;"
                        + "-fx-background-radius: 10px;"
                        + "-fx-padding: 10px 24px;"
                        + "-fx-cursor: hand;");
        cancelBtn.setOnAction(ev -> dialog.close());

        Button activateBtn = new Button("Activate");
        activateBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, -fx-accent, #f06292);"
                        + "-fx-text-fill: white;"
                        + "-fx-font-family: 'DM Sans';"
                        + "-fx-font-weight: bold;"
                        + "-fx-font-size: 14px;"
                        + "-fx-background-radius: 10px;"
                        + "-fx-padding: 10px 24px;"
                        + "-fx-cursor: hand;");
        activateBtn.setOnAction(ev -> {
            if (processAutoBidInput(maxBidField.getText(), incrementField.getText())) {
                dialog.close();
            }
        });

        buttonBox.getChildren().addAll(cancelBtn, activateBtn);

        mainCard.getChildren().addAll(titleBar, subtitleLabel, priceBadge, maxBidGroup, incGroup, buttonBox);
        root.getChildren().add(mainCard);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        com.auction.client.service.AppStyleManager.applyCurrentStyle(scene);
        dialog.setScene(scene);

        Platform.runLater(maxBidField::requestFocus);
        dialog.showAndWait();
    }

    private boolean processAutoBidInput(String maxBidText, String incrementText) {
        BigDecimal maxBid;
        BigDecimal increment;

        try {
            maxBid = parseMoneyInput(maxBidText);
        } catch (NumberFormatException e) {
            showError("Max bid must be a valid number!");
            return false;
        }

        try {
            increment = parseMoneyInput(incrementText);
        } catch (NumberFormatException e) {
            showError("Increment must be a valid number!");
            return false;
        }

        if (maxBid.compareTo(currentPrice) <= 0) {
            showError("Max bid must be greater than current price (" + MONEY_PREFIX + formatPrice(currentPrice) + ")!");
            return false;
        }

        BigDecimal minimumIncrement = getEffectiveStepPrice();
        BigDecimal minimumAutoBid = currentPrice.add(minimumIncrement);
        if (maxBid.compareTo(minimumAutoBid) < 0) {
            showError("Max bid must be at least " + MONEY_PREFIX + formatPrice(minimumAutoBid) + "!");
            return false;
        }

        if (User.isBalanceLoaded()) {
            BigDecimal spendableForThisSession = User.getAvailableBalance();
            if (User.getId() != null && User.getId().equals(highestBidderId)) {
                spendableForThisSession = spendableForThisSession.add(currentPrice);
            }
            if (maxBid.compareTo(spendableForThisSession) > 0) {
                showError("Max bid cannot exceed available balance (" + MONEY_PREFIX
                        + formatPrice(spendableForThisSession) + ")!");
                return false;
            }
        }

        if (increment.compareTo(minimumIncrement) < 0) {
            showError("Auto increment must be at least " + MONEY_PREFIX + formatPrice(minimumIncrement) + "!");
            return false;
        }

        JSONObject json = new JSONObject();
        json.put("auctionId", currentSessionId);
        json.put("bidderId", User.getId());
        json.put("maxBid", maxBid);
        json.put("increment", increment);

        if (webSocket != null) {
            webSocket.sendText(AUTOBID_PREFIX + json.toString(), true);
        }

        messageLabel.setStyle(WARNING_STYLE);
        messageLabel.setText("Activating Auto-bidding...");
        logger.info("Sent AUTOBID request: {}", json);

        return true;
    }

    @FXML
    private void handleStartSelling(ActionEvent event) {
        disconnectSocket();
        logger.info("User pressed Start Selling (+), switching to UpToSeller.fxml");
        try {
            SceneSwitcher.switchScene(event, "UpToSeller.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Error switching to Seller registration page: ", e);
        }
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        disconnectSocket();

        try {
            SceneSwitcher.switchScene(event, MAIN_TEMPLATE_FXML);
        } catch (IOException e) {
            logger.error("Cannot go back to main template", e);
            showError("Error returning to previous page.");
        }
    }

    private void initBidTrajectoryCard() {
        allBidPoints.clear();
        seenBidIds.clear();
        seenCompositeKeys.clear();
        if (chartContainer != null)
            chartContainer.getChildren().clear();
        if (bidHistoryContainer != null)
            bidHistoryContainer.getChildren().clear();
        if (chartContainer != null && chartContainer.getParent() != null) {
            javafx.scene.Node card = chartContainer.getParent().getParent();
            if (card != null) {
                card.setCursor(javafx.scene.Cursor.HAND);
                card.setOnMouseClicked(e -> showFullBidHistoryDialog());
            }
        }
    }

    private void loadBidHistoryFromServer() {
        Thread t = new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auctions/" + currentSessionId + "/bid-history")).GET()
                        .build();
                HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
                if (isSuccessfulResponse(res)) {
                    org.json.JSONArray arr = new org.json.JSONArray(res.body());
                    Platform.runLater(() -> {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject b = arr.getJSONObject(i);
                            appendOrMergeBidPoint(b.optInt("bidId", -1), getMoney(b, "amount", BigDecimal.ZERO),
                                    b.optString("bidTime", null), b.optInt("bidderId", 0),
                                    b.optString("bidderName", "#????"));
                        }
                        allBidPoints.sort(java.util.Comparator
                                .comparingLong(com.auction.client.model.BidChartPoint::getEpochMillis));
                        bidCount = allBidPoints.size();
                        renderMiniChart();
                        renderRecentActivity();
                        updateBidInfoLabels();
                        if (fullHistoryUpdater != null)
                            fullHistoryUpdater.run();
                    });
                }
            } catch (Exception e) {
                logger.warn("Error loading bid history: {}", e.getMessage());
            }
        }, "auction-bid-history-loader");
        t.setDaemon(true);
        t.start();
    }

    private void appendOrMergeBidPoint(int bidId, BigDecimal amount, String bidTime, int bidderId,
            String maskedBidderCode) {
        if (bidId > 0) {
            if (seenBidIds.contains(bidId))
                return;
            seenBidIds.add(bidId);
        } else {
            String k = (bidTime != null ? bidTime : "") + "|" + bidderId + "|" + amount;
            if (seenCompositeKeys.contains(k))
                return;
            seenCompositeKeys.add(k);
        }
        boolean mine = User.getId() != null && bidderId == User.getId();
        com.auction.client.model.BidChartPoint pt = new com.auction.client.model.BidChartPoint(bidId, amount, bidTime,
                toEpochMillis(bidTime), bidderId, maskedBidderCode, mine);
        pt.setRelativeTime(formatRelativeTime(bidTime));
        allBidPoints.add(pt);
        while (allBidPoints.size() > MAX_CHART_POINTS)
            allBidPoints.remove(0);
    }

    private void renderMiniChart() {
        if (chartContainer == null)
            return;
        chartContainer.getChildren().clear();
        if (allBidPoints.isEmpty()) {
            Label el = new Label("No bids yet");
            el.setStyle("-fx-font-family:'DM Sans';-fx-font-size:11px;");
            chartContainer.getChildren().add(el);
            return;
        }
        int n = allBidPoints.size(), start = Math.max(0, n - MINI_CHART_POINTS);
        java.util.List<com.auction.client.model.BidChartPoint> recent = new java.util.ArrayList<>(
                allBidPoints.subList(start, n));
        javafx.scene.chart.NumberAxis xa = new javafx.scene.chart.NumberAxis(-0.3, recent.size() - 1 + 0.3, 1);
        xa.setTickLabelsVisible(false);
        xa.setTickMarkVisible(false);
        xa.setMinorTickVisible(false);
        xa.setOpacity(0);

        double minAmt = Double.MAX_VALUE;
        double maxAmt = Double.MIN_VALUE;
        for (com.auction.client.model.BidChartPoint p : recent) {
            double a = p.getAmount().doubleValue();
            if (a < minAmt)
                minAmt = a;
            if (a > maxAmt)
                maxAmt = a;
        }
        if (minAmt == Double.MAX_VALUE) {
            minAmt = 0;
            maxAmt = 10000;
        }
        double padding = getEffectiveStepPrice() != null ? getEffectiveStepPrice().doubleValue() : 0;
        if (padding <= 0)
            padding = currentPrice != null ? currentPrice.doubleValue() * 0.05 : 10000;
        if (padding <= 0)
            padding = 10000;
        double yLower = recent.get(0).getAmount().doubleValue();
        double yUpper = (currentPrice != null ? currentPrice.doubleValue() : maxAmt) + padding;
        if (yLower >= yUpper) {
            yLower = Math.max(0, yUpper - padding * 2);
        }

        javafx.scene.chart.NumberAxis ya = new javafx.scene.chart.NumberAxis(yLower, yUpper, (yUpper - yLower) / 4);
        ya.setAutoRanging(false);
        ya.setForceZeroInRange(false);
        ya.setTickMarkVisible(false);
        ya.setMinorTickVisible(false);
        ya.setTickLabelsVisible(true);
        ya.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number num) {
                double v = num.doubleValue();
                if (v >= 1000000000)
                    return MONEY_SYMBOL + String.format("%.1fB", v / 1000000000);
                if (v >= 1000000)
                    return MONEY_SYMBOL + String.format("%.1fM", v / 1000000);
                if (v >= 1000)
                    return MONEY_SYMBOL + String.format("%.1fK", v / 1000);
                return MONEY_SYMBOL + String.format("%.0f", v);
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
        ya.setStyle(
                "-fx-tick-label-font-family: 'DM Sans'; -fx-tick-label-font-size: 11px; -fx-tick-label-fill: -app-text-muted; -fx-font-weight: bold;");

        javafx.scene.chart.AreaChart<Number, Number> mc = new javafx.scene.chart.AreaChart<>(xa, ya);
        mc.setLegendVisible(false);
        mc.setAnimated(false);
        mc.setCreateSymbols(true);
        mc.setHorizontalGridLinesVisible(true);
        mc.setVerticalGridLinesVisible(false);
        mc.setAlternativeRowFillVisible(false);
        mc.setAlternativeColumnFillVisible(false);
        mc.setPrefHeight(140);
        mc.setMaxHeight(140);
        mc.setStyle("-fx-padding:0;-fx-background-color:transparent;");
        javafx.scene.chart.XYChart.Series<Number, Number> s = new javafx.scene.chart.XYChart.Series<>();
        for (int i = 0; i < recent.size(); i++)
            s.getData().add(new javafx.scene.chart.XYChart.Data<>(i, recent.get(i).getAmount().doubleValue()));
        mc.getData().add(s);
        Platform.runLater(() -> {
            try {
                javafx.scene.Node ln = mc.lookup(".default-color0.chart-series-area-line");
                if (ln != null)
                    ln.setStyle("-fx-stroke:-fx-accent;-fx-stroke-width:3px;");
                javafx.scene.Node fl = mc.lookup(".default-color0.chart-series-area-fill");
                if (fl != null)
                    fl.setStyle("-fx-fill:linear-gradient(to bottom,rgba(224,64,160,0.35),rgba(224,64,160,0.02));");
                javafx.scene.Node bg = mc.lookup(".chart-plot-background");
                if (bg != null)
                    bg.setStyle(
                            "-fx-background-color:transparent; -fx-border-color: transparent transparent #dcc8e0 #dcc8e0; -fx-border-width: 0 0 1 1;");
                javafx.scene.Node hgl = mc.lookup(".chart-horizontal-grid-lines");
                if (hgl != null)
                    hgl.setStyle("-fx-stroke: #f2e8f2; -fx-stroke-dash-array: 4 4;");
                for (int i = 0; i < s.getData().size(); i++) {
                    javafx.scene.Node sym = s.getData().get(i).getNode();
                    if (sym != null) {
                        boolean last = (i == s.getData().size() - 1);
                        sym.setStyle(
                                "-fx-background-color:-fx-accent,white;-fx-background-insets:0,2;-fx-background-radius:"
                                        + (last ? "10px" : "7px") + ";-fx-padding:" + (last ? "5" : "3.5") + ";");
                        com.auction.client.model.BidChartPoint p = recent.get(i);

                        String timeStr = p.getBidTime() != null ? p.getBidTime().replace("T", " ") : "";
                        if (timeStr.length() > 19)
                            timeStr = timeStr.substring(0, 19);
                        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(
                                "Bid #" + p.getBidId() + "\n" + p.getDisplayName() + "\n" + MONEY_PREFIX + formatPrice(p.getAmount())
                                        + "\n" + timeStr + "\n" + p.getRelativeTime());
                        tip.setStyle(
                                "-fx-font-family:'DM Sans';-fx-font-size:12px; -fx-background-color: rgba(46,26,40,0.9); -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 8px;");
                        tip.setShowDelay(javafx.util.Duration.millis(100));
                        javafx.scene.control.Tooltip.install(sym, tip);

                        sym.setCursor(javafx.scene.Cursor.HAND);
                        sym.setOnMouseClicked(ev -> showFullBidHistoryDialog());
                        sym.setOnMouseEntered(ev -> {
                            sym.setScaleX(1.3);
                            sym.setScaleY(1.3);
                        });
                        sym.setOnMouseExited(ev -> {
                            sym.setScaleX(1.0);
                            sym.setScaleY(1.0);
                        });
                    }
                }
            } catch (Exception ignored) {
            }
        });
        javafx.scene.layout.HBox xLabels = new javafx.scene.layout.HBox();
        xLabels.setAlignment(javafx.geometry.Pos.CENTER);
        xLabels.setStyle("-fx-padding:4 8 0 8;");
        for (int i = 0; i < recent.size(); i++) {
            boolean last = (i == recent.size() - 1);
            long sec = 999999;
            try {
                sec = Duration.between(LocalDateTime.parse(recent.get(i).getBidTime()), LocalDateTime.now())
                        .getSeconds();
            } catch (Exception ignored) {
            }
            String labelTxt = last ? (sec <= 60 ? "NOW" : "LATEST") : formatShortRelative(recent.get(i).getBidTime());
            Label lbl = new Label(labelTxt);
            lbl.setStyle("-fx-font-family:'DM Sans';-fx-font-size:11px;-fx-font-weight:900;"
                    + (last ? "-fx-text-fill:-fx-accent;" : ""));
            javafx.scene.layout.HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(javafx.geometry.Pos.CENTER);
            xLabels.getChildren().add(lbl);
        }
        chartContainer.getChildren().addAll(mc, xLabels);
    }

    private String formatShortRelative(String bidTime) {
        if (bidTime == null)
            return "START";
        try {
            long sec = Duration.between(LocalDateTime.parse(bidTime), LocalDateTime.now()).getSeconds();
            if (sec < 60)
                return sec + "s ago";
            if (sec < 3600)
                return (sec / 60) + "m ago";
            if (sec < 86400)
                return (sec / 3600) + "h ago";
            return (sec / 86400) + "d ago";
        } catch (Exception e) {
            return "START";
        }
    }

    private String formatRelativeTime(String bidTime) {
        if (bidTime == null)
            return "Past";
        try {
            long sec = Duration.between(LocalDateTime.parse(bidTime), LocalDateTime.now()).getSeconds();
            if (sec < 60)
                return sec + "s ago";
            if (sec < 3600)
                return (sec / 60) + "m ago";
            if (sec < 86400)
                return (sec / 3600) + "h ago";
            return (sec / 86400) + "d ago";
        } catch (Exception e) {
            return "Past";
        }
    }

    private long toEpochMillis(String iso) {
        if (iso == null || iso.isEmpty())
            return System.currentTimeMillis();
        try {
            return LocalDateTime.parse(iso).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private void renderRecentActivity() {
        if (bidHistoryContainer == null)
            return;
        bidHistoryContainer.getChildren().clear();
        int n = allBidPoints.size(), show = Math.min(n, 5);
        for (int i = 0; i < show; i++) {
            com.auction.client.model.BidChartPoint pt = allBidPoints.get(n - 1 - i);
            pt.setRelativeTime(formatRelativeTime(pt.getBidTime()));
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER);
            row.setOpacity(ACTIVITY_OPACITY[Math.min(i, ACTIVITY_OPACITY.length - 1)]);
            String dn = pt.isMine() ? pt.getMaskedBidderCode() + " (You)" : pt.getDisplayName();
            Label nl = new Label(dn);
            nl.setStyle("-fx-font-family:'DM Sans';-fx-font-size:12px;-fx-font-weight:bold;");
            javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
            Label al = new Label(MONEY_PREFIX + formatPrice(pt.getAmount()));
            al.setStyle("-fx-font-family:'DM Sans';-fx-font-size:13px;-fx-font-weight:900;");
            Label tl = new Label(pt.getRelativeTime());
            tl.setStyle("-fx-font-family:'DM Sans';-fx-font-size:10px;");
            tl.setPrefWidth(60);
            tl.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            row.getChildren().addAll(nl, sp, al, tl);
            bidHistoryContainer.getChildren().add(row);
        }
    }

    private Runnable fullHistoryUpdater = null;

    private void showFullBidHistoryDialog() {
        if (fullHistoryPopup != null && fullHistoryPopup.isShowing()) {
            fullHistoryPopup.requestFocus();
            return;
        }
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.NONE);
        popup.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        fullHistoryPopup = popup;
        popup.setOnCloseRequest(e -> {
            fullHistoryPopup = null;
            fullHistoryUpdater = null;
        });

        VBox root = new VBox();
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        root.setPrefSize(780, 740);
        try {
            URL baseCss = AuctionPageController.class.getResource("/com/auction/client/view/styles.css");
            if (baseCss != null) {
                root.getStylesheets().add(baseCss.toExternalForm());
            }
            com.auction.client.service.AppStyleManager.applyCurrentStyle(root);
        } catch (Exception ignored) {
        }

        VBox mainCard = new VBox(15);
        mainCard.setStyle(
                "-fx-padding: 24; -fx-background-color: -app-card; -fx-background-radius: 18; -fx-border-color: -app-border; -fx-border-radius: 18; -fx-border-width: 1.5;");
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setColor(javafx.scene.paint.Color.rgb(46, 26, 40, 0.15));
        shadow.setRadius(20);
        shadow.setOffsetY(8);
        mainCard.setEffect(shadow);
        VBox.setVgrow(mainCard, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().add(mainCard);

        // CSS for TableView and Chart
        String css = ".table-view { -fx-background-color: transparent; -fx-border-color: -app-border; -fx-border-radius: 8px; -fx-background-radius: 8px; } "
                +
                ".table-view .column-header-background { -fx-background-color: -app-surface-2; -fx-background-radius: 8px 8px 0 0; } "
                +
                ".table-view .column-header, .table-view .filler { -fx-background-color: transparent; -fx-size: 40px; -fx-border-color: -app-border; -fx-border-width: 0 0 1 0; } "
                +
                ".table-view .column-header .label {  -fx-font-weight: 900; -fx-font-size: 13px; -fx-font-family: 'DM Sans'; } "
                +
                ".table-view .table-row-cell { -fx-background-color: -app-card; -fx-border-color: -app-border; -fx-border-width: 0 0 1 0; -fx-cell-size: 45px; } "
                +
                ".table-view .table-row-cell:hover { -fx-background-color: -app-accent-chip-bg; } " +
                ".table-view .table-row-cell:selected { -fx-background-color: -app-accent-opacity-16; -fx-background-insets: 0; } "
                +
                ".table-view .table-cell { -fx-font-size: 13px; -fx-font-family: 'DM Sans'; } " +
                ".table-view .scroll-bar:vertical, .table-view .scroll-bar:horizontal { -fx-background-color: transparent; } "
                +
                ".table-view .scroll-bar:vertical .track, .table-view .scroll-bar:horizontal .track { -fx-background-color: transparent; -fx-border-color: transparent; -fx-background-radius: 0; } "
                +
                ".table-view .scroll-bar:vertical .thumb, .table-view .scroll-bar:horizontal .thumb { -fx-background-color: -app-accent-opacity-38; -fx-background-radius: 8px; } "
                +
                ".table-view .scroll-bar:vertical .thumb:hover, .table-view .scroll-bar:horizontal .thumb:hover { -fx-background-color: -app-accent-opacity-58; } "
                +
                ".table-view .scroll-bar .increment-button, .table-view .scroll-bar .decrement-button { -fx-background-color: transparent; -fx-padding: 0; } "
                +
                ".table-view .scroll-bar .increment-arrow, .table-view .scroll-bar .decrement-arrow { -fx-shape: \" \"; -fx-padding: 0; } "
                +
                ".table-view .corner { -fx-background-color: transparent; } " +
                ".chart-vertical-grid-lines { -fx-stroke: transparent; } " +
                ".chart-horizontal-grid-lines { -fx-stroke: -app-border; -fx-stroke-dash-array: 4 4; } " +
                ".axis { -fx-tick-label-fill: -app-text-muted; -fx-tick-label-font-size: 11px; } " +
                ".axis-label { -fx-text-fill: -app-text; -fx-font-weight: bold; -fx-font-size: 12px; }";
        try {
            java.io.File cssFile = java.io.File.createTempFile("popupStyle", ".css");
            cssFile.deleteOnExit();
            java.nio.file.Files.writeString(cssFile.toPath(), css);
            root.getStylesheets().add(cssFile.toURI().toString());
        } catch (Exception ignored) {
        }

        javafx.scene.layout.HBox titleBar = new javafx.scene.layout.HBox(10);
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-padding: 0 0 10 0; -fx-cursor: move;");

        Label titleLbl = new Label("Bid Trajectory & Full History");
        titleLbl.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 16px; -fx-font-weight: 900; ");

        javafx.scene.layout.Region titleSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(titleSpacer, javafx.scene.layout.Priority.ALWAYS);

        Button minBtn = new Button();
        minBtn.setAccessibleText("Minimize");
        minBtn.getStyleClass().addAll("mac-window-control", "mac-minimize");
        minBtn.setOnAction(ev -> popup.setIconified(true));

        Button maxBtn = new Button();
        maxBtn.setAccessibleText("Maximize");
        maxBtn.getStyleClass().addAll("mac-window-control", "mac-maximize");
        maxBtn.setOnAction(ev -> popup.setMaximized(!popup.isMaximized()));

        Button closeBtn = new Button();
        closeBtn.setAccessibleText("Close");
        closeBtn.getStyleClass().addAll("mac-window-control", "mac-close");
        closeBtn.setOnAction(ev -> {
            fullHistoryPopup = null;
            fullHistoryUpdater = null;
            popup.close();
        });

        titleBar.getChildren().addAll(titleLbl, titleSpacer, minBtn, maxBtn, closeBtn);

        final double[] xOffset = { 0 };
        final double[] yOffset = { 0 };
        titleBar.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            popup.setX(event.getScreenX() - xOffset[0]);
            popup.setY(event.getScreenY() - yOffset[0]);
        });

        javafx.scene.layout.HBox hdr = new javafx.scene.layout.HBox(12);
        hdr.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        javafx.scene.control.ComboBox<String> flt = new javafx.scene.control.ComboBox<>();
        flt.getItems().addAll("Last 10", "Last 50", "Full History");
        flt.setStyle(
                "-fx-background-color: -app-input-bg; -fx-border-color: -app-border; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 4 8; -fx-font-family:'DM Sans'; -fx-font-weight: bold; -fx-text-fill: -app-input-text; -fx-cursor: hand;");
        hdr.getChildren().addAll(flt);
        javafx.scene.chart.NumberAxis fxa = new javafx.scene.chart.NumberAxis();
        fxa.setLabel("Time");
        fxa.setAutoRanging(true);
        fxa.setForceZeroInRange(false);
        fxa.setTickMarkVisible(false);
        fxa.setMinorTickVisible(false);
        fxa.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            final java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

            @Override
            public String toString(Number v) {
                return v == null ? ""
                        : java.time.Instant.ofEpochMilli(v.longValue()).atZone(java.time.ZoneId.systemDefault())
                                .toLocalTime().format(f);
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
        javafx.scene.chart.NumberAxis fya = new javafx.scene.chart.NumberAxis();
        fya.setLabel("Price (VND)");
        fya.setAutoRanging(true);
        fya.setForceZeroInRange(false);
        fya.setTickMarkVisible(false);
        fya.setMinorTickVisible(false);
        fya.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number num) {
                double v = num.doubleValue();
                if (v >= 1000000000)
                    return String.format("%.1fB", v / 1000000000);
                if (v >= 1000000)
                    return String.format("%.1fM", v / 1000000);
                if (v >= 1000)
                    return String.format("%.1fK", v / 1000);
                return String.format("%.0f", v);
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
        javafx.scene.chart.LineChart<Number, Number> fc = new javafx.scene.chart.LineChart<>(fxa, fya);
        fc.setLegendVisible(false);
        fc.setAnimated(false);
        fc.setCreateSymbols(true);
        fc.setPrefHeight(240);
        javafx.scene.layout.HBox sb = new javafx.scene.layout.HBox(16);
        sb.setStyle(
                "-fx-padding:16;-fx-background-color:-app-accent-chip-bg;-fx-background-radius:12;-fx-border-color:-app-accent-chip-border;-fx-border-radius:12;-fx-border-width:1;");

        javafx.scene.control.TableView<com.auction.client.model.BidChartPoint> tbl = new javafx.scene.control.TableView<>();
        tbl.setStyle(
                "-fx-background-color: transparent; -fx-font-family: 'DM Sans';  -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        javafx.scene.control.TableColumn<com.auction.client.model.BidChartPoint, String> c1 = new javafx.scene.control.TableColumn<>(
                "Time");
        c1.setCellValueFactory(cd -> {
            if (cd.getValue().getBidTime() == null)
                return new javafx.beans.property.SimpleStringProperty("");
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(cd.getValue().getBidTime());
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter
                        .ofPattern("HH:mm:ss \u00b7 dd/MM/yyyy");
                return new javafx.beans.property.SimpleStringProperty(dt.format(dtf));
            } catch (Exception ex) {
                return new javafx.beans.property.SimpleStringProperty(cd.getValue().getBidTime());
            }
        });
        c1.setStyle("-fx-font-weight: bold; ");
        javafx.scene.control.TableColumn<com.auction.client.model.BidChartPoint, String> c2 = new javafx.scene.control.TableColumn<>(
                "Bidder");
        c2.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDisplayName()));
        c2.setStyle("-fx-font-weight: 900; ");
        javafx.scene.control.TableColumn<com.auction.client.model.BidChartPoint, String> c3 = new javafx.scene.control.TableColumn<>(
                "Amount");
        c3.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleStringProperty(MONEY_PREFIX + formatPrice(cd.getValue().getAmount())));
        c3.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: 900;  -fx-font-size: 14px;");
        javafx.scene.control.TableColumn<com.auction.client.model.BidChartPoint, String> c4 = new javafx.scene.control.TableColumn<>(
                "Increment");
        c4.setCellValueFactory(cd -> {
            int idx = allBidPoints.indexOf(cd.getValue());
            if (idx <= 0)
                return new javafx.beans.property.SimpleStringProperty("-");
            return new javafx.beans.property.SimpleStringProperty(
                    "+" + MONEY_PREFIX + formatPrice(cd.getValue().getAmount().subtract(allBidPoints.get(idx - 1).getAmount())));
        });
        c4.setStyle(
                "-fx-alignment: CENTER-RIGHT; -fx-text-fill: -fx-accent; -fx-font-weight: 900; -fx-font-size: 14px;");

        tbl.getColumns().addAll(java.util.List.of(c1, c2, c3, c4));
        tbl.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tbl, javafx.scene.layout.Priority.ALWAYS);

        Runnable pop = () -> {
            if (allBidPoints.isEmpty()) {
                fc.getData().clear();
                sb.getChildren().clear();
                tbl.getItems().clear();
                tbl.setPlaceholder(new Label("No bid history yet"));
                return;
            }
            String fv = flt.getValue();
            if (fv == null)
                fv = "Last 50";
            int lim = "Last 10".equals(fv) ? 10 : "Last 50".equals(fv) ? 50 : allBidPoints.size();
            int ss = Math.max(0, allBidPoints.size() - lim);
            java.util.List<com.auction.client.model.BidChartPoint> sub = new java.util.ArrayList<>(
                    allBidPoints.subList(ss, allBidPoints.size()));
            fc.getData().clear();
            javafx.scene.chart.XYChart.Series<Number, Number> fs = new javafx.scene.chart.XYChart.Series<>();
            for (com.auction.client.model.BidChartPoint p : sub)
                fs.getData()
                        .add(new javafx.scene.chart.XYChart.Data<>(p.getEpochMillis(), p.getAmount().doubleValue()));
            fc.getData().add(fs);
            Platform.runLater(() -> {
                javafx.scene.Node l = fc.lookup(".default-color0.chart-series-line");
                if (l != null)
                    l.setStyle("-fx-stroke:-fx-accent;-fx-stroke-width:2.5px;");
                javafx.scene.Node bg = fc.lookup(".chart-plot-background");
                if (bg != null)
                    bg.setStyle(
                            "-fx-background-color:transparent; -fx-border-color: transparent transparent -app-border -app-border; -fx-border-width: 0 0 1 1;");
                for (int i = 0; i < fs.getData().size(); i++) {
                    javafx.scene.Node sym = fs.getData().get(i).getNode();
                    if (sym != null) {
                        sym.setStyle(
                                "-fx-background-color: -fx-accent, white; -fx-background-insets: 0, 2; -fx-background-radius: 6px; -fx-padding: 4px;");
                        sym.setCursor(javafx.scene.Cursor.HAND);
                        com.auction.client.model.BidChartPoint p = sub.get(i);
                        String timeStr = p.getBidTime() != null ? p.getBidTime().replace("T", " ") : "";
                        if (timeStr.length() > 19)
                            timeStr = timeStr.substring(0, 19);
                        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(
                                "Bid #" + p.getBidId() + "\n" + p.getDisplayName() + "\n" + MONEY_PREFIX + formatPrice(p.getAmount())
                                        + "\n" + timeStr + "\n" + p.getRelativeTime());
                        tip.setStyle(
                                "-fx-font-family:'DM Sans';-fx-font-size:12px; -fx-background-color: rgba(46,26,40,0.9); -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 8px;");
                        tip.setShowDelay(javafx.util.Duration.millis(100));
                        javafx.scene.control.Tooltip.install(sym, tip);
                        sym.setOnMouseEntered(ev -> {
                            sym.setScaleX(1.3);
                            sym.setScaleY(1.3);
                        });
                        sym.setOnMouseExited(ev -> {
                            sym.setScaleX(1.0);
                            sym.setScaleY(1.0);
                        });
                    }
                }
            });
            sb.getChildren().clear();
            BigDecimal hi = allBidPoints.isEmpty() ? BigDecimal.ZERO
                    : allBidPoints.get(allBidPoints.size() - 1).getAmount();
            BigDecimal lo = allBidPoints.isEmpty() ? BigDecimal.ZERO : allBidPoints.get(0).getAmount();
            BigDecimal mxi = BigDecimal.ZERO;
            for (int i = 1; i < allBidPoints.size(); i++) {
                BigDecimal d = allBidPoints.get(i).getAmount().subtract(allBidPoints.get(i - 1).getAmount());
                if (d.compareTo(mxi) > 0)
                    mxi = d;
            }
            String lt = allBidPoints.isEmpty() ? "-"
                    : formatRelativeTime(allBidPoints.get(allBidPoints.size() - 1).getBidTime());
            String[][] sts = { { "Total Bids", "" + allBidPoints.size() }, { "Highest", MONEY_PREFIX + formatPrice(hi) },
                    { "Start", MONEY_PREFIX + formatPrice(lo) }, { "Max Delta", "+" + MONEY_PREFIX + formatPrice(mxi) }, { "Last Bid", lt } };
            for (String[] st : sts) {
                VBox sv = new VBox(4);
                sv.setAlignment(javafx.geometry.Pos.CENTER);
                Label k = new Label(st[0]);
                k.setStyle("-fx-font-family:'DM Sans';-fx-font-size:11px;-fx-font-weight:700;");
                Label v = new Label(st[1]);
                v.setStyle("-fx-font-family:'DM Sans';-fx-font-size:16px;-fx-font-weight:900;");
                sv.getChildren().addAll(k, v);
                javafx.scene.layout.HBox.setHgrow(sv, javafx.scene.layout.Priority.ALWAYS);
                sb.getChildren().add(sv);
            }
            javafx.collections.ObservableList<com.auction.client.model.BidChartPoint> items = javafx.collections.FXCollections
                    .observableArrayList();
            for (int i = sub.size() - 1; i >= 0; i--)
                items.add(sub.get(i));
            tbl.setItems(items);
        };
        flt.setOnAction(e -> pop.run());
        flt.setValue("Last 50"); // This will also trigger the action if it changes, but just to be safe:
        pop.run();
        this.fullHistoryUpdater = pop;

        mainCard.getChildren().addAll(titleBar, hdr, fc, sb, tbl);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        com.auction.client.service.AppStyleManager.applyCurrentStyle(scene);
        popup.show();
    }

    public void setRemainingTime(String endTimeStr) {
        stopTimeline();

        if (endTimeStr == null || endTimeStr.isBlank()) {
            remainingTimeLabel.setText("Loading...");
            return;
        }

        try {
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr.trim());

            // Update immediately so the UI never shows a fake 00:00:00 value.
            updateRemainingTime(endTime);

            timeline = new Timeline(
                    new KeyFrame(javafx.util.Duration.seconds(1), event -> updateRemainingTime(endTime)));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        } catch (Exception e) {
            remainingTimeLabel.setText("Unknown time");
            logger.warn("Invalid auction end time: {}", endTimeStr);
        }
    }

    private void initDefaultView() {
        productNameLabel.setText("Loading...");
        currentPriceLabel.setText("...");
        remainingTimeLabel.setText("Loading...");

        setLabelText(minIncrementLabel, "Min increment " + MONEY_PREFIX + "0");
        setLabelText(highestBidderLabel, DEFAULT_HIGHEST_BIDDER);
        setLabelText(reserveStatusLabel, "");
        if (reserveStatusLabel != null) {
            reserveStatusLabel.setVisible(false);
            reserveStatusLabel.setManaged(false);
        }
        setLabelText(totalBidsLabel, "0");
        setLabelText(watchingLabel, "0");
        setLabelText(itemDescriptionLabel, DEFAULT_DESCRIPTION);
    }

    private void setupResponsiveFontListeners() {
        Platform.runLater(() -> {
            if (currentPriceLabel.getScene() == null) {
                return;
            }

            double initialWidth = currentPriceLabel.getScene().getWidth();

            currentPriceLabel.getScene().widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    updateResponsiveFonts(newVal.doubleValue());
                }
            });

            currentPriceLabel.textProperty().addListener((obs, oldVal, newVal) -> {
                if (currentPriceLabel.getScene() != null) {
                    updateResponsiveFonts(currentPriceLabel.getScene().getWidth());
                }
            });

            remainingTimeLabel.textProperty().addListener((obs, oldVal, newVal) -> {
                if (currentPriceLabel.getScene() != null) {
                    updateResponsiveFonts(currentPriceLabel.getScene().getWidth());
                }
            });

            productNameLabel.textProperty().addListener((obs, oldVal, newVal) -> {
                if (currentPriceLabel.getScene() != null) {
                    updateResponsiveFonts(currentPriceLabel.getScene().getWidth());
                }
            });

            updateResponsiveFonts(initialWidth);
        });
    }

    private void updateResponsiveFonts(double windowWidth) {
        double scale = calculateAuctionPageScale(windowWidth);
        double priceFont = calculatePriceFont(windowWidth);
        double timeFont = calculateTimeFont(windowWidth);
        double productNameFont = calculateProductNameFont(windowWidth);
        double startPriceFont = Math.max(11, Math.min(20, windowWidth * 0.012));
        double bidFieldFont = Math.max(16, Math.min(24, windowWidth * 0.017));
        double compactLabelFont = Math.max(9, Math.min(12, 12 * scale));
        double quickBidFont = Math.max(9, Math.min(12, 12 * scale));
        double mediaHeight = Math.max(250, Math.min(400, 400 * scale));

        if (mainContentGrid != null) {
            mainContentGrid.setHgap(Math.max(14, 24 * scale));
        }

        if (productMediaFrame != null) {
            productMediaFrame.setPrefHeight(mediaHeight);
            productMediaFrame.setMaxHeight(mediaHeight);
        }

        if (productImageView != null) {
            productImageView.setFitHeight(mediaHeight);
        }

        setNodeStyle(currentPriceLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) priceFont + "px; -fx-font-weight: 900; ");

        setNodeStyle(highestBidTitleLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) Math.max(10, Math.min(14, windowWidth * 0.01))
                        + "px; -fx-font-weight: 900; ");

        setNodeStyle(remainingTimeLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) timeFont
                        + "px; -fx-font-weight: 900; -fx-text-fill: -fx-accent;");

        setNodeStyle(endingInTitleLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) Math.max(10, timeFont * 0.7)
                        + "px; -fx-font-weight: 900; -fx-text-fill: -app-text-muted;");

        setNodeStyle(productNameLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) productNameFont
                        + "px; -fx-font-weight: 900; -fx-text-fill: -app-text;");

        setNodeStyle(startPriceLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) startPriceFont
                        + "px; -fx-font-weight: 900; -fx-text-fill: -app-text;");

        setNodeStyle(startPriceTitleLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) Math.max(8, Math.min(12, windowWidth * 0.008))
                        + "px; -fx-font-weight: 900; -fx-text-fill: -app-text-muted; -fx-padding: 8 0 0 0;");

        setNodeStyle(bidAmountField,
                "-fx-background-color: -app-input-bg; -fx-background-radius: 999; -fx-text-fill: -app-input-text; "
                        + "-fx-border-color: -app-border; -fx-border-width: 2; "
                        + "-fx-border-radius: 999; -fx-padding: " + (int) Math.max(12, 16 * scale)
                        + " 16 " + (int) Math.max(12, 16 * scale) + " 48; "
                        + "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) bidFieldFont
                        + "px; -fx-font-weight: 900;");

        setNodeStyle(bidAmountTitleLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) Math.max(10, Math.min(14, 14 * scale))
                        + "px; -fx-font-weight: 900; -fx-text-fill: -app-text;");

        setNodeStyle(minIncrementLabel,
                "-fx-font-family: 'DM Sans'; -fx-font-size: " + (int) compactLabelFont
                        + "px; -fx-font-weight: 900; -fx-text-fill: -fx-accent;");

        if (productNameLabel != null) {
            productNameLabel.setWrapText(true);
            productNameLabel.setMaxWidth(Double.MAX_VALUE);
            productNameLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        }

        if (remainingTimeLabel != null) {
            remainingTimeLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            if (remainingTimeLabel.getParent() instanceof javafx.scene.layout.Region parentRegion) {
                parentRegion.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            }
        }

        setQuickBidButtonFont(btnQuickBidOne, quickBidFont);
        setQuickBidButtonFont(btnQuickBidTwo, quickBidFont);
        setQuickBidButtonFont(btnQuickBidFive, quickBidFont);
        setButtonFont(placeBidBtn, Math.max(14, Math.min(18, 18 * scale)));
        setButtonFont(btnAutoBid, Math.max(10, Math.min(14, 14 * scale)));
    }

    private double calculatePriceFont(double windowWidth) {
        String priceText = currentPriceLabel.getText();
        double baseFont = Math.max(19, Math.min(48, windowWidth * 0.031));
        int extraChars = Math.max(0, priceText.length() - 8);
        return Math.max(16, baseFont - extraChars * 1.7);
    }

    private double calculateTimeFont(double windowWidth) {
        String timeText = remainingTimeLabel.getText();
        double baseFont = Math.max(11, Math.min(22, windowWidth * 0.014));
        int extraChars = Math.max(0, timeText.length() - 8);
        return Math.max(10, baseFont - extraChars * 0.95);
    }

    private double calculateProductNameFont(double windowWidth) {
        String nameText = productNameLabel.getText();
        double baseFont = Math.max(20, Math.min(30, windowWidth * 0.021));
        int extraChars = Math.max(0, nameText == null ? 0 : nameText.length() - 28);
        return Math.max(17, baseFont - extraChars * 0.22);
    }

    private double calculateAuctionPageScale(double windowWidth) {
        return Math.max(0.68, Math.min(1.0, windowWidth / 1500.0));
    }

    private void setQuickBidButtonFont(Button button, double fontSize) {
        if (button == null) {
            return;
        }
        button.setStyle("-fx-background-color: -app-accent-opacity-08; -fx-text-fill: -fx-accent; "
                + "-fx-border-color: -fx-accent; -fx-border-width: 1; -fx-border-radius: 999; "
                + "-fx-background-radius: 999; -fx-padding: 7 8; -fx-font-family: 'DM Sans'; "
                + "-fx-font-size: " + (int) fontSize + "px; -fx-font-weight: 900; -fx-cursor: hand;");
    }

    private void setButtonFont(Button button, double fontSize) {
        if (button == null) {
            return;
        }
        String style = button.getStyle();
        if (style == null) {
            style = "";
        }
        button.setStyle(style.replaceAll("-fx-font-size:\\s*\\d+(\\.\\d+)?px;", "")
                + " -fx-font-size: " + (int) fontSize + "px;");
    }

    private void refreshSessionFromServer() {
        Thread refreshThread = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auctions/" + currentSessionId))
                        .GET()
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

                if (isSuccessfulResponse(response)) {
                    JSONObject session = new JSONObject(response.body());
                    JSONObject item = session.optJSONObject("item");
                    Platform.runLater(() -> applySessionData(session, item));
                }
            } catch (Exception e) {
                logger.warn("Could not reload session {} details: {}", currentSessionId, e.getMessage());
            }
        }, "auction-session-refresh");

        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private boolean isSuccessfulResponse(HttpResponse<String> response) {
        return response.statusCode() == 200
                && response.body() != null
                && !response.body().isBlank();
    }

    private void applySessionData(JSONObject sessionObj, JSONObject itemObj) {
        sellerId = getOptionalInt(sessionObj, "sellerId");
        auctionOpenForBidding = "ACTIVE".equalsIgnoreCase(sessionObj.optString("status", "ACTIVE"));
        loadPriceData(sessionObj);
        loadProductData(sessionObj, itemObj);
        updatePriceLabels();
        updateBidInfoLabels();
        updateBiddingAvailability();

        String endTime = sessionObj.optString("endTime", "");
        if (!endTime.isEmpty()) {
            setRemainingTime(endTime);
        }
    }

    private void loadPriceData(JSONObject sessionObj) {
        startingPrice = getMoneyAny(sessionObj, BigDecimal.ZERO, "startingPrice", "startPrice");
        currentPrice = getMoneyAny(sessionObj, startingPrice, "currentPrice", "highestBid");
        stepPrice = getMoneyAny(sessionObj, BigDecimal.ZERO, "stepPrice", "minBidIncrement");
        reservePrice = getMoneyAny(sessionObj, BigDecimal.ZERO, "reservePrice");
        highestBidderId = getOptionalInt(sessionObj, "highestBidderId");
        bidCount = getBidCount(sessionObj);

        if (currentPrice.compareTo(BigDecimal.ZERO) <= 0 && startingPrice.compareTo(BigDecimal.ZERO) > 0) {
            currentPrice = startingPrice;
        }
    }

    private void loadProductData(JSONObject sessionObj, JSONObject itemObj) {
        JSONObject source = itemObj != null ? itemObj : sessionObj;

        String productName = source.optString(
                "name",
                sessionObj.optString("productName", DEFAULT_PRODUCT_NAME));

        String description = source.optString(
                "description",
                sessionObj.optString("description", ""));

        String imagePath = firstNonBlank(
                getString(source, "imagePath"),
                getString(source, "imageUrl"),
                getString(source, "image_url"),
                getString(sessionObj, "imagePath"),
                getString(sessionObj, "imageUrl"),
                getString(sessionObj, "image_url"),
                productImagePath);

        productNameLabel.setText(productName);
        updateDescription(description);
        loadProductImage(imagePath);

        // Reset and check 3D Mode
        String effectiveImagePath = firstNonBlank(productImagePath, imagePath);
        this.itemUuid = extractUuid(effectiveImagePath);
        this.is3DMode = false;
        if (model3DContainer != null) {
            model3DContainer.setVisible(false);
            model3DContainer.setManaged(false);
            model3DContainer.getChildren().clear();
        }
        if (productImageView != null) {
            productImageView.setVisible(true);
            productImageView.setManaged(true);
        }
        if (btnToggle3D != null) {
            btnToggle3D.setVisible(false);
            btnToggle3D.setManaged(false);
            if (btnToggle3D.getGraphic() instanceof javafx.scene.layout.HBox) {
                javafx.scene.layout.HBox hbox = (javafx.scene.layout.HBox) btnToggle3D.getGraphic();
                if (hbox.getChildren().size() >= 2 && hbox.getChildren().get(0) instanceof Label
                        && hbox.getChildren().get(1) instanceof Label) {
                    Label iconLabel = (Label) hbox.getChildren().get(0);
                    Label textLabel = (Label) hbox.getChildren().get(1);
                    iconLabel.setText("\uE0B4"); // 3d_rotation
                    textLabel.setText("3D VIEW");
                }
            }
        }
        check3DModelExists(resolveModelUrl(effectiveImagePath));
    }

    private void updatePriceLabels() {
        currentPriceLabel.setText(MONEY_PREFIX + formatPrice(currentPrice));

        if (startingPrice.compareTo(BigDecimal.ZERO) >= 0) {
            startPriceLabel.setText(MONEY_PREFIX + formatPrice(startingPrice));
        } else {
            startPriceLabel.setText("---");
        }
    }

    private void loadProductImage(String imagePath) {
        String imageUrl = buildImageUrl(imagePath);

        if (imageUrl.isBlank()) {
            if (productImagePath == null || productImagePath.isBlank()) {
                productImageView.setImage(null);
            }
            return;
        }

        boolean hasAcceptedPath = productImagePath != null && !productImagePath.isBlank();

        try {
            Image image = CacheManager.getCachedImage(imageUrl, newImage -> {
                if (isDisplayableImage(newImage)
                        && productImageView != null
                        && !is3DMode
                        && imageUrl.equals(buildImageUrl(productImagePath))) {
                    productImageView.setImage(newImage);
                }
            });
            if (image != null) {
                image.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        logger.warn("Could not load product image from {}", imageUrl);
                    }
                });
                if (isDisplayableImage(image)) {
                    productImagePath = imagePath;
                    productImageView.setImage(image);
                } else if (!hasAcceptedPath && !isDisplayableImage(productImageView.getImage())) {
                    productImagePath = imagePath;
                    productImageView.setImage(image);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading product image from {}", imageUrl, e);
        }
    }

    private boolean isDisplayableImage(Image image) {
        return image != null
                && !image.isError()
                && image.getWidth() > 1
                && image.getHeight() > 1;
    }

    private String getString(JSONObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.isNull(key)) {
            return "";
        }
        return object.optString(key, "");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private BigDecimal getEffectiveStepPrice() {
        BigDecimal dynamicIncrement = getDynamicStepPrice(currentPrice);
        if (stepPrice != null && stepPrice.compareTo(dynamicIncrement) > 0) {
            return stepPrice;
        }
        return dynamicIncrement;
    }

    private BigDecimal getDynamicStepPrice(BigDecimal price) {
        return BidStepPolicy.getDynamicStepPrice(price);
    }

    private void updateBidInfoLabels() {
        updateQuickBidLabels(getEffectiveStepPrice());
        setLabelText(minIncrementLabel, "Min increment " + MONEY_PREFIX + formatPrice(getEffectiveStepPrice()));
        setLabelText(highestBidderLabel, formatHighestBidder());
        updateReserveStatusLabel();
        int displayBidCount = Math.max(Math.max(0, bidCount), allBidPoints.size());
        setLabelText(totalBidsLabel, String.valueOf(displayBidCount));
        setLabelText(watchingLabel, String.valueOf(Math.max(0, watchingCount)));
    }

    private void updateQuickBidLabels(BigDecimal increment) {
        if (increment == null) {
            return;
        }
        if (btnQuickBidOne != null) {
            btnQuickBidOne.setText("+ " + formatPrice(increment));
        }
        if (btnQuickBidTwo != null) {
            btnQuickBidTwo.setText("+ " + formatPrice(increment.multiply(BigDecimal.valueOf(2))));
        }
        if (btnQuickBidFive != null) {
            btnQuickBidFive.setText("+ " + formatPrice(increment.multiply(BigDecimal.valueOf(5))));
        }
    }

    private String formatHighestBidder() {
        if (highestBidderId == null) {
            return DEFAULT_HIGHEST_BIDDER;
        }
        if (User.getId() != null && highestBidderId.equals(User.getId())) {
            return "Highest bidder: You";
        }
        return "Highest bidder: User #" + highestBidderId;
    }

    private void updateReserveStatusLabel() {
        if (reserveStatusLabel == null) {
            return;
        }

        if (reservePrice == null || reservePrice.compareTo(BigDecimal.ZERO) <= 0) {
            reserveStatusLabel.setText("");
            reserveStatusLabel.setVisible(false);
            reserveStatusLabel.setManaged(false);
            return;
        }

        boolean reserveMet = currentPrice.compareTo(reservePrice) >= 0;
        reserveStatusLabel.setVisible(true);
        reserveStatusLabel.setManaged(true);
        reserveStatusLabel.setText(reserveMet ? "Reserve Met" : "Under Reserve");
        reserveStatusLabel.setStyle(reserveMet ? RESERVE_MET_STYLE : UNDER_RESERVE_STYLE);
    }

    private void connectToServer() {
        disconnectSocket();
        startSocketListener("auction-socket-listener");
    }

    private void reconnectBidSocket() {
        logger.info("Reconnecting bid WebSocket for session {}", currentSessionId);
        connectToServer();
    }

    private void startSocketListener(String threadName) {
        Thread listenerThread = new Thread(() -> {
            String wsUrl = Config.API_URL.replace("https://", "wss://").replace("http://", "ws://")
                    + "/ws/notification";
            logger.info("Connecting to session WebSocket: {}", wsUrl);

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new java.net.http.WebSocket.Listener() {
                        private final StringBuilder buffer = new StringBuilder();

                        @Override
                        public void onOpen(java.net.http.WebSocket ws) {
                            logger.info("WebSocket connection opened for session {}", currentSessionId);
                            webSocket = ws;

                            if (com.auction.client.model.User.getSessionToken() != null) {
                                JSONObject authJson = new JSONObject();
                                authJson.put("token", com.auction.client.model.User.getSessionToken());
                                ws.sendText("AUTH:" + authJson.toString(), true);
                            }

                            ws.sendText(JOIN_PREFIX + currentSessionId, true);
                            ws.request(1);
                        }

                        @Override
                        public java.util.concurrent.CompletionStage<?> onText(
                                java.net.http.WebSocket ws,
                                CharSequence data,
                                boolean last) {
                            buffer.append(data);
                            if (last) {
                                String msg = buffer.toString();
                                buffer.setLength(0);
                                handleServerMessage(msg);
                            }
                            ws.request(1);
                            return null;
                        }

                        @Override
                        public java.util.concurrent.CompletionStage<?> onClose(
                                java.net.http.WebSocket ws,
                                int statusCode,
                                String reason) {
                            logger.info("WebSocket connection closed: {} - {}", statusCode, reason);
                            return null;
                        }

                        @Override
                        public void onError(java.net.http.WebSocket ws, Throwable error) {
                            logger.error("WebSocket error", error);
                            Platform.runLater(() -> {
                                finishBidProcessing();
                                clearLocalBidRequest("socket error");
                                showError("Lost connection to Socket server!");
                            });
                        }
                    })
                    .exceptionally(error -> {
                        logger.error("Failed to connect session WebSocket", error);
                        Platform.runLater(() -> showError("Cannot connect to Socket server!"));
                        return null;
                    });
        }, threadName);

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleServerMessage(String serverResponse) {
        logger.info("Received raw message from server: {}", serverResponse);
        try {
            if (serverResponse.startsWith(NOTICE_PREFIX)) {
                handleNoticeMessage(serverResponse.substring(NOTICE_PREFIX.length()));
            } else if (serverResponse.startsWith(RESPONSE_PREFIX)) {
                handleBidResponseMessage(serverResponse.substring(RESPONSE_PREFIX.length()));
            } else if (serverResponse.startsWith(ROOM_COUNT_PREFIX)) {
                handleRoomCountMessage(serverResponse.substring(ROOM_COUNT_PREFIX.length()));
            } else if (serverResponse.startsWith("WATCHING:")) {
                // Ignore WATCHING log clutter, it's handled or we don't care here if it's not
                // implemented
            } else {
                logger.warn("Unknown message prefix: {}", serverResponse);
            }
        } catch (Exception e) {
            logger.error("Error parsing/handling server message: raw={}, error={}", serverResponse, e.getMessage(), e);
        }
    }

    private void handleNoticeMessage(String jsonString) {
        JSONObject noticeObj = new JSONObject(jsonString);
        logger.info("handleNoticeMessage: auctionId={}, bidderId={}, highestBidderId={}, newPrice={}",
                noticeObj.optInt("auctionId", currentSessionId),
                noticeObj.optInt("bidderId", -1),
                noticeObj.opt("highestBidderId"),
                noticeObj.opt("newPrice"));

        Platform.runLater(() -> {
            BigDecimal newPrice = noticeObj.getBigDecimal("newPrice");
            currentPrice = newPrice;
            currentPriceLabel.setText(MONEY_PREFIX + formatPrice(newPrice));

            highestBidderId = getOptionalInt(noticeObj, "highestBidderId");
            bidCount = getOptionalIntOrDefault(noticeObj, "bidCount", bidCount);
            updateBidInfoLabels();

            // ============ REALTIME CHART UPDATE ============
            int bidId = noticeObj.optInt("bidId", -1);
            int bidderId = noticeObj.optInt("bidderId", 0);
            String bidTime = noticeObj.optString("bidTime", null);
            String maskedCode = noticeObj.optString("maskedBidderCode",
                    "#" + String.format("%04d", Math.abs(bidderId) % 10000));
            appendOrMergeBidPoint(bidId, newPrice, bidTime, bidderId, maskedCode);
            allBidPoints
                    .sort(java.util.Comparator.comparingLong(com.auction.client.model.BidChartPoint::getEpochMillis));
            renderMiniChart();
            renderRecentActivity();
            // ================================================

            if (noticeObj.has("newEndTime")) {
                handleAuctionExtended(noticeObj.getString("newEndTime"));
            } else {
                if (User.getId() == null || highestBidderId == null || !highestBidderId.equals(User.getId())) {
                    showInfo("Someone just placed a new bid!");
                }
            }
            fetchLatestUserBalance();
        });
    }

    private void handleAuctionExtended(String newEndTime) {
        messageLabel.setStyle(EXTENSION_STYLE);
        messageLabel.setText("The auction session has been extended by 60 seconds!");
        setRemainingTime(newEndTime);
    }

    private void handleBidResponseMessage(String jsonString) {
        JSONObject responseObj = new JSONObject(jsonString);
        logger.info("handleBidResponseMessage: auctionId={}, bidderId={}, highestBidderId={}, type={}, success={}, pendingLocalBid={}",
                responseObj.opt("auctionId"),
                responseObj.opt("bidderId"),
                responseObj.opt("highestBidderId"),
                responseObj.optString("type", ""),
                responseObj.optBoolean("success", false),
                hasPendingLocalBidRequest());

        Platform.runLater(() -> {
            if (responseObj.getBoolean("success")) {
                if ("AUTOBID_CONFIG".equals(responseObj.optString("type"))) {
                    if (!hasPendingLocalBidRequest()) {
                        finishBidProcessing();
                    }
                    messageLabel.setStyle(SUCCESS_STYLE);
                    messageLabel.setText(responseObj.optString("message"));

                    AppNotification notif = new AppNotification(NotificationType.AUTO_BID_CONFIGURED,
                            NotificationSeverity.SUCCESS,
                            "Auto-bid configured",
                            "The system will automatically bid on " + productNameLabel.getText());
                    notif.setAuctionId(currentSessionId);
                    notif.setItemName(productNameLabel.getText());
                    NotificationCenterService.getInstance().addNotification(notif);
                    fetchLatestUserBalance();
                } else {
                    if (!isExpectedLocalBidResponse(responseObj)) {
                        logger.info("Ignoring successful RESPONSE without matching local BID request: {}", responseObj);
                        return;
                    }
                    finishBidProcessing();
                    clearLocalBidRequest("successful response handled");
                    handleSuccessfulBid(responseObj);
                }
            } else {
                if (!hasPendingLocalBidRequest()) {
                    logger.info("Ignoring non-bid failure RESPONSE with no local BID request: {}", responseObj);
                    return;
                }
                boolean expectedFailure = isExpectedLocalBidResponse(responseObj);
                finishBidProcessing();
                clearLocalBidRequest("failure response handled");
                if (!expectedFailure) {
                    logger.warn("Ignoring failure RESPONSE that does not match local BID request: {}", responseObj);
                    return;
                }
                showError(responseObj.optString("message", "Bid placement failed."));
                AppNotification notif = new AppNotification(NotificationType.BID_FAILED, NotificationSeverity.DANGER,
                        "Bid failed", responseObj.optString("message", "Bid placement failed."));
                notif.setAuctionId(currentSessionId);
                notif.setItemName(productNameLabel.getText());
                if (bidErrorSoundPlayedForCurrentAttempt) {
                    notif.setSoundMuted(true);
                } else {
                    bidErrorSoundPlayedForCurrentAttempt = true;
                }
                NotificationCenterService.getInstance().addNotification(notif);
            }
        });
    }

    private void handleSuccessfulBid(JSONObject responseObj) {
        logger.info("handleSuccessfulBid: auctionId={}, bidderId={}, highestBidderId={}, bidId={}, currentUserId={}",
                responseObj.opt("auctionId"),
                responseObj.opt("bidderId"),
                responseObj.opt("highestBidderId"),
                responseObj.opt("bidId"),
                User.getId());
        currentPrice = getMoney(responseObj, "currentPrice", currentPrice);
        highestBidderId = getOptionalInt(responseObj, "highestBidderId");
        bidCount = getOptionalIntOrDefault(responseObj, "bidCount", bidCount);

        currentPriceLabel.setText(MONEY_PREFIX + formatPrice(currentPrice));
        updateBidInfoLabels();

        messageLabel.setStyle(SUCCESS_STYLE);
        messageLabel.setText(responseObj.optString("message", "Bid placed successfully."));
        bidAmountField.clear();

        AppNotification notif = new AppNotification(NotificationType.BID_SUCCESS, NotificationSeverity.SUCCESS,
                "Bid successful", "You have successfully bid on " + productNameLabel.getText());
        notif.setAuctionId(currentSessionId);
        notif.setItemName(productNameLabel.getText());
        NotificationCenterService.getInstance().addNotification(notif);
        fetchLatestUserBalance();
    }

    private void fetchLatestUserBalance() {
        if (User.getId() == null)
            return;
        Thread balanceThread = new Thread(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + User.getId()))
                        .GET();
                if (User.getSessionToken() != null) {
                    builder.header("X-Auth-Token", User.getSessionToken());
                }
                HttpRequest request = builder.build();
                HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.body());
                    if (responseJson.optInt("status", 500) == 200) {
                        JSONObject data = responseJson.optJSONObject("data");
                        if (data != null) {
                            BigDecimal balance = new BigDecimal(data.opt("balance").toString());
                            BigDecimal frozen = new BigDecimal(data.opt("frozenBalance").toString());
                            User.updateProfile(
                                    data.optString("username", User.getUsername()),
                                    data.optString("fullname", User.getFullname()),
                                    data.optString("email", User.getEmail()),
                                    data.optString("dob", User.getDob()),
                                    data.optString("placeOfBirth",
                                            data.optString("place_of_birth", User.getPlace_of_birth())),
                                    balance,
                                    frozen,
                                    data.optString("avatarUrl", data.optString("avatar_url", User.getAvatarUrl())));
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch latest user balance: {}", e.getMessage());
            }
        }, "auction-user-balance-refresh");
        balanceThread.setDaemon(true);
        balanceThread.start();
    }

    private void handleRoomCountMessage(String countText) {
        int count = Integer.parseInt(countText);

        Platform.runLater(() -> {
            watchingCount = count;
            updateBidInfoLabels();
        });
    }

    private void disconnectSocket() {
        try {
            if (webSocket != null) {
                webSocket.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "Disconnecting");
            }
        } catch (Exception e) {
            logger.warn("Cannot close WebSocket", e);
        }
        webSocket = null;

        stopTimeline();
        stopBidTimeout();
        clearLocalBidRequest("socket disconnected");
    }

    private synchronized void markLocalBidRequestInFlight(BigDecimal bidAmount) {
        bidRequestInFlight = true;
        pendingBidAuctionId = currentSessionId;
        pendingBidderId = User.getId();
        pendingBidAmount = bidAmount;
        logger.info("Local BID request marked in-flight: auctionId={}, bidderId={}, amount={}",
                pendingBidAuctionId, pendingBidderId, pendingBidAmount);
    }

    private synchronized boolean hasPendingLocalBidRequest() {
        return bidRequestInFlight;
    }

    private synchronized void clearLocalBidRequest(String reason) {
        if (bidRequestInFlight) {
            logger.info("Clearing local BID request: reason={}, auctionId={}, bidderId={}, amount={}",
                    reason, pendingBidAuctionId, pendingBidderId, pendingBidAmount);
        }
        bidRequestInFlight = false;
        pendingBidAuctionId = null;
        pendingBidderId = null;
        pendingBidAmount = null;
    }

    private synchronized boolean isExpectedLocalBidResponse(JSONObject responseObj) {
        if (!bidRequestInFlight) {
            return false;
        }

        if (responseObj.has("auctionId") && !responseObj.isNull("auctionId")
                && responseObj.optInt("auctionId") != pendingBidAuctionId) {
            return false;
        }

        if (responseObj.has("bidderId") && !responseObj.isNull("bidderId")
                && responseObj.optInt("bidderId") != pendingBidderId) {
            return false;
        }

        Integer currentUserId = User.getId();
        if (currentUserId == null || pendingBidderId == null || !pendingBidderId.equals(currentUserId)) {
            return false;
        }

        if (responseObj.optBoolean("success", false)) {
            if (!responseObj.has("currentPrice")) {
                return false;
            }
            if (responseObj.has("highestBidderId") && !responseObj.isNull("highestBidderId")
                    && responseObj.optInt("highestBidderId") != currentUserId) {
                return false;
            }
        }

        return true;
    }

    private boolean isUserLoggedIn() {
        return User.getId() != null;
    }

    private boolean isSellerOfCurrentAuction() {
        return User.getId() != null && sellerId != null && sellerId.equals(User.getId());
    }

    private boolean isBiddingUnavailable() {
        return !auctionOpenForBidding || isSellerOfCurrentAuction();
    }

    private void updateBiddingAvailability() {
        boolean disabled = isBiddingUnavailable();
        if (bidAmountField != null) {
            bidAmountField.setDisable(disabled);
        }
        if (placeBidBtn != null) {
            placeBidBtn.setDisable(disabled);
        }
        if (btnAutoBid != null) {
            btnAutoBid.setDisable(disabled);
        }

        if (messageLabel != null && isSellerOfCurrentAuction()) {
            messageLabel.setStyle(INFO_STYLE);
            messageLabel.setText(OWN_AUCTION_BID_MESSAGE);
        } else if (messageLabel != null && OWN_AUCTION_BID_MESSAGE.equals(messageLabel.getText())) {
            messageLabel.setText("");
        }
    }

    private BigDecimal getValidBidAmount() {
        String input = bidAmountField.getText().trim();

        if (input.isEmpty()) {
            showError("Please enter a bid amount!");
            return null;
        }

        BigDecimal bidAmount;
        try {
            bidAmount = parseMoneyInput(input);
        } catch (NumberFormatException e) {
            showError("Bid amount must be a valid number!");
            return null;
        }

        if (bidAmount.compareTo(currentPrice) <= 0) {
            showError("Bid must be GREATER THAN current price (" + MONEY_PREFIX + formatPrice(currentPrice) + ")!");
            return null;
        }

        BigDecimal increment = getEffectiveStepPrice();
        BigDecimal minimumBid = currentPrice.add(increment);
        if (bidAmount.compareTo(minimumBid) < 0) {
            showError("Minimum bid is " + MONEY_PREFIX + formatPrice(minimumBid) + "!");
            return null;
        }

        return bidAmount;
    }

    private boolean isSocketReady() {
        return webSocket != null && !webSocket.isOutputClosed();
    }

    private boolean sendBidRequest(BigDecimal bidAmount) {
        JSONObject jsonBid = new JSONObject();
        jsonBid.put("auctionId", currentSessionId);
        jsonBid.put("bidderId", User.getId());
        jsonBid.put("amount", bidAmount);

        String payload = BID_PREFIX + jsonBid;
        logger.info("Sending BID request: {}", payload);

        if (!isSocketReady()) {
            logger.error("Failed to send BID request to server (socket not ready)!");
            Platform.runLater(() -> {
                finishBidProcessing();
                showError("Khong gui duoc yeu cau dat gia toi server");
                if (!bidErrorSoundPlayedForCurrentAttempt) {
                    com.auction.client.service.SoundManager.getInstance()
                            .playSound(com.auction.client.model.audio.SoundEvent.BID_ERROR);
                    bidErrorSoundPlayedForCurrentAttempt = true;
                }
            });
            return false;
        }

        try {
            webSocket.sendText(payload, true);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send BID request: {}", e.getMessage());
            Platform.runLater(() -> {
                finishBidProcessing();
                showError("Khong gui duoc yeu cau dat gia toi server");
            });
            return false;
        }
    }

    private void showBidProcessing() {
        bidErrorSoundPlayedForCurrentAttempt = false;
        placeBidBtn.setDisable(true);
        messageLabel.setStyle(WARNING_STYLE);
        messageLabel.setText(PROCESSING_MESSAGE);
        startBidTimeout();
    }

    private void startBidTimeout() {
        stopBidTimeout();

        logger.warn("Starting bid timeout for auctionId={}, bidderId={}, webSocketReady={}",
                currentSessionId, User.getId(), isSocketReady());

        bidTimeout = new Timeline(new KeyFrame(javafx.util.Duration.seconds(BID_TIMEOUT_SECONDS), event -> {
            placeBidBtn.setDisable(isBiddingUnavailable());

            if (PROCESSING_MESSAGE.equals(messageLabel.getText())) {
                messageLabel.setStyle(WARNING_STYLE);
                messageLabel.setText("No bid response from server. Auction data was refreshed; please try again.");
                clearLocalBidRequest("timeout");
                refreshSessionFromServer();
                fetchLatestUserBalance();
                reconnectBidSocket();
                if (!bidErrorSoundPlayedForCurrentAttempt) {
                    com.auction.client.service.SoundManager.getInstance()
                            .playSound(com.auction.client.model.audio.SoundEvent.BID_ERROR);
                    bidErrorSoundPlayedForCurrentAttempt = true;
                }
            }
        }));

        bidTimeout.setCycleCount(1);
        bidTimeout.play();
    }

    private void finishBidProcessing() {
        stopBidTimeout();

        if (placeBidBtn != null) {
            placeBidBtn.setDisable(isBiddingUnavailable());
        }
    }

    private void setSidebarExpanded(boolean expanded) {
        int width = expanded ? EXPANDED_SIDEBAR_WIDTH : COLLAPSED_SIDEBAR_WIDTH;
        sideBar.setPrefWidth(width);
        sideBar.setMaxWidth(width);

        setLabelsVisible(expanded,
                mainMenuLabel,
                dashboardText,
                liveAuctionsText,
                myBidsText,
                sellingText,
                discoverLabel,
                categoriesText,
                activeBidsText,
                watchlistText,
                endedSoonText,
                otherLabel,
                supportText,
                startSellingText);
    }

    private void setLabelsVisible(boolean visible, Label... labels) {
        for (Label label : labels) {
            if (label != null) {
                label.setVisible(visible);
                label.setManaged(visible);
            }
        }
    }

    private void updateRemainingTime(LocalDateTime endTime) {
        long secondsLeft = Duration.between(LocalDateTime.now(), endTime).getSeconds();

        if (secondsLeft <= 0) {
            stopTimeline();
            remainingTimeLabel.setText("Auction ended!");
            handleAuctionEnd();
            return;
        }

        long hours = secondsLeft / 3600;
        long minutes = (secondsLeft % 3600) / 60;
        long seconds = secondsLeft % 60;

        remainingTimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

        // ENDING_SOON logic (e.g., 5 minutes = 300 seconds)
        if (secondsLeft <= 300 && !endingSoonNotified) {
            endingSoonNotified = true;
            if (SettingsService.getInstance().isEndingSoonNotificationEnabled()) {
                AppNotification notif = new AppNotification(NotificationType.ENDING_SOON, NotificationSeverity.WARNING,
                        "Auction ending soon",
                        "The auction for " + productNameLabel.getText() + " is ending in 5 minutes!");
                notif.setAuctionId(currentSessionId);
                notif.setItemName(productNameLabel.getText());
                NotificationCenterService.getInstance().addNotification(notif);
            }
        }
    }

    private void handleAuctionEnd() {
        auctionOpenForBidding = false;
        updateBiddingAvailability();
        disconnectSocket();
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void stopBidTimeout() {
        if (bidTimeout != null) {
            bidTimeout.stop();
            bidTimeout = null;
        }
    }

    private BigDecimal parseMoneyInput(String raw) {
        return MoneyFormatUtil.parseMoneyInput(raw);
    }

    private BigDecimal getMoney(JSONObject object, String key, BigDecimal defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }

        try {
            return new BigDecimal(object.get(key).toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal getMoneyAny(JSONObject object, BigDecimal defaultValue, String... keys) {
        if (keys == null) {
            return defaultValue == null ? BigDecimal.ZERO : defaultValue;
        }

        for (String key : keys) {
            BigDecimal value = getMoney(object, key, null);
            if (value != null) {
                return value;
            }
        }

        return defaultValue == null ? BigDecimal.ZERO : defaultValue;
    }

    private Integer getOptionalInt(JSONObject object, String key) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return null;
        }

        return object.optInt(key);
    }

    private int getOptionalIntOrDefault(JSONObject object, String key, int defaultValue) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return defaultValue;
        }

        return object.optInt(key, defaultValue);
    }

    private int getBidCount(JSONObject object) {
        if (object == null) {
            return 0;
        }

        if (object.has("bidCount") && !object.isNull("bidCount")) {
            return object.optInt("bidCount", 0);
        }

        if (object.has("bids") && !object.isNull("bids")) {
            return object.optJSONArray("bids") == null ? 0 : object.optJSONArray("bids").length();
        }

        return 0;
    }

    private void updateDescription(String description) {
        if (description == null || description.isBlank()) {
            setLabelText(itemDescriptionLabel, DEFAULT_DESCRIPTION);
            return;
        }

        setLabelText(itemDescriptionLabel, description.trim());
    }

    private String buildImageUrl(String rawPath) {
        return ImageUrlUtil.buildImageUrl(rawPath);
    }


    private String extractUuid(String path) {
        if (path == null || path.isBlank())
            return null;
        java.util.regex.Pattern uuidPattern = java.util.regex.Pattern
                .compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        java.util.regex.Matcher matcher = uuidPattern.matcher(path);
        return matcher.find() ? matcher.group() : null;
    }

    private String getCloudName(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }
        try {
            String prefix = "cloudinary.com/";
            int index = cloudinaryUrl.indexOf(prefix);
            if (index >= 0) {
                String sub = cloudinaryUrl.substring(index + prefix.length());
                int slashIndex = sub.indexOf("/");
                if (slashIndex > 0) {
                    return sub.substring(0, slashIndex);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract Cloudinary cloud name: {}", e.getMessage());
        }
        return null;
    }

    private String resolveModelUrl(String imagePath) {
        String uuid = extractUuid(imagePath);
        if (uuid == null || uuid.isBlank()) {
            return null;
        }

        String cloudName = getCloudName(imagePath);
        String url;
        if (cloudName != null && !cloudName.isBlank()) {
            url = "https://res.cloudinary.com/" + cloudName + "/raw/upload/auction_system/items/models_3d/" + uuid + ".glb";
        } else {
            url = Config.API_URL + "/api/files/models-3d/" + uuid + "/" + uuid + ".glb";
        }
        return Config.applyCacheBuster(url);
    }

    private void reload3DModel() {
        if (model3DContainer == null)
            return;
        String modelUrl = resolveModelUrl(this.productImagePath);
        if (modelUrl == null || modelUrl.isBlank() || itemUuid == null || itemUuid.isBlank()) {
            return;
        }
        try {
            Path cachedFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".glb");
            if (Files.exists(cachedFile)) {
                logger.info("Reloading 3D model from updated cache: {}", cachedFile.toAbsolutePath());
                byte[] bytes = Files.readAllBytes(cachedFile);
                Node node3D = GltfImporterJFX.loadFromBytes(bytes, modelUrl);
                Platform.runLater(() -> {
                    model3DContainer.getChildren().clear();
                    model3DContainer.getChildren().add(node3D);
                });
            }
        } catch (Exception e) {
            logger.error("Failed to reload 3D model", e);
        }
    }

    private void check3DModelExists(String modelUrl) {
        if (modelUrl == null || modelUrl.isBlank() || itemUuid == null || itemUuid.isBlank()) {
            Platform.runLater(() -> {
                if (btnToggle3D != null) {
                    btnToggle3D.setVisible(false);
                    btnToggle3D.setManaged(false);
                }
            });
            return;
        }

        Path cachedFile = CacheManager.getCachedModel(modelUrl, itemUuid, () -> {
            Platform.runLater(() -> {
                if (btnToggle3D != null) {
                    btnToggle3D.setVisible(true);
                    btnToggle3D.setManaged(true);
                }
                if (is3DMode) {
                    reload3DModel();
                }
            });
        });

        if (cachedFile != null) {
            Platform.runLater(() -> {
                if (btnToggle3D != null) {
                    btnToggle3D.setVisible(true);
                    btnToggle3D.setManaged(true);
                }
            });
        } else {
            // Check remote availability to show the 3D button
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(3))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        boolean exists = response.statusCode() == 200;
                        Platform.runLater(() -> {
                            if (btnToggle3D != null) {
                                btnToggle3D.setVisible(exists);
                                btnToggle3D.setManaged(exists);
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            if (btnToggle3D != null) {
                                btnToggle3D.setVisible(false);
                                btnToggle3D.setManaged(false);
                            }
                        });
                        return null;
                    });
        }
    }

    @FXML
    private void handleToggle3D(ActionEvent event) {
        String modelUrl = resolveModelUrl(this.productImagePath);
        if (modelUrl == null || modelUrl.isBlank() || itemUuid == null || itemUuid.isBlank()) {
            return;
        }

        is3DMode = !is3DMode;

        if (is3DMode) {
            if (productImageView != null) {
                productImageView.setVisible(false);
                productImageView.setManaged(false);
            }
            if (model3DContainer != null) {
                model3DContainer.setVisible(true);
                model3DContainer.setManaged(true);

                if (model3DContainer.getChildren().isEmpty()) {
                    model3DContainer.getChildren().add(create3DLoadingNode());

                    CacheManager.getModelAsync(modelUrl, itemUuid).thenAccept(cachedFile -> {
                        if (cachedFile != null) {
                            // Parse GLB in background thread to avoid parsing lag on JavaFX thread
                            Thread parseThread = new Thread(() -> {
                                Node node3D;
                                try {
                                    byte[] bytes = Files.readAllBytes(cachedFile);
                                    node3D = GltfImporterJFX.loadFromBytes(bytes, modelUrl);
                                } catch (Exception e) {
                                    logger.error("Failed to parse 3D model", e);
                                    node3D = create3DMessageNode("3D model unavailable", "Cannot parse this GLB model.");
                                }
                                Node finalNode3D = node3D;
                                Platform.runLater(() -> model3DContainer.getChildren().setAll(finalNode3D));
                            });
                            parseThread.setDaemon(true);
                            parseThread.start();
                        } else {
                            Platform.runLater(() -> model3DContainer.getChildren().setAll(
                                create3DMessageNode("3D model unavailable", "Cannot load this GLB model.")
                            ));
                        }
                    });
                } else {
                    reset3DViewState();
                }
            }
            setToggle3DLabel(true);
        } else {
            if (model3DContainer != null) {
                model3DContainer.setVisible(false);
                model3DContainer.setManaged(false);
            }
            if (productImageView != null) {
                productImageView.setVisible(true);
                productImageView.setManaged(true);
            }
            setToggle3DLabel(false);
        }
    }

    private void reset3DViewState() {
        try {
            if (model3DContainer == null || model3DContainer.getChildren().isEmpty()) {
                return;
            }
            Node node3D = model3DContainer.getChildren().get(0);
            if (node3D instanceof javafx.scene.SubScene subScene) {
                javafx.scene.transform.Rotate rx = (javafx.scene.transform.Rotate) subScene.getProperties().get("rx");
                javafx.scene.transform.Rotate ry = (javafx.scene.transform.Rotate) subScene.getProperties().get("ry");
                javafx.scene.transform.Translate cameraTranslate =
                        (javafx.scene.transform.Translate) subScene.getProperties().get("cameraTranslate");
                if (rx != null) {
                    rx.setAngle(0);
                }
                if (ry != null) {
                    ry.setAngle(0);
                }
                if (cameraTranslate != null) {
                    cameraTranslate.setZ(-315);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to reset 3D view state", e);
        }
    }

    private Node create3DLoadingNode() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(54, 54);

        Label label = new Label("Loading 3D model...");
        label.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: -fx-accent;");

        VBox box = new VBox(12, indicator, label);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setMinSize(550, 400);
        box.setPrefSize(550, 400);
        box.setStyle("-fx-background-color: -app-surface-2; -fx-background-radius: 32;");
        return box;
    }

    private Node create3DMessageNode(String titleText, String detailText) {
        Label title = new Label(titleText);
        title.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: -app-text;");

        Label detail = new Label(detailText);
        detail.setWrapText(true);
        detail.setMaxWidth(360);
        detail.setAlignment(javafx.geometry.Pos.CENTER);
        detail.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -app-text-muted; -fx-text-alignment: center;");

        VBox box = new VBox(10, title, detail);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setMinSize(550, 400);
        box.setPrefSize(550, 400);
        box.setStyle("-fx-background-color: -app-surface-2; -fx-background-radius: 32;");
        return box;
    }

    private void setToggle3DLabel(boolean showing3D) {
        setToggle3DText(showing3D ? "\uE3F4" : "\uE0B4", showing3D ? "2D VIEW" : "3D VIEW");
    }

    private void setToggle3DText(String icon, String text) {
        if (btnToggle3D == null || !(btnToggle3D.getGraphic() instanceof javafx.scene.layout.HBox hbox)) {
            return;
        }
        if (hbox.getChildren().size() >= 2
                && hbox.getChildren().get(0) instanceof Label iconLabel
                && hbox.getChildren().get(1) instanceof Label textLabel) {
            iconLabel.setText(icon);
            textLabel.setText(text);
        }
    }

    private String formatPrice(BigDecimal price) {
        return MoneyFormatUtil.formatGrouped(price);
    }

    private String formatVnd(BigDecimal price) {
        return MoneyFormatUtil.formatVndSuffix(price);
    }

    private void showError(String message) {
        messageLabel.setStyle(ERROR_STYLE);
        messageLabel.setText(message);
    }

    private void showInfo(String message) {
        messageLabel.setStyle(INFO_STYLE);
        messageLabel.setText(message);
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    private void setNodeStyle(javafx.scene.Node node, String style) {
        if (node != null) {
            node.setStyle(style);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            logger.warn("Cannot close resource", e);
        }
    }


    private void updateTopBarAvatar(String avatarUrl) {
        if (topBarAvatarPane == null)
            return;
        try {
            topBarAvatarPane.getChildren().clear();
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : Config.API_URL + avatarUrl;
                fullUrl = Config.applyCacheBuster(fullUrl);
                ImageView imgView = new ImageView(CacheManager.getCachedImage(fullUrl, updatedImage -> {
                    if (topBarAvatarPane != null) {
                        ImageView updatedView = new ImageView(updatedImage);
                        updatedView.setFitWidth(36);
                        updatedView.setFitHeight(36);
                        updatedView.setSmooth(true);
                        updatedView.setClip(new javafx.scene.shape.Circle(18, 18, 18));
                        topBarAvatarPane.getChildren().setAll(updatedView);
                    }
                }));
                imgView.setFitWidth(36);
                imgView.setFitHeight(36);
                imgView.setSmooth(true);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                imgView.setClip(clip);
                topBarAvatarPane.getChildren().add(imgView);
            } else {
                Label icon = new Label("\uE7FD");
                icon.setStyle(
                        "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: white;");
                topBarAvatarPane.getChildren().add(icon);
            }
        } catch (Exception e) {
            logger.warn("Cannot update avatar on top bar: {}", e.getMessage());
        }
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
