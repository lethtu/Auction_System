package com.auction.client.controller;


import com.auction.client.util.AlertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.HttpClientSingleton;
import com.auction.client.model.User;
import com.auction.client.service.SettingsService;
import com.auction.client.util.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.beans.property.ReadOnlyObjectWrapper;

import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(SellerDashboardController.class);

    @FXML
    private StackPane modalOverlay;
    @FXML
    private VBox modalDialog;
    @FXML
    private Label modalTitle;
    @FXML
    private Button btnSubmit;
    private SessionItem editingSession = null;
    @FXML
    private ComboBox<String> productTypeCombo;
    @FXML
    private TextField productNameField;
    @FXML
    private TextField imageUrlField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField reservePriceField;
    @FXML
    private HBox errorReservePrice;
    @FXML
    private Label lblErrorReservePrice;
    @FXML
    private TextField startingPriceField;
    @FXML
    private VBox imageUploadArea;
    @FXML
    private Label lblImageFileName;
    @FXML
    private DatePicker datePickerStart;
    @FXML
    private TextField txtStartDay;
    @FXML
    private TextField txtStartMonth;
    @FXML
    private TextField txtStartYear;
    @FXML
    private TextField txtStartHour;
    @FXML
    private TextField txtStartMin;

    @FXML
    private DatePicker datePickerEnd;
    @FXML
    private TextField txtEndDay;
    @FXML
    private TextField txtEndMonth;
    @FXML
    private TextField txtEndYear;
    @FXML
    private TextField txtEndHour;
    @FXML
    private TextField txtEndMin;
    @FXML
    private HBox wrapperEndDT;
    @FXML
    private HBox errorEndDT;
    @FXML
    private Label lblErrorEndDT;
    @FXML
    private HBox wrapperStartDT;
    @FXML
    private HBox errorStartDT;
    @FXML
    private Label lblErrorStartDT;
    @FXML
    private HBox errorTitle;
    @FXML
    private HBox errorCategory;
    @FXML
    private HBox errorPrice;
    @FXML
    private Label lblErrorPrice;
    @FXML
    private Button btnDraftOrReset;

    @FXML
    private Label lblTotalRevenue;
    @FXML
    private Label lblActiveAuctions;
    @FXML
    private Label lblTotalBids;

    @FXML
    private TableView<SessionItem> sessionsTable;
    @FXML
    private TableColumn<SessionItem, SessionItem> colItem;
    @FXML
    private TableColumn<SessionItem, String> colStatus;
    @FXML
    private TableColumn<SessionItem, BigDecimal> colBid;
    @FXML
    private TableColumn<SessionItem, String> colTimeLeft;
    @FXML
    private TableColumn<SessionItem, SessionItem> colActions;

    @FXML
    private SidebarController sidebarController;
    @FXML
    private TopbarController topbarController;

    private HttpClient httpClient = HttpClientSingleton.getInstance().getHttpClient();
    final List<SessionItem> allSessions = new ArrayList<>();
    final ObservableList<SessionItem> displayedSessions = FXCollections.observableArrayList();

    public void setHttpClient(HttpClient client) {
        this.httpClient = client;
    }

    @FXML
    public void initialize() {
        productTypeCombo.setItems(FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"));
        productTypeCombo.setValue("Electronics");

        setupTable();
        setupImageUpload();
        setupSplitDatetimePickers();

        if (topbarController != null) {
            topbarController.setSearchVisible(false);
            if (sidebarController != null) {
                topbarController.setSidebarController(sidebarController);
            }
        }

        if (modalDialog != null && modalOverlay != null) {
            modalOverlay.heightProperty().addListener((obs, oldVal, newVal) -> updateModalMaxHeight());
            modalDialog.widthProperty().addListener((obs, oldVal, newVal) -> updateModalMaxHeight());
        }
        javafx.application.Platform.runLater(() -> {
            if (sidebarController != null) {
                sidebarController.setActiveSelling();
            }
        });

        if (sidebarController != null) {
            sidebarController.setSidebarListener(new SidebarController.SidebarListener() {
                @Override
                public void onFilterWatchlist(javafx.event.ActionEvent event) {
                    try {
                        MainController.initialShowWatchlist = true;
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Navigation error:", e);
                    }
                }

                @Override
                public void onFilterMyBids(javafx.event.ActionEvent event) {
                    try {
                        MainController.initialHomeFilterMode = "MY_BIDS";
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Navigation error to My Bids:", e);
                    }
                }

                @Override
                public void onResetFilter(javafx.event.ActionEvent event) {
                    try {
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Navigation error:", e);
                    }
                }
            });
        }

        loadMySessions();
    }

    private void setupSplitDatetimePickers() {
        // Auto-jump day -> month -> year -> hour -> min
        setupAutoJump(txtStartDay, txtStartMonth, 2);
        setupAutoJump(txtStartMonth, txtStartYear, 2);
        setupAutoJump(txtStartYear, txtStartHour, 4);
        setupAutoJump(txtStartHour, txtStartMin, 2);

        setupAutoJump(txtEndDay, txtEndMonth, 2);
        setupAutoJump(txtEndMonth, txtEndYear, 2);
        setupAutoJump(txtEndYear, txtEndHour, 4);
        setupAutoJump(txtEndHour, txtEndMin, 2);

        // Auto sync from DatePicker value
        datePickerStart.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtStartDay.setText(String.format("%02d", newVal.getDayOfMonth()));
                txtStartMonth.setText(String.format("%02d", newVal.getMonthValue()));
                txtStartYear.setText(String.format("%04d", newVal.getYear()));
            }
        });

        datePickerEnd.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtEndDay.setText(String.format("%02d", newVal.getDayOfMonth()));
                txtEndMonth.setText(String.format("%02d", newVal.getMonthValue()));
                txtEndYear.setText(String.format("%04d", newVal.getYear()));
            }
        });
    }

    private void setupAutoJump(TextField source, TextField target, int length) {
        if (source == null || target == null)
            return;
        source.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() >= length) {
                target.requestFocus();
            }
        });
    }

    private void setupImageUpload() {
        if (imageUploadArea == null)
            return;

        imageUploadArea.setOnDragOver(event -> {
            if (event.getGestureSource() != imageUploadArea && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                imageUploadArea.setStyle(
                        "-fx-border-color: -fx-accent; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 32px; -fx-cursor: hand; -fx-background-color: #ffd6ee;");
            }
            event.consume();
        });

        imageUploadArea.setOnDragExited(event -> {
            imageUploadArea.setStyle(
                    "-fx-border-color: #dcc8e0; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 32px; -fx-cursor: hand; -fx-background-color: transparent;");
            event.consume();
        });

        imageUploadArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                imageUrlField.setText(file.getAbsolutePath());
                lblImageFileName.setText(file.getName());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        imageUploadArea.setOnMouseClicked(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Product Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp",
                            "*.bmp"));
            File selectedFile = fileChooser.showOpenDialog(imageUploadArea.getScene().getWindow());
            if (selectedFile != null) {
                imageUrlField.setText(selectedFile.getAbsolutePath());
                lblImageFileName.setText(selectedFile.getName());
            }
        });
    }

    private boolean isDarkThemeActive() {
        return SettingsService.getInstance().getTheme().toLowerCase(java.util.Locale.ROOT).contains("dark");
    }

    private String sellerPrimaryTextHex() {
        return isDarkThemeActive() ? "#f0e6f8" : "#2e1a28";
    }

    private String sellerMutedTextHex() {
        return isDarkThemeActive() ? "#b8a8c8" : "#604868";
    }

    private String sellerAccentHex() {
        String color = SettingsService.getInstance().getPrimaryColor();
        if (color == null) return "#e040a0";
        String normalized = color.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("purple")) return "#8b5cf6";
        if (normalized.contains("emerald") || normalized.contains("green")) return "#10b981";
        if (normalized.contains("blue")) return "#3b82f6";
        if (normalized.contains("orange")) return "#f97316";
        return "#e040a0";
    }

    private String sellerImageCellStyle() {
        if (isDarkThemeActive()) {
            return "-fx-background-color: #2a2035; -fx-background-radius: 24px; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 24px;";
        }
        return "-fx-background-color: #f2e8f2; -fx-background-radius: 24px;";
    }

    private void setupTable() {
        sessionsTable.setItems(displayedSessions);

        colItem.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        colItem.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(SessionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox hbox = new HBox(12);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    // Image placeholder
                    StackPane imgContainer = new StackPane();
                    imgContainer.setPrefSize(48, 48);
                    imgContainer.setMinSize(48, 48);
                    imgContainer.setMaxSize(48, 48);
                    imgContainer.setStyle(sellerImageCellStyle());

                    if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                        try {
                            String tableImageUrl = buildSellerImageUrl(item.imageUrl);
                            ImageView iv = new ImageView(new Image(tableImageUrl, 48, 48, true, true));
                            Circle clip = new Circle(24, 24, 24);
                            iv.setClip(clip);
                            imgContainer.getChildren().add(iv);
                        } catch (Exception e) {
                            FontIcon icon = new FontIcon("mdi2i-image-outline");
                            icon.setIconSize(24);
                            icon.setIconColor(Color.valueOf(sellerMutedTextHex()));
                            imgContainer.getChildren().add(icon);
                        }
                    } else {
                        FontIcon icon = new FontIcon("mdi2i-image-outline");
                        icon.setIconSize(24);
                        icon.setIconColor(Color.valueOf(sellerMutedTextHex()));
                        imgContainer.getChildren().add(icon);
                    }

                    VBox vbox = new VBox(2);
                    vbox.setAlignment(Pos.CENTER_LEFT);
                    Label lblName = new Label(item.productName);
                    lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: " + sellerPrimaryTextHex() + ";");
                    Label lblId = new Label("ID: #AUC-" + item.id);
                    lblId.setStyle("-fx-font-size: 11px; -fx-text-fill: " + sellerMutedTextHex() + ";");

                    vbox.getChildren().addAll(lblName, lblId);
                    hbox.getChildren().addAll(imgContainer, vbox);
                    setGraphic(hbox);
                }
            }
        });

        colStatus.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().status));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                } else {
                    Label lblStatus = new Label(status.toUpperCase());
                    lblStatus.getStyleClass().add("badge");

                    switch (status.toUpperCase()) {
                        case "ACTIVE", "LIVE" -> lblStatus.getStyleClass().add("badge-live");
                        case "DRAFT" -> lblStatus.getStyleClass().add("badge-draft");
                        case "COMPLETED", "ENDED" -> lblStatus.getStyleClass().add("badge-ended");
                        case "COMING" -> {
                            lblStatus.setStyle(
                                    "-fx-background-color: #f3e8ff; -fx-text-fill: #7c52aa; -fx-border-color: #dcc8e0; -fx-border-radius: 12px; -fx-background-radius: 12px;");
                        }
                        case "CANCELED" -> {
                            lblStatus.setStyle("-fx-background-color: #ffe8e8; -fx-text-fill: #e53e3e;");
                        }
                        default -> lblStatus.setStyle("-fx-background-color: #f2e8f2; -fx-text-fill: -app-text-muted;");
                    }

                    StackPane wrapper = new StackPane(lblStatus);
                    wrapper.setMaxWidth(Double.MAX_VALUE);
                    wrapper.prefWidthProperty().bind(widthProperty().subtract(20));
                    wrapper.setAlignment(Pos.CENTER);

                    setText(null);
                    setGraphic(wrapper);
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-alignment: CENTER; -fx-padding: 0 10 0 10;");
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        colBid.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().currentPrice));
        colBid.setCellFactory(col -> new TableCell<>() {
            private final DecimalFormat df = new DecimalFormat("#,##0.##");

            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                setText(null);
                setAlignment(Pos.CENTER);
                setStyle("-fx-alignment: CENTER; -fx-padding: 0 10 0 10;");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                if (empty || price == null) {
                    setGraphic(null);
                    return;
                }

                Label priceLabel = new Label(price.compareTo(BigDecimal.ZERO) == 0 ? "--" : "$" + df.format(price));
                priceLabel.setStyle(price.compareTo(BigDecimal.ZERO) == 0
                        ? "-fx-text-fill: " + sellerMutedTextHex() + "; -fx-font-weight: bold;"
                        : "-fx-text-fill: -fx-accent; -fx-font-weight: 900;");

                StackPane wrapper = new StackPane(priceLabel);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                wrapper.prefWidthProperty().bind(widthProperty().subtract(20));
                wrapper.setAlignment(Pos.CENTER);
                setGraphic(wrapper);
            }
        });

        colTimeLeft.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().endTime));
        colTimeLeft.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String endTime, boolean empty) {
                super.updateItem(endTime, empty);
                setText(null);
                setAlignment(Pos.CENTER);
                setStyle("-fx-alignment: CENTER; -fx-padding: 0 10 0 10;");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                if (empty || endTime == null || endTime.isEmpty()) {
                    setGraphic(null);
                    return;
                }

                String displayText;
                String textColor = sellerMutedTextHex();
                try {
                    LocalDateTime end = LocalDateTime.parse(endTime);
                    if (end.isBefore(LocalDateTime.now())) {
                        displayText = "Ended";
                        textColor = "#e53e3e";
                    } else {
                        Duration d = Duration.between(LocalDateTime.now(), end);
                        long hours = d.toHours();
                        long minutes = d.toMinutesPart();
                        displayText = String.format("%02dh %02dm", hours, minutes);
                    }
                } catch (Exception e) {
                    displayText = endTime;
                }

                Label timeLabel = new Label(displayText);
                timeLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold;");

                StackPane wrapper = new StackPane(timeLabel);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                wrapper.prefWidthProperty().bind(widthProperty().subtract(20));
                wrapper.setAlignment(Pos.CENTER);
                setGraphic(wrapper);
            }
        });

        colActions.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(SessionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                } else {
                    HBox hbox = new HBox(8);
                    hbox.setAlignment(Pos.CENTER);

                    Button btnView;
                    if ("DRAFT".equalsIgnoreCase(item.status)) {
                        btnView = createIconButton("mdi2p-publish", sellerAccentHex());
                        btnView.setTooltip(new javafx.scene.control.Tooltip("Quick Sale"));
                        btnView.setOnAction(e -> handleQuickPublish(item));
                    } else {
                        btnView = createIconButton("mdi2e-eye", sellerAccentHex());
                        btnView.setTooltip(new javafx.scene.control.Tooltip("View Details"));
                        btnView.setOnAction(e -> handleViewSession(item, e));
                    }
                    Button btnEdit = createIconButton("mdi2p-pencil", "#7c52aa");
                    Button btnDelete = createIconButton("mdi2d-delete", "#e53e3e");

                    btnView.setId("btnView_" + item.id);
                    btnEdit.setId("btnEdit_" + item.id);
                    btnDelete.setId("btnDelete_" + item.id);

                    btnDelete.setOnAction(e -> handleCancelSpecificSession(item));
                    btnEdit.setOnAction(e -> handleShowEditModal(item));

                    // Delete is allowed for ACTIVE, COMING or DRAFT sessions
                    boolean isDeletable = "ACTIVE".equalsIgnoreCase(item.status)
                            || "COMING".equalsIgnoreCase(item.status) || "DRAFT".equalsIgnoreCase(item.status);
                    if (!isDeletable) {
                        btnDelete.setDisable(true);
                        btnDelete.setOpacity(0.3);
                    }
                    // Edit is allowed for ACTIVE, COMING or DRAFT sessions
                    boolean isEditable = "ACTIVE".equalsIgnoreCase(item.status)
                            || "COMING".equalsIgnoreCase(item.status) || "DRAFT".equalsIgnoreCase(item.status);
                    if (!isEditable) {
                        btnEdit.setDisable(true);
                        btnEdit.setOpacity(0.3);
                    }

                    hbox.getChildren().addAll(btnView, btnEdit, btnDelete);

                    StackPane wrapper = new StackPane(hbox);
                    wrapper.setMaxWidth(Double.MAX_VALUE);
                    wrapper.prefWidthProperty().bind(widthProperty().subtract(20));
                    wrapper.setAlignment(Pos.CENTER);

                    setText(null);
                    setGraphic(wrapper);
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-alignment: CENTER; -fx-padding: 0 10 0 10;");
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });
    }

    private Button createIconButton(String iconLiteral, String color) {
        Button btn = new Button();
        btn.getStyleClass().add("action-btn");
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(20);
        icon.setIconColor(Color.valueOf(color));
        btn.setGraphic(icon);
        return btn;
    }

    @FXML
    private void handleShowAddModal() {
        editingSession = null;
        if (modalTitle != null) {
            modalTitle.setText("Add New Listing");
        }
        if (btnSubmit != null) {
            btnSubmit.setText("Launch Auction");
        }
        if (btnDraftOrReset != null) {
            btnDraftOrReset.setText("Save as Draft");
        }
        clearForm();
        modalOverlay.setVisible(true);
    }

    @FXML
    void handleCloseModal() {
        modalOverlay.setVisible(false);
    }

    void handleShowEditModal(SessionItem item) {
        if (item == null)
            return;
        editingSession = item;

        if (modalTitle != null) {
            modalTitle.setText("Edit Listing");
        }
        boolean isActive = item.status != null
                && ("ACTIVE".equalsIgnoreCase(item.status) || "LIVE".equalsIgnoreCase(item.status));
        boolean isActiveOrComing = isActive || "COMING".equalsIgnoreCase(item.status);
        if (isActiveOrComing) {
            if (btnSubmit != null) {
                btnSubmit.setText("Save Changes");
            }
            if (btnDraftOrReset != null) {
                btnDraftOrReset.setText("Reset");
            }
        } else {
            if (btnSubmit != null) {
                btnSubmit.setText("Publish");
            }
            if (btnDraftOrReset != null) {
                btnDraftOrReset.setText("Save Changes");
            }
        }

        if (startingPriceField != null)
            startingPriceField.setDisable(isActive);
        if (txtStartDay != null)
            txtStartDay.setDisable(isActive);
        if (txtStartMonth != null)
            txtStartMonth.setDisable(isActive);
        if (txtStartYear != null)
            txtStartYear.setDisable(isActive);
        if (txtStartHour != null)
            txtStartHour.setDisable(isActive);
        if (txtStartMin != null)
            txtStartMin.setDisable(isActive);

        // Populate the form fields
        productNameField.setText(item.productName);
        productTypeCombo.setValue(item.productType);
        imageUrlField.setText(item.imageUrl);
        if (lblImageFileName != null) {
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                try {
                    File file = new File(new URI(item.imageUrl));
                    lblImageFileName.setText(file.getName());
                } catch (Exception e) {
                    try {
                        File file = new File(item.imageUrl);
                        lblImageFileName.setText(file.getName());
                    } catch (Exception ex) {
                        lblImageFileName.setText("Image Loaded");
                    }
                }
            } else {
                lblImageFileName.setText("");
            }
        }
        descriptionArea.setText(item.description);
        startingPriceField.setText(item.startingPrice != null ? item.startingPrice.toString() : "0");
        if (reservePriceField != null) {
            reservePriceField.setText(item.reservePrice != null ? item.reservePrice.toString() : "");
        }

        // Populate Date/Time fields
        populateSplitTimeFields(item.startTime, txtStartDay, txtStartMonth, txtStartYear, txtStartHour, txtStartMin);
        populateSplitTimeFields(item.endTime, txtEndDay, txtEndMonth, txtEndYear, txtEndHour, txtEndMin);

        modalOverlay.setVisible(true);
    }

    private void populateSplitTimeFields(String timeStr, TextField day, TextField month, TextField year, TextField hour,
            TextField min) {
        if (day == null || month == null || year == null || hour == null || min == null)
            return;
        if (timeStr == null || timeStr.trim().isEmpty() || "null".equalsIgnoreCase(timeStr.trim())) {
            day.clear();
            month.clear();
            year.clear();
            hour.clear();
            min.clear();
            return;
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(timeStr.trim());
            day.setText(String.format("%02d", dt.getDayOfMonth()));
            month.setText(String.format("%02d", dt.getMonthValue()));
            year.setText(String.format("%04d", dt.getYear()));
            hour.setText(String.format("%02d", dt.getHour()));
            min.setText(String.format("%02d", dt.getMinute()));
        } catch (Exception e) {
            logger.error("Error parsing date: " + timeStr, e);
            day.clear();
            month.clear();
            year.clear();
            hour.clear();
            min.clear();
        }
    }

    @FXML
    private void handleSaveAction() {
        if (editingSession == null) {
            handleCreateSession();
        } else {
            handleUpdateSession();
        }
    }

    @FXML
    private void handleSaveDraftAction() {
        if (editingSession != null) {
            boolean isActiveOrComing = editingSession.status != null
                    && ("ACTIVE".equalsIgnoreCase(editingSession.status)
                            || "LIVE".equalsIgnoreCase(editingSession.status)
                            || "COMING".equalsIgnoreCase(editingSession.status));
            if (isActiveOrComing) {
                // Clear any inline warning/error styles first
                if (errorTitle != null) {
                    errorTitle.setVisible(false);
                    errorTitle.setManaged(false);
                }
                if (productNameField != null) {
                    productNameField.getStyleClass().remove("error-text-input");
                }
                if (errorPrice != null) {
                    errorPrice.setVisible(false);
                    errorPrice.setManaged(false);
                }
                if (startingPriceField != null) {
                    startingPriceField.getStyleClass().remove("error-text-input");
                }
                if (errorEndDT != null) {
                    errorEndDT.setVisible(false);
                    errorEndDT.setManaged(false);
                }
                if (wrapperEndDT != null) {
                    wrapperEndDT.getStyleClass().remove("error-segmented-input");
                }

                // Reset the fields back to original editingSession values
                handleShowEditModal(editingSession);
                return;
            }
        }

        if (editingSession == null) {
            handleCreateDraftSession();
        } else {
            handleUpdateDraftSession();
        }
    }

    private void handleCreateDraftSession() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Could not get sellerId from session");
            showAlert(Alert.AlertType.ERROR, "Error", "Could not get sellerId from session.");
            return;
        }

        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String existingUuid = (editingSession != null) ? extractUuid(editingSession.imageUrl) : null;
        String imageUrl = prepareImagePathForSave(productNameOrEmpty(imageUrlField), existingUuid);
        if (imageUrl == null) {
            return;
        }
        String description = productNameOrEmpty(descriptionArea);
        String startingPriceText = productNameOrEmpty(startingPriceField);

        boolean formIsValid = true;

        // 1. Title Validation
        if (productName.isEmpty()) {
            formIsValid = false;
            if (errorTitle != null) {
                errorTitle.setVisible(true);
                errorTitle.setManaged(true);
            }
            if (productNameField != null && !productNameField.getStyleClass().contains("error-text-input")) {
                productNameField.getStyleClass().add("error-text-input");
            }
        } else {
            if (errorTitle != null) {
                errorTitle.setVisible(false);
                errorTitle.setManaged(false);
            }
            if (productNameField != null) {
                productNameField.getStyleClass().remove("error-text-input");
            }
        }

        // 2. Category Validation
        if (productType == null) {
            formIsValid = false;
            if (errorCategory != null) {
                errorCategory.setVisible(true);
                errorCategory.setManaged(true);
            }
            if (productTypeCombo != null && !productTypeCombo.getStyleClass().contains("error-text-input")) {
                productTypeCombo.getStyleClass().add("error-text-input");
            }
        } else {
            if (errorCategory != null) {
                errorCategory.setVisible(false);
                errorCategory.setManaged(false);
            }
            if (productTypeCombo != null) {
                productTypeCombo.getStyleClass().remove("error-text-input");
            }
        }

        // 3. Price Validation
        BigDecimal startingPrice = BigDecimal.ZERO;
        if (startingPriceText.isEmpty()) {
            formIsValid = false;
            if (errorPrice != null) {
                errorPrice.setVisible(true);
                errorPrice.setManaged(true);
            }
            if (lblErrorPrice != null) {
                lblErrorPrice.setText("Starting price cannot be empty");
            }
            if (startingPriceField != null && !startingPriceField.getStyleClass().contains("error-text-input")) {
                startingPriceField.getStyleClass().add("error-text-input");
            }
        } else {
            try {
                startingPrice = new BigDecimal(startingPriceText.trim());
                if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException();
                }
                if (errorPrice != null) {
                    errorPrice.setVisible(false);
                    errorPrice.setManaged(false);
                }
                if (startingPriceField != null) {
                    startingPriceField.getStyleClass().remove("error-text-input");
                }
            } catch (Exception e) {
                formIsValid = false;
                if (errorPrice != null) {
                    errorPrice.setVisible(true);
                    errorPrice.setManaged(true);
                }
                if (lblErrorPrice != null) {
                    lblErrorPrice.setText("Starting price must be a positive number");
                }
                if (startingPriceField != null && !startingPriceField.getStyleClass().contains("error-text-input")) {
                    startingPriceField.getStyleClass().add("error-text-input");
                }
            }
        }

        // 4. Time Validation
        LocalDateTime startDT = getLocalDateTimeFromSplitFields(txtStartDay, txtStartMonth, txtStartYear, txtStartHour,
                txtStartMin);
        LocalDateTime endDT = getLocalDateTimeFromSplitFields(txtEndDay, txtEndMonth, txtEndYear, txtEndHour,
                txtEndMin);

        // Validate Start Time
        if (startDT == null) {
            formIsValid = false;
            if (errorStartDT != null) {
                errorStartDT.setVisible(true);
                errorStartDT.setManaged(true);
            }
            if (wrapperStartDT != null && !wrapperStartDT.getStyleClass().contains("error-segmented-input")) {
                wrapperStartDT.getStyleClass().add("error-segmented-input");
            }
            if (lblErrorStartDT != null) {
                lblErrorStartDT.setText("Please enter a valid start date and time!");
            }
        } else {
            if (errorStartDT != null) {
                errorStartDT.setVisible(false);
                errorStartDT.setManaged(false);
            }
            if (wrapperStartDT != null) {
                wrapperStartDT.getStyleClass().remove("error-segmented-input");
            }
        }

        // Validate End Time
        if (endDT == null) {
            formIsValid = false;
            if (errorEndDT != null) {
                errorEndDT.setVisible(true);
                errorEndDT.setManaged(true);
            }
            if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                wrapperEndDT.getStyleClass().add("error-segmented-input");
            }
            if (lblErrorEndDT != null) {
                lblErrorEndDT.setText("Please enter a valid end date and time!");
            }
        } else {
            if (startDT != null) {
                boolean isEndFuture = endDT.isAfter(LocalDateTime.now());
                boolean isEndAfterStart = endDT.isAfter(startDT);

                if (!isEndFuture || !isEndAfterStart) {
                    formIsValid = false;
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(true);
                        errorEndDT.setManaged(true);
                    }
                    if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                        wrapperEndDT.getStyleClass().add("error-segmented-input");
                    }
                    if (lblErrorEndDT != null) {
                        if (!isEndFuture) {
                            lblErrorEndDT.setText("End time must be in the future");
                        } else {
                            lblErrorEndDT.setText("End time must be after start time");
                        }
                    }
                } else {
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(false);
                        errorEndDT.setManaged(false);
                    }
                    if (wrapperEndDT != null) {
                        wrapperEndDT.getStyleClass().remove("error-segmented-input");
                    }
                }
            } else {
                boolean isEndFuture = endDT.isAfter(LocalDateTime.now());
                if (!isEndFuture) {
                    formIsValid = false;
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(true);
                        errorEndDT.setManaged(true);
                    }
                    if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                        wrapperEndDT.getStyleClass().add("error-segmented-input");
                    }
                    if (lblErrorEndDT != null) {
                        lblErrorEndDT.setText("End time must be in the future");
                    }
                } else {
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(false);
                        errorEndDT.setManaged(false);
                    }
                    if (wrapperEndDT != null) {
                        wrapperEndDT.getStyleClass().remove("error-segmented-input");
                    }
                }
            }
        }

        if (!formIsValid) {
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("name", productName);
            body.put("type", productType);
            body.put("imagePath", imageUrl);
            body.put("description", description);
            body.put("startingPrice", startingPrice);

            BigDecimal reservePrice = BigDecimal.ZERO;
            String reservePriceText = reservePriceField != null ? reservePriceField.getText() : "";
            if (reservePriceText != null && !reservePriceText.trim().isEmpty()) {
                try {
                    reservePrice = new BigDecimal(reservePriceText.trim());
                } catch (Exception e) {
                }
            }
            body.put("reservePrice", reservePrice);

            body.put("sellerId", sellerId);
            body.put("stepPrice", 10000); // Default step price
            body.put("status", "DRAFT");

            if (startDT == null) {
                body.put("startTime", JSONObject.NULL);
            } else {
                body.put("startTime", startDT.toString());
            }

            if (endDT == null) {
                body.put("endTime", JSONObject.NULL);
            } else {
                body.put("endTime", endDT.toString());
            }

            HttpRequest request = newRequestBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/create-auction"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(), "Draft saved successfully.");

            if (api.success) {
                clearForm();
                handleCloseModal();
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Success", api.message);
            } else {
                logger.error("Error api: {}", api.message);
                showAlert(Alert.AlertType.ERROR, "Error", api.message);
            }

        } catch (Exception e) {
            logger.error("Cannot connect to server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot connect to the server!");
        }
    }

    private void handleUpdateDraftSession() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Could not get sellerId from session");
            showAlert(Alert.AlertType.ERROR, "Error", "Could not get sellerId from session.");
            return;
        }

        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String existingUuid = (editingSession != null) ? extractUuid(editingSession.imageUrl) : null;
        String imageUrl = prepareImagePathForSave(productNameOrEmpty(imageUrlField), existingUuid);
        if (imageUrl == null) {
            return;
        }
        String description = productNameOrEmpty(descriptionArea);
        String startingPriceText = productNameOrEmpty(startingPriceField);

        boolean formIsValid = true;

        // 1. Title Validation
        if (productName.isEmpty()) {
            formIsValid = false;
            if (errorTitle != null) {
                errorTitle.setVisible(true);
                errorTitle.setManaged(true);
            }
            if (productNameField != null && !productNameField.getStyleClass().contains("error-text-input")) {
                productNameField.getStyleClass().add("error-text-input");
            }
        } else {
            if (errorTitle != null) {
                errorTitle.setVisible(false);
                errorTitle.setManaged(false);
            }
            if (productNameField != null) {
                productNameField.getStyleClass().remove("error-text-input");
            }
        }

        // 2. Category Validation
        if (productType == null) {
            formIsValid = false;
            if (errorCategory != null) {
                errorCategory.setVisible(true);
                errorCategory.setManaged(true);
            }
            if (productTypeCombo != null && !productTypeCombo.getStyleClass().contains("error-text-input")) {
                productTypeCombo.getStyleClass().add("error-text-input");
            }
        } else {
            if (errorCategory != null) {
                errorCategory.setVisible(false);
                errorCategory.setManaged(false);
            }
            if (productTypeCombo != null) {
                productTypeCombo.getStyleClass().remove("error-text-input");
            }
        }

        // 3. Price Validation
        BigDecimal startingPrice = BigDecimal.ZERO;
        if (startingPriceText.isEmpty()) {
            formIsValid = false;
            if (errorPrice != null) {
                errorPrice.setVisible(true);
                errorPrice.setManaged(true);
            }
            if (lblErrorPrice != null) {
                lblErrorPrice.setText("Starting price cannot be empty");
            }
            if (startingPriceField != null && !startingPriceField.getStyleClass().contains("error-text-input")) {
                startingPriceField.getStyleClass().add("error-text-input");
            }
        } else {
            try {
                startingPrice = new BigDecimal(startingPriceText.trim());
                if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException();
                }
                if (errorPrice != null) {
                    errorPrice.setVisible(false);
                    errorPrice.setManaged(false);
                }
                if (startingPriceField != null) {
                    startingPriceField.getStyleClass().remove("error-text-input");
                }
            } catch (Exception e) {
                formIsValid = false;
                if (errorPrice != null) {
                    errorPrice.setVisible(true);
                    errorPrice.setManaged(true);
                }
                if (lblErrorPrice != null) {
                    lblErrorPrice.setText("Starting price must be a positive number");
                }
                if (startingPriceField != null && !startingPriceField.getStyleClass().contains("error-text-input")) {
                    startingPriceField.getStyleClass().add("error-text-input");
                }
            }
        }

        // 4. Time Validation
        LocalDateTime startDT = getLocalDateTimeFromSplitFields(txtStartDay, txtStartMonth, txtStartYear, txtStartHour,
                txtStartMin);
        LocalDateTime endDT = getLocalDateTimeFromSplitFields(txtEndDay, txtEndMonth, txtEndYear, txtEndHour,
                txtEndMin);

        // Validate Start Time
        if (startDT == null) {
            formIsValid = false;
            if (errorStartDT != null) {
                errorStartDT.setVisible(true);
                errorStartDT.setManaged(true);
            }
            if (wrapperStartDT != null && !wrapperStartDT.getStyleClass().contains("error-segmented-input")) {
                wrapperStartDT.getStyleClass().add("error-segmented-input");
            }
            if (lblErrorStartDT != null) {
                lblErrorStartDT.setText("Please enter a valid start date and time!");
            }
        } else {
            if (errorStartDT != null) {
                errorStartDT.setVisible(false);
                errorStartDT.setManaged(false);
            }
            if (wrapperStartDT != null) {
                wrapperStartDT.getStyleClass().remove("error-segmented-input");
            }
        }

        // Validate End Time
        if (endDT == null) {
            formIsValid = false;
            if (errorEndDT != null) {
                errorEndDT.setVisible(true);
                errorEndDT.setManaged(true);
            }
            if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                wrapperEndDT.getStyleClass().add("error-segmented-input");
            }
            if (lblErrorEndDT != null) {
                lblErrorEndDT.setText("Please enter a valid end date and time!");
            }
        } else {
            if (startDT != null) {
                boolean isEndFuture = endDT.isAfter(LocalDateTime.now());
                boolean isEndAfterStart = endDT.isAfter(startDT);

                if (!isEndFuture || !isEndAfterStart) {
                    formIsValid = false;
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(true);
                        errorEndDT.setManaged(true);
                    }
                    if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                        wrapperEndDT.getStyleClass().add("error-segmented-input");
                    }
                    if (lblErrorEndDT != null) {
                        if (!isEndFuture) {
                            lblErrorEndDT.setText("End time must be in the future");
                        } else {
                            lblErrorEndDT.setText("End time must be after start time");
                        }
                    }
                } else {
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(false);
                        errorEndDT.setManaged(false);
                    }
                    if (wrapperEndDT != null) {
                        wrapperEndDT.getStyleClass().remove("error-segmented-input");
                    }
                }
            } else {
                boolean isEndFuture = endDT.isAfter(LocalDateTime.now());
                if (!isEndFuture) {
                    formIsValid = false;
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(true);
                        errorEndDT.setManaged(true);
                    }
                    if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                        wrapperEndDT.getStyleClass().add("error-segmented-input");
                    }
                    if (lblErrorEndDT != null) {
                        lblErrorEndDT.setText("End time must be in the future");
                    }
                } else {
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(false);
                        errorEndDT.setManaged(false);
                    }
                    if (wrapperEndDT != null) {
                        wrapperEndDT.getStyleClass().remove("error-segmented-input");
                    }
                }
            }
        }

        if (!formIsValid) {
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("name", productName);
            body.put("type", productType);
            body.put("imagePath", imageUrl);
            body.put("description", description);
            body.put("startingPrice", startingPrice);

            BigDecimal reservePrice = BigDecimal.ZERO;
            String reservePriceText = reservePriceField != null ? reservePriceField.getText() : "";
            if (reservePriceText != null && !reservePriceText.trim().isEmpty()) {
                try {
                    reservePrice = new BigDecimal(reservePriceText.trim());
                } catch (Exception e) {
                }
            }
            body.put("reservePrice", reservePrice);

            body.put("sellerId", sellerId);
            body.put("stepPrice", 10000); // Default step price
            body.put("status", "DRAFT");

            if (startDT == null) {
                body.put("startTime", JSONObject.NULL);
            } else {
                body.put("startTime", startDT.toString());
            }

            if (endDT == null) {
                body.put("endTime", JSONObject.NULL);
            } else {
                body.put("endTime", endDT.toString());
            }

            HttpRequest request = newRequestBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/update-session/" + editingSession.id + "?sellerId="
                            + sellerId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(), "Draft updated successfully.");

            if (api.success) {
                clearForm();
                handleCloseModal();
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Success", api.message);
            } else {
                logger.error("Error api: {}", api.message);
                showAlert(Alert.AlertType.ERROR, "Error", api.message);
            }

        } catch (Exception e) {
            logger.error("Cannot connect to server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot connect to the server!");
        }
    }

    private void handleUpdateSession() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Could not get sellerId from session");
            showAlert(Alert.AlertType.ERROR, "Error", "Could not get sellerId from session.");
            return;
        }

        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String existingUuid = (editingSession != null) ? extractUuid(editingSession.imageUrl) : null;
        String imageUrl = prepareImagePathForSave(productNameOrEmpty(imageUrlField), existingUuid);
        if (imageUrl == null) {
            return;
        }
        String description = productNameOrEmpty(descriptionArea);
        String startingPriceText = productNameOrEmpty(startingPriceField);

        boolean formIsValid = true;

        // 1. Title Validation
        if (productName.isEmpty()) {
            formIsValid = false;
            if (errorTitle != null) {
                errorTitle.setVisible(true);
                errorTitle.setManaged(true);
            }
            if (productNameField != null && !productNameField.getStyleClass().contains("error-text-input")) {
                productNameField.getStyleClass().add("error-text-input");
            }
        } else {
            if (errorTitle != null) {
                errorTitle.setVisible(false);
                errorTitle.setManaged(false);
            }
            if (productNameField != null) {
                productNameField.getStyleClass().remove("error-text-input");
            }
        }

        // 2. Category Validation
        if (productType == null) {
            formIsValid = false;
            if (errorCategory != null) {
                errorCategory.setVisible(true);
                errorCategory.setManaged(true);
            }
            if (productTypeCombo != null && !productTypeCombo.getStyleClass().contains("error-text-input")) {
                productTypeCombo.getStyleClass().add("error-text-input");
            }
        } else {
            if (errorCategory != null) {
                errorCategory.setVisible(false);
                errorCategory.setManaged(false);
            }
            if (productTypeCombo != null) {
                productTypeCombo.getStyleClass().remove("error-text-input");
            }
        }

        // 3. Starting Price Validation
        BigDecimal startingPrice = BigDecimal.ZERO;
        if (startingPriceText.isEmpty()) {
            formIsValid = false;
            if (errorPrice != null) {
                errorPrice.setVisible(true);
                errorPrice.setManaged(true);
            }
            if (lblErrorPrice != null) {
                lblErrorPrice.setText("Starting price cannot be empty");
            }
            if (startingPriceField != null && !startingPriceField.getStyleClass().contains("error-text-input")) {
                startingPriceField.getStyleClass().add("error-text-input");
            }
        } else {
            try {
                startingPrice = new BigDecimal(startingPriceText.trim());
                if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException();
                }
                if (errorPrice != null) {
                    errorPrice.setVisible(false);
                    errorPrice.setManaged(false);
                }
                if (startingPriceField != null) {
                    startingPriceField.getStyleClass().remove("error-text-input");
                }
            } catch (Exception e) {
                formIsValid = false;
                if (errorPrice != null) {
                    errorPrice.setVisible(true);
                    errorPrice.setManaged(true);
                }
                if (lblErrorPrice != null) {
                    lblErrorPrice.setText("Starting price must be a positive number");
                }
                if (startingPriceField != null && !startingPriceField.getStyleClass().contains("error-text-input")) {
                    startingPriceField.getStyleClass().add("error-text-input");
                }
            }
        }

        // 3. Time Validation
        LocalDateTime startDT = getLocalDateTimeFromSplitFields(txtStartDay, txtStartMonth, txtStartYear, txtStartHour,
                txtStartMin);
        LocalDateTime endDT = getLocalDateTimeFromSplitFields(txtEndDay, txtEndMonth, txtEndYear, txtEndHour,
                txtEndMin);

        // Validate Start Time
        if (startDT == null) {
            formIsValid = false;
            if (errorStartDT != null) {
                errorStartDT.setVisible(true);
                errorStartDT.setManaged(true);
            }
            if (wrapperStartDT != null && !wrapperStartDT.getStyleClass().contains("error-segmented-input")) {
                wrapperStartDT.getStyleClass().add("error-segmented-input");
            }
            if (lblErrorStartDT != null) {
                lblErrorStartDT.setText("Please enter a valid start date and time!");
            }
        } else {
            if (errorStartDT != null) {
                errorStartDT.setVisible(false);
                errorStartDT.setManaged(false);
            }
            if (wrapperStartDT != null) {
                wrapperStartDT.getStyleClass().remove("error-segmented-input");
            }
        }

        // Validate End Time
        if (endDT == null) {
            formIsValid = false;
            if (errorEndDT != null) {
                errorEndDT.setVisible(true);
                errorEndDT.setManaged(true);
            }
            if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                wrapperEndDT.getStyleClass().add("error-segmented-input");
            }
            if (lblErrorEndDT != null) {
                lblErrorEndDT.setText("Please enter a valid end date and time!");
            }
        } else {
            if (startDT != null) {
                boolean isEndFuture = endDT.isAfter(LocalDateTime.now());
                boolean isEndAfterStart = endDT.isAfter(startDT);

                if (!isEndFuture || !isEndAfterStart) {
                    formIsValid = false;
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(true);
                        errorEndDT.setManaged(true);
                    }
                    if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                        wrapperEndDT.getStyleClass().add("error-segmented-input");
                    }
                    if (lblErrorEndDT != null) {
                        if (!isEndFuture) {
                            lblErrorEndDT.setText("End time must be in the future");
                        } else {
                            lblErrorEndDT.setText("End time must be after start time");
                        }
                    }
                } else {
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(false);
                        errorEndDT.setManaged(false);
                    }
                    if (wrapperEndDT != null) {
                        wrapperEndDT.getStyleClass().remove("error-segmented-input");
                    }
                }
            } else {
                boolean isEndFuture = endDT.isAfter(LocalDateTime.now());
                if (!isEndFuture) {
                    formIsValid = false;
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(true);
                        errorEndDT.setManaged(true);
                    }
                    if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                        wrapperEndDT.getStyleClass().add("error-segmented-input");
                    }
                    if (lblErrorEndDT != null) {
                        lblErrorEndDT.setText("End time must be in the future");
                    }
                } else {
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(false);
                        errorEndDT.setManaged(false);
                    }
                    if (wrapperEndDT != null) {
                        wrapperEndDT.getStyleClass().remove("error-segmented-input");
                    }
                }
            }
        }

        // If any validation failed, stop here
        if (!formIsValid) {
            return;
        }

        // 4. All validations passed! Show past start confirmation if needed
        if (startDT != null && startDT.isBefore(LocalDateTime.now())) {
            if (!showPastStartTimeConfirmationDialog()) {
                return;
            }
        }

        try {
            JSONObject body = new JSONObject();
            body.put("name", productName);
            body.put("type", productType);
            body.put("imagePath", imageUrl);
            body.put("description", description);
            body.put("startingPrice", startingPrice);

            BigDecimal reservePrice = BigDecimal.ZERO;
            String reservePriceText = reservePriceField != null ? reservePriceField.getText() : "";
            if (reservePriceText != null && !reservePriceText.trim().isEmpty()) {
                try {
                    reservePrice = new BigDecimal(reservePriceText.trim());
                } catch (Exception e) {
                }
            }
            body.put("reservePrice", reservePrice);
            body.put("sellerId", sellerId);
            body.put("stepPrice", 10000); // Default step price to avoid database NOT NULL constraint

            if (startDT == null) {
                body.put("startTime", JSONObject.NULL);
            } else {
                body.put("startTime", startDT.toString());
            }
            body.put("endTime", endDT.toString());

            HttpRequest request = newRequestBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/update-session/" + editingSession.id + "?sellerId="
                            + sellerId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(),
                    "Auction session updated successfully.");

            if (api.success) {
                clearForm();
                handleCloseModal();
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Success", api.message);
            } else {
                logger.error("Error api: {}", api.message);
                showAlert(Alert.AlertType.ERROR, "Error", api.message);
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Data Error", "Starting price must be a number.");
        } catch (Exception e) {
            logger.error("Cannot connect to server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot connect to the server!");
        }
    }

    @FXML
    private void handleShowStartCalendar() {
        if (datePickerStart != null) {
            datePickerStart.show();
        }
    }

    @FXML
    private void handleShowEndCalendar() {
        if (datePickerEnd != null) {
            datePickerEnd.show();
        }
    }

    private LocalDateTime getLocalDateTimeFromSplitFields(TextField day, TextField month, TextField year,
            TextField hour, TextField min) {
        if (day == null || month == null || year == null || hour == null || min == null)
            return null;
        String dStr = day.getText().trim();
        String mStr = month.getText().trim();
        String yStr = year.getText().trim();
        String hStr = hour.getText().trim();
        String minStr = min.getText().trim();

        if (dStr.isEmpty() || mStr.isEmpty() || yStr.isEmpty() || hStr.isEmpty() || minStr.isEmpty()) {
            return null;
        }

        try {
            int d = Integer.parseInt(dStr);
            int m = Integer.parseInt(mStr);
            int y = Integer.parseInt(yStr);
            int hr = Integer.parseInt(hStr);
            int mn = Integer.parseInt(minStr);

            return LocalDateTime.of(y, m, d, hr, mn);
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void handleCreateSession() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Could not get sellerId from session");
            showAlert(Alert.AlertType.ERROR, "Error", "Could not get sellerId from session.");
            return;
        }

        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String existingUuid = (editingSession != null) ? extractUuid(editingSession.imageUrl) : null;
        String imageUrl = prepareImagePathForSave(productNameOrEmpty(imageUrlField), existingUuid);
        if (imageUrl == null) {
            return;
        }
        String description = productNameOrEmpty(descriptionArea);
        String startingPriceText = productNameOrEmpty(startingPriceField);

        boolean formIsValid = true;

        // 1. Title Validation
        if (productName.isEmpty()) {
            formIsValid = false;
            if (errorTitle != null) {
                errorTitle.setVisible(true);
                errorTitle.setManaged(true);
            }
            if (productNameField != null && !productNameField.getStyleClass().contains("error-text-input")) {
                productNameField.getStyleClass().add("error-text-input");
            }
        } else {
            if (errorTitle != null) {
                errorTitle.setVisible(false);
                errorTitle.setManaged(false);
            }
            if (productNameField != null) {
                productNameField.getStyleClass().remove("error-text-input");
            }
        }

        // 2. Category Validation
        if (productType == null) {
            formIsValid = false;
            if (errorCategory != null) {
                errorCategory.setVisible(true);
                errorCategory.setManaged(true);
            }
            if (productTypeCombo != null && !productTypeCombo.getStyleClass().contains("error-text-input")) {
                productTypeCombo.getStyleClass().add("error-text-input");
            }
        } else {
            if (errorCategory != null) {
                errorCategory.setVisible(false);
                errorCategory.setManaged(false);
            }
            if (productTypeCombo != null) {
                productTypeCombo.getStyleClass().remove("error-text-input");
            }
        }

        // 3. Starting Price Validation
        BigDecimal startingPrice = BigDecimal.ZERO;
        if (startingPriceText.isEmpty()) {
            formIsValid = false;
            if (errorPrice != null) {
                errorPrice.setVisible(true);
                errorPrice.setManaged(true);
            }
            if (lblErrorPrice != null) {
                lblErrorPrice.setText("Starting price cannot be empty");
            }
            if (startingPriceField != null && !startingPriceField.getStyleClass().contains("error-text-input")) {
                startingPriceField.getStyleClass().add("error-text-input");
            }
        } else {
            try {
                startingPrice = new BigDecimal(startingPriceText.trim());
                if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException();
                }
                if (errorPrice != null) {
                    errorPrice.setVisible(false);
                    errorPrice.setManaged(false);
                }
                if (startingPriceField != null) {
                    startingPriceField.getStyleClass().remove("error-text-input");
                }
            } catch (Exception e) {
                formIsValid = false;
                if (errorPrice != null) {
                    errorPrice.setVisible(true);
                    errorPrice.setManaged(true);
                }
                if (lblErrorPrice != null) {
                    lblErrorPrice.setText("Starting price must be a positive number");
                }
                if (startingPriceField != null && !startingPriceField.getStyleClass().contains("error-text-input")) {
                    startingPriceField.getStyleClass().add("error-text-input");
                }
            }
        }

        // 3. Time Validation
        LocalDateTime startDT = getLocalDateTimeFromSplitFields(txtStartDay, txtStartMonth, txtStartYear, txtStartHour,
                txtStartMin);
        LocalDateTime endDT = getLocalDateTimeFromSplitFields(txtEndDay, txtEndMonth, txtEndYear, txtEndHour,
                txtEndMin);

        // Validate Start Time
        if (startDT == null) {
            formIsValid = false;
            if (errorStartDT != null) {
                errorStartDT.setVisible(true);
                errorStartDT.setManaged(true);
            }
            if (wrapperStartDT != null && !wrapperStartDT.getStyleClass().contains("error-segmented-input")) {
                wrapperStartDT.getStyleClass().add("error-segmented-input");
            }
            if (lblErrorStartDT != null) {
                lblErrorStartDT.setText("Please enter a valid start date and time!");
            }
        } else {
            if (errorStartDT != null) {
                errorStartDT.setVisible(false);
                errorStartDT.setManaged(false);
            }
            if (wrapperStartDT != null) {
                wrapperStartDT.getStyleClass().remove("error-segmented-input");
            }
        }

        // Validate End Time
        if (endDT == null) {
            formIsValid = false;
            if (errorEndDT != null) {
                errorEndDT.setVisible(true);
                errorEndDT.setManaged(true);
            }
            if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                wrapperEndDT.getStyleClass().add("error-segmented-input");
            }
            if (lblErrorEndDT != null) {
                lblErrorEndDT.setText("Please enter a valid end date and time!");
            }
        } else {
            if (startDT != null) {
                boolean isEndFuture = endDT.isAfter(LocalDateTime.now());
                boolean isEndAfterStart = endDT.isAfter(startDT);

                if (!isEndFuture || !isEndAfterStart) {
                    formIsValid = false;
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(true);
                        errorEndDT.setManaged(true);
                    }
                    if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                        wrapperEndDT.getStyleClass().add("error-segmented-input");
                    }
                    if (lblErrorEndDT != null) {
                        if (!isEndFuture) {
                            lblErrorEndDT.setText("End time must be in the future");
                        } else {
                            lblErrorEndDT.setText("End time must be after start time");
                        }
                    }
                } else {
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(false);
                        errorEndDT.setManaged(false);
                    }
                    if (wrapperEndDT != null) {
                        wrapperEndDT.getStyleClass().remove("error-segmented-input");
                    }
                }
            } else {
                boolean isEndFuture = endDT.isAfter(LocalDateTime.now());
                if (!isEndFuture) {
                    formIsValid = false;
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(true);
                        errorEndDT.setManaged(true);
                    }
                    if (wrapperEndDT != null && !wrapperEndDT.getStyleClass().contains("error-segmented-input")) {
                        wrapperEndDT.getStyleClass().add("error-segmented-input");
                    }
                    if (lblErrorEndDT != null) {
                        lblErrorEndDT.setText("End time must be in the future");
                    }
                } else {
                    if (errorEndDT != null) {
                        errorEndDT.setVisible(false);
                        errorEndDT.setManaged(false);
                    }
                    if (wrapperEndDT != null) {
                        wrapperEndDT.getStyleClass().remove("error-segmented-input");
                    }
                }
            }
        }

        // If any validation failed, stop here
        if (!formIsValid) {
            return;
        }

        // 4. All validations passed! Show past start confirmation if needed
        if (startDT != null && startDT.isBefore(LocalDateTime.now())) {
            if (!showPastStartTimeConfirmationDialog()) {
                return;
            }
        }

        try {
            JSONObject body = new JSONObject();
            body.put("name", productName);
            body.put("type", productType);
            body.put("imagePath", imageUrl);
            body.put("description", description);
            body.put("startingPrice", startingPrice);

            BigDecimal reservePrice = BigDecimal.ZERO;
            String reservePriceText = reservePriceField != null ? reservePriceField.getText() : "";
            if (reservePriceText != null && !reservePriceText.trim().isEmpty()) {
                try {
                    reservePrice = new BigDecimal(reservePriceText.trim());
                } catch (Exception e) {
                }
            }
            body.put("reservePrice", reservePrice);
            body.put("sellerId", sellerId);
            body.put("stepPrice", 10000); // Default step price

            if (startDT == null) {
                body.put("startTime", JSONObject.NULL);
            } else {
                body.put("startTime", startDT.toString());
            }
            body.put("endTime", endDT.toString());

            HttpRequest request = newRequestBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/create-auction"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(),
                    "Auction session created successfully.");

            if (api.success) {
                clearForm();
                handleCloseModal();
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Success", api.message);
            } else {
                logger.error("Error api: {}", api.message);
                showAlert(Alert.AlertType.ERROR, "Error", api.message);
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Data Error", "Starting price must be a number.");
        } catch (Exception e) {
            logger.error("Cannot connect to server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot connect to the server!");
        }
    }

    private void handleCancelSpecificSession(SessionItem selected) {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not get sellerId from session.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to cancel session #" + selected.id + "?");
        AlertUtil.styleDialog(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            HttpRequest request = newRequestBuilder()
                    .uri(URI.create(
                            Config.API_URL + "/api/seller/cancel-session/" + selected.id + "?sellerId=" + sellerId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(), "Session canceled successfully.");

            if (api.success) {
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Success", api.message);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", api.message);
            }
        } catch (Exception e) {
            logger.error("Cannot connect to server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot connect to the server!");
        }
    }

    private void handleQuickPublish(SessionItem selected) {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not get sellerId from session.");
            return;
        }

        // Validate basic fields (Title, Product Type, Starting Price)
        if (selected.productName == null || selected.productName.trim().isEmpty()
                || "Unknown".equals(selected.productName)) {
            showAlert(Alert.AlertType.ERROR, "Sale Error", "Product name of the draft cannot be empty!");
            return;
        }

        if (selected.productType == null || selected.productType.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Sale Error", "Product type of the draft cannot be empty!");
            return;
        }

        if (selected.startingPrice == null || selected.startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            showAlert(Alert.AlertType.ERROR, "Sale Error", "Starting price must be greater than 0!");
            return;
        }

        // Parse & validate Start/End times
        LocalDateTime startDT = null;
        if (selected.startTime != null && !selected.startTime.trim().isEmpty()
                && !"null".equalsIgnoreCase(selected.startTime)) {
            try {
                startDT = LocalDateTime.parse(selected.startTime);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Time Error",
                        "Invalid start time format in the draft!");
                return;
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Time Error", "Please enter a valid start date and time!");
            return;
        }

        LocalDateTime endDT = null;
        if (selected.endTime != null && !selected.endTime.trim().isEmpty()
                && !"null".equalsIgnoreCase(selected.endTime)) {
            try {
                endDT = LocalDateTime.parse(selected.endTime);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Time Error",
                        "Invalid end time format in the draft!");
                return;
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Time Error", "Please enter a valid end date and time!");
            return;
        }

        // End Time must be in the future
        if (!endDT.isAfter(LocalDateTime.now())) {
            showAlert(Alert.AlertType.ERROR, "Time Error", "End time must be in the future!");
            return;
        }

        // End Time must be after Start Time
        if (!endDT.isAfter(startDT)) {
            showAlert(Alert.AlertType.ERROR, "Time Error", "End time must be after start time!");
            return;
        }

        // Confirmation dialog
        if (!showQuickPublishConfirmationDialog(selected.id, selected.productName)) {
            return;
        }

        // Past Start Time check:
        if (startDT.isBefore(LocalDateTime.now())) {
            if (!showPastStartTimeConfirmationDialog()) {
                return;
            }
        }

        try {
            String selectedImagePath = prepareImagePathForSave(selected.imageUrl, extractUuid(selected.imageUrl));
            if (selectedImagePath == null) {
                return;
            }

            JSONObject body = new JSONObject();
            body.put("name", selected.productName);
            body.put("type", selected.productType);
            body.put("imagePath", selectedImagePath);
            body.put("description", selected.description);
            body.put("startingPrice", selected.startingPrice);
            body.put("sellerId", sellerId);
            body.put("stepPrice", selected.stepPrice.compareTo(BigDecimal.ZERO) <= 0 ? 10000 : selected.stepPrice);

            body.put("startTime", startDT.toString());
            body.put("endTime", endDT.toString());

            HttpRequest request = newRequestBuilder()
                    .uri(URI.create(
                            Config.API_URL + "/api/seller/update-session/" + selected.id + "?sellerId=" + sellerId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(),
                    "Auction session published successfully.");

            if (api.success) {
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Success", api.message);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", api.message);
            }

        } catch (Exception e) {
            logger.error("Cannot connect to server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot connect to the server!");
        }
    }

    @FXML
    private void goBack(javafx.event.ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
    }

    private void loadMySessions() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Could not get sellerId from session");
            showAlert(Alert.AlertType.ERROR, "Error", "Could not get sellerId from session.");
            return;
        }

        try {
            HttpRequest request = newRequestBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/my-sessions/" + sellerId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            ApiArrayResult api = extractDataArray(response.body(), response.statusCode());
            if (!api.success) {
                showAlert(Alert.AlertType.ERROR, "Error", api.message);
                return;
            }

            allSessions.clear();
            for (int i = 0; i < api.data.length(); i++) {
                allSessions.add(parseSession(api.data.getJSONObject(i)));
            }

            renderSessions(allSessions);
            updateStats();

        } catch (Exception e) {
            logger.error("Cannot connect to server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Network Error", "Cannot load seller data from server.");
        }
    }

    private SessionItem parseSession(JSONObject item) {
        SessionItem s = new SessionItem();
        s.id = item.optInt("id", 0);

        if (item.has("productName")) {
            s.productName = item.optString("productName", "Unknown");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.productName = product != null ? product.optString("name", "Unknown") : "Unknown";
        } else {
            s.productName = "Unknown";
        }

        if (item.has("productType")) {
            s.productType = item.optString("productType", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.productType = product != null ? product.optString("type", "") : "";
        }

        if (item.has("imagePath")) {
            s.imageUrl = item.optString("imagePath", "");
        } else if (item.has("imageUrl")) {
            s.imageUrl = item.optString("imageUrl", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            if (product != null && product.has("imagePath")) {
                s.imageUrl = product.optString("imagePath", "");
            } else {
                s.imageUrl = product != null ? product.optString("imageUrl", "") : "";
            }
        }

        if (item.has("description")) {
            s.description = item.optString("description", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.description = product != null ? product.optString("description", "") : "";
        }

        s.startingPrice = parseBigDecimal(item, "startingPrice");
        s.currentPrice = parseBigDecimal(item, "currentPrice");
        s.stepPrice = parseBigDecimal(item, "stepPrice");
        s.reservePrice = parseBigDecimal(item, "reservePrice");
        s.startTime = item.optString("startTime", "");
        s.endTime = item.optString("endTime", "");
        s.status = item.optString("status", "UNKNOWN");
        return s;
    }

    private BigDecimal parseBigDecimal(JSONObject item, String key) {
        if (!item.has(key) || item.isNull(key)) {
            return BigDecimal.ZERO;
        }

        Object value = item.get(key);

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void renderSessions(List<SessionItem> sessions) {
        displayedSessions.clear();
        displayedSessions.addAll(sessions);
    }

    private void updateStats() {
        int active = 0;
        int totalBidsEstimate = 0; // Since we don't have exact bid counts from session api directly, we might mock
                                   // this or use size
        BigDecimal revenue = BigDecimal.ZERO;

        for (SessionItem s : allSessions) {
            if (s.status == null)
                continue;
            switch (s.status.toUpperCase()) {
                case "ACTIVE", "LIVE" -> active++;
                case "COMPLETED" -> {
                    if (s.currentPrice != null) {
                        revenue = revenue.add(s.currentPrice);
                    }
                }
            }
            // Mock total bids based on status as we don't have the API data for it
            if ("ACTIVE".equalsIgnoreCase(s.status) || "COMPLETED".equalsIgnoreCase(s.status)) {
                totalBidsEstimate += 3;
            }
        }

        DecimalFormat df = new DecimalFormat("#,##0.##");
        lblTotalRevenue.setText("$" + df.format(revenue));
        lblActiveAuctions.setText(String.valueOf(active));
        lblTotalBids.setText(String.valueOf(totalBidsEstimate)); // Mock logic
    }

    private void clearForm() {
        productNameField.clear();
        imageUrlField.clear();
        descriptionArea.clear();
        if (reservePriceField != null)
            reservePriceField.clear();
        startingPriceField.clear();
        if (lblImageFileName != null)
            lblImageFileName.setText("");

        if (startingPriceField != null)
            startingPriceField.setDisable(false);
        if (txtStartDay != null)
            txtStartDay.setDisable(false);
        if (txtStartMonth != null)
            txtStartMonth.setDisable(false);
        if (txtStartYear != null)
            txtStartYear.setDisable(false);
        if (txtStartHour != null)
            txtStartHour.setDisable(false);
        if (txtStartMin != null)
            txtStartMin.setDisable(false);

        LocalDateTime now = LocalDateTime.now();
        txtStartDay.setText(String.format("%02d", now.getDayOfMonth()));
        txtStartMonth.setText(String.format("%02d", now.getMonthValue()));
        txtStartYear.setText(String.valueOf(now.getYear()));
        txtStartHour.setText(String.format("%02d", now.getHour()));
        txtStartMin.setText(String.format("%02d", now.getMinute()));

        LocalDateTime tomorrow = now.plusDays(1);
        txtEndDay.setText(String.format("%02d", tomorrow.getDayOfMonth()));
        txtEndMonth.setText(String.format("%02d", tomorrow.getMonthValue()));
        txtEndYear.setText(String.valueOf(tomorrow.getYear()));
        txtEndHour.setText(String.format("%02d", tomorrow.getHour()));
        txtEndMin.setText(String.format("%02d", tomorrow.getMinute()));

        if (errorEndDT != null) {
            errorEndDT.setVisible(false);
            errorEndDT.setManaged(false);
        }
        if (wrapperEndDT != null) {
            wrapperEndDT.getStyleClass().remove("error-segmented-input");
        }

        if (errorStartDT != null) {
            errorStartDT.setVisible(false);
            errorStartDT.setManaged(false);
        }
        if (wrapperStartDT != null) {
            wrapperStartDT.getStyleClass().remove("error-segmented-input");
        }

        if (errorTitle != null) {
            errorTitle.setVisible(false);
            errorTitle.setManaged(false);
        }
        if (productNameField != null) {
            productNameField.getStyleClass().remove("error-text-input");
        }

        if (errorPrice != null) {
            errorPrice.setVisible(false);
            errorPrice.setManaged(false);
        }
        if (startingPriceField != null) {
            startingPriceField.getStyleClass().remove("error-text-input");
        }

        if (errorCategory != null) {
            errorCategory.setVisible(false);
            errorCategory.setManaged(false);
        }
        if (productTypeCombo != null) {
            productTypeCombo.getStyleClass().remove("error-text-input");
        }

        fillDefaultStartTime();
        fillDefaultEndTime();
        productTypeCombo.setValue("Electronics");
    }

    private void fillDefaultStartTime() {
        LocalDateTime now = LocalDateTime.now();
        if (txtStartDay != null && txtStartDay.getText().trim().isEmpty()) {
            txtStartDay.setText(String.format("%02d", now.getDayOfMonth()));
        }
        if (txtStartMonth != null && txtStartMonth.getText().trim().isEmpty()) {
            txtStartMonth.setText(String.format("%02d", now.getMonthValue()));
        }
        if (txtStartYear != null && txtStartYear.getText().trim().isEmpty()) {
            txtStartYear.setText(String.format("%04d", now.getYear()));
        }
        if (txtStartHour != null && txtStartHour.getText().trim().isEmpty()) {
            txtStartHour.setText(String.format("%02d", now.getHour()));
        }
        if (txtStartMin != null && txtStartMin.getText().trim().isEmpty()) {
            txtStartMin.setText(String.format("%02d", now.getMinute()));
        }
    }

    private void fillDefaultEndTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        if (txtEndDay != null && txtEndDay.getText().trim().isEmpty()) {
            txtEndDay.setText(String.format("%02d", tomorrow.getDayOfMonth()));
            txtEndMonth.setText(String.format("%02d", tomorrow.getMonthValue()));
            txtEndYear.setText(String.format("%04d", tomorrow.getYear()));
            txtEndHour.setText("23");
            txtEndMin.setText("59");
        }
    }

    private void updateModalMaxHeight() {
        if (modalDialog == null || modalOverlay == null)
            return;
        double parentHeight = modalOverlay.getHeight() * 0.85;
        if (parentHeight <= 0)
            return;

        double chromeHeight = 0;
        double contentHeight = 0;

        for (Node child : modalDialog.getChildren()) {
            if (child instanceof ScrollPane scrollPane) {
                chromeHeight += 48; // ScrollPane vertical padding fallback (24px top + 24px bottom)
                if (scrollPane.getContent() instanceof Parent contentParent) {
                    contentHeight = contentParent.prefHeight(-1);
                }
            } else if (child instanceof Region region) {
                double h = region.getHeight();
                if (h <= 0) {
                    h = region.prefHeight(-1);
                }
                chromeHeight += h;
            }
        }

        // Add a 40px safety buffer to completely eliminate the vertical scrollbar when
        // stretched
        double naturalHeight = chromeHeight + contentHeight + 40;
        modalDialog.setMaxHeight(Math.min(parentHeight, naturalHeight));
    }

    private void handleViewSession(SessionItem item, ActionEvent event) {
        if (item == null) {
            return;
        }

        try {
            JSONObject sessionObj = buildSessionJson(item);
            JSONObject itemObj = buildItemJson(item);

            FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 1280, 800);
            AuctionPageController controller = loader.getController();
            if (controller != null) {
                controller.setItem(sessionObj, itemObj);
            }
        } catch (IOException e) {
            logger.error("Cannot open auction detail page", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Cannot open auction detail page.");
        }
    }

    private JSONObject buildSessionJson(SessionItem item) {
        JSONObject obj = new JSONObject();
        obj.put("id", item.id);
        obj.put("productName", safeString(item.productName));
        obj.put("productType", safeString(item.productType));
        obj.put("description", safeString(item.description));
        obj.put("imagePath", safeString(item.imageUrl));
        obj.put("startingPrice", item.startingPrice == null ? BigDecimal.ZERO : item.startingPrice);
        obj.put("currentPrice", item.currentPrice == null ? BigDecimal.ZERO : item.currentPrice);
        obj.put("stepPrice", item.stepPrice == null ? BigDecimal.ZERO : item.stepPrice);
        obj.put("reservePrice", item.reservePrice == null ? BigDecimal.ZERO : item.reservePrice);
        obj.put("startTime", safeString(item.startTime));
        obj.put("endTime", safeString(item.endTime));
        obj.put("status", safeString(item.status));
        obj.put("item", buildItemJson(item));
        return obj;
    }

    private JSONObject buildItemJson(SessionItem item) {
        JSONObject obj = new JSONObject();
        obj.put("name", safeString(item.productName));
        obj.put("type", safeString(item.productType));
        obj.put("description", safeString(item.description));
        obj.put("imagePath", safeString(item.imageUrl));
        obj.put("startingPrice", item.startingPrice == null ? BigDecimal.ZERO : item.startingPrice);
        return obj;
    }

    private String extractUuid(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        java.util.regex.Pattern uuidPattern = java.util.regex.Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        java.util.regex.Matcher matcher = uuidPattern.matcher(path);
        return matcher.find() ? matcher.group() : null;
    }

    private String prepareImagePathForSave(String rawPath) {
        return prepareImagePathForSave(rawPath, null);
    }

    private String prepareImagePathForSave(String rawPath, String existingUuid) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }

        String path = rawPath.trim();
        File localImage = toExistingLocalFile(path);
        if (localImage != null) {
            try {
                return uploadProductImage(localImage, existingUuid);
            } catch (Exception e) {
                logger.error("Cannot upload product image: {}", e.getMessage(), e);
                showAlert(Alert.AlertType.ERROR, "Image Upload Error", "Cannot upload product image to the server.");
                return null;
            }
        }

        return normalizeImagePathForStorage(path);
    }

    private File toExistingLocalFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        try {
            if (rawPath.startsWith("file:/")) {
                File file = new File(new URI(rawPath));
                return file.isFile() ? file : null;
            }
        } catch (Exception ignored) {
        }

        try {
            File file = new File(rawPath);
            return file.isFile() ? file : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String uploadProductImage(File imageFile) throws Exception {
        return uploadProductImage(imageFile, null);
    }

    private String uploadProductImage(File imageFile, String existingUuid) throws Exception {
        String uploadPath = "/api/files/images" + (existingUuid != null ? "?uuid=" + existingUuid : "");
        HttpResponse<String> response = HttpRequestUtil.uploadImage(Config.API_URL, uploadPath, imageFile);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Image upload failed with status " + response.statusCode());
        }

        JSONObject obj = new JSONObject(response.body());
        JSONObject data = obj.optJSONObject("data");
        String imagePath = data != null ? data.optString("imagePath", "") : obj.optString("imagePath", "");
        if (imagePath.isBlank()) {
            throw new IOException("Image upload response did not contain imagePath.");
        }
        return normalizeImagePathForStorage(imagePath);
    }

    private String normalizeImagePathForStorage(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }

        String path = rawPath.trim().replace("\\", "/");
        int apiIndex = path.indexOf("/api/files/images/");
        if (apiIndex >= 0) {
            path = path.substring(apiIndex + "/api/files/images/".length());
        }

        String apiPrefix = Config.API_URL + "/api/files/images/";
        if (path.startsWith(apiPrefix)) {
            path = path.substring(apiPrefix.length());
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.startsWith("api/files/images/")) {
            path = path.substring("api/files/images/".length());
        }
        if (path.startsWith("server/upload/images/")) {
            path = path.substring("server/upload/images/".length());
        }
        if (path.startsWith("upload/images/")) {
            path = path.substring("upload/images/".length());
        }
        if (path.startsWith("images/")) {
            path = path.substring("images/".length());
        }
        return path;
    }

    private String buildSellerImageUrl(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }

        String path = rawPath.trim().replace("\\", "/");
        if ((path.startsWith("http://") || path.startsWith("https://")) && !path.contains("/api/files/images/")) {
            return path;
        }

        path = normalizeImagePathForStorage(path);
        return path.isBlank() ? "" : Config.applyCacheBuster(Config.API_URL + "/api/files/images/" + path);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String productNameOrEmpty(TextInputControl input) {
        if (input == null || input.getText() == null) {
            return "";
        }
        return input.getText().trim();
    }

    private String safeMessage(String body) {
        if (body == null || body.isBlank())
            return "An error occurred from the server.";
        return body;
    }

    private ApiResult parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        if (body == null || body.isBlank()) {
            return new ApiResult(httpStatus >= 200 && httpStatus < 300,
                    httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage
                            : "An error occurred from the server.");
        }

        try {
            String trimmed = body.trim();
            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                int status = obj.optInt("status", httpStatus);
                String message = obj.optString("message",
                        status >= 200 && status < 300 ? defaultSuccessMessage : "An error occurred from the server.");
                return new ApiResult(status >= 200 && status < 300, message);
            }
        } catch (Exception ignored) {
        }

        return new ApiResult(httpStatus >= 200 && httpStatus < 300,
                httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage : safeMessage(body));
    }

    private ApiArrayResult extractDataArray(String body, int httpStatus) {
        if (body == null || body.isBlank()) {
            return new ApiArrayResult(false, "No data from the server.", new JSONArray());
        }

        try {
            String trimmed = body.trim();

            if (trimmed.startsWith("[")) {
                return new ApiArrayResult(true, "OK", new JSONArray(trimmed));
            }

            JSONObject obj = new JSONObject(trimmed);
            int status = obj.optInt("status", httpStatus);
            String message = obj.optString("message", "An error occurred from the server.");

            if (status < 200 || status >= 300) {
                return new ApiArrayResult(false, message, new JSONArray());
            }

            Object data = obj.opt("data");
            if (data instanceof JSONArray) {
                return new ApiArrayResult(true, message, (JSONArray) data);
            }

            return new ApiArrayResult(true, message, new JSONArray());
        } catch (Exception e) {
            return new ApiArrayResult(false, "Could not read data from the server.", new JSONArray());
        }
    }

    private boolean showPastStartTimeConfirmationDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().clear();
        dialogPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new javafx.geometry.Insets(32));
        container.setPrefWidth(360);
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16px; -fx-border-color: #ffe8f2; -fx-border-width: 1px; -fx-border-radius: 16px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.25), 30, 0, 0, 10);");

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(64, 64);
        iconCircle.setMaxSize(64, 64);
        iconCircle.setStyle("-fx-background-color: rgba(224, 64, 160, 0.1); -fx-background-radius: 32px;");

        FontIcon timerIcon = new FontIcon("mdi2t-timer-outline");
        timerIcon.setIconSize(36);
        timerIcon.setIconColor(Color.valueOf(sellerAccentHex()));
        iconCircle.getChildren().add(timerIcon);

        Label titleLabel = new Label("Start time has arrived!");
        titleLabel.setStyle(
                "-fx-font-family: 'DM Sans'; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -app-text;");

        Label descLabel = new Label(
                "Phiên đấu giá của sản phẩm đã sẵn sàng.\nBạn có muốn bắt đầu ngay không?");
        descLabel.setStyle(
                "-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-text-fill: -app-text-muted; -fx-text-alignment: center;");
        descLabel.setWrapText(true);

        VBox btnBox = new VBox(12);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPrefWidth(300);

        Button btnStartNow = new Button("Bắt đầu ngay");
        btnStartNow.setPrefHeight(44);
        btnStartNow.setMaxWidth(Double.MAX_VALUE);
        btnStartNow.setStyle(
                "-fx-background-color: -fx-accent; -fx-background-radius: 22px; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand;");

        Button btnEdit = new Button("Chỉnh sửa");
        btnEdit.setPrefHeight(44);
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnEdit.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #dcc8e0; -fx-border-width: 2px; -fx-border-radius: 22px; -fx-background-radius: 22px; -fx-text-fill: -app-text-muted; -fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");

        btnBox.getChildren().addAll(btnStartNow, btnEdit);
        container.getChildren().addAll(iconCircle, titleLabel, descLabel, btnBox);
        dialogPane.setContent(container);

        btnStartNow.setOnAction(e -> {
            dialog.setResult(ButtonType.OK);
            dialog.close();
        });

        btnEdit.setOnAction(e -> {
            dialog.setResult(ButtonType.CANCEL);
            dialog.close();
        });

        javafx.stage.Stage stage = (javafx.stage.Stage) dialogPane.getScene().getWindow();
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dialogPane.getScene().setFill(Color.TRANSPARENT);

        AlertUtil.styleDialog(dialog);

        java.util.Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private boolean showQuickPublishConfirmationDialog(int id, String productName) {
        Dialog<ButtonType> dialog = new Dialog<>();
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().clear();
        dialogPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new javafx.geometry.Insets(32));
        container.setPrefWidth(360);
        container.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16px; -fx-border-color: #ffe8f2; -fx-border-width: 1px; -fx-border-radius: 16px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.25), 30, 0, 0, 10);");

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(64, 64);
        iconCircle.setMaxSize(64, 64);
        iconCircle.setStyle("-fx-background-color: rgba(224, 64, 160, 0.1); -fx-background-radius: 32px;");

        FontIcon publishIcon = new FontIcon("mdi2p-publish");
        publishIcon.setIconSize(36);
        publishIcon.setIconColor(Color.valueOf(sellerAccentHex()));
        iconCircle.getChildren().add(publishIcon);

        Label titleLabel = new Label("Đăng bán nhanh");
        titleLabel.setStyle(
                "-fx-font-family: 'DM Sans'; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -app-text;");

        Label descLabel = new Label(
                "Bạn có chắc muốn đăng bán nhanh phiên #" + id + "\n(" + productName + ")?");
        descLabel.setStyle(
                "-fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-text-fill: -app-text-muted; -fx-text-alignment: center;");
        descLabel.setWrapText(true);

        VBox btnBox = new VBox(12);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPrefWidth(300);

        Button btnConfirm = new Button("Đăng bán");
        btnConfirm.setPrefHeight(44);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setStyle(
                "-fx-background-color: -fx-accent; -fx-background-radius: 22px; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand;");

        Button btnCancel = new Button("Hủy");
        btnCancel.setPrefHeight(44);
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #dcc8e0; -fx-border-width: 2px; -fx-border-radius: 22px; -fx-background-radius: 22px; -fx-text-fill: -app-text-muted; -fx-font-family: 'DM Sans'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");

        btnBox.getChildren().addAll(btnConfirm, btnCancel);
        container.getChildren().addAll(iconCircle, titleLabel, descLabel, btnBox);
        dialogPane.setContent(container);

        btnConfirm.setOnAction(e -> {
            dialog.setResult(ButtonType.OK);
            dialog.close();
        });

        btnCancel.setOnAction(e -> {
            dialog.setResult(ButtonType.CANCEL);
            dialog.close();
        });

        javafx.stage.Stage stage = (javafx.stage.Stage) dialogPane.getScene().getWindow();
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dialogPane.getScene().setFill(Color.TRANSPARENT);

        AlertUtil.styleDialog(dialog);

        java.util.Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        AlertUtil.show(type, title, message);
    }

    static class SessionItem {
        int id;
        String productName;
        String productType;
        String imageUrl;
        String description;
        BigDecimal startingPrice = BigDecimal.ZERO;
        BigDecimal currentPrice = BigDecimal.ZERO;
        BigDecimal stepPrice = BigDecimal.ZERO;
        BigDecimal reservePrice = BigDecimal.ZERO;
        String startTime;
        String endTime;
        String status;
    }

    private static class ApiResult {
        boolean success;
        String message;

        ApiResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private static class ApiArrayResult {
        boolean success;
        String message;
        JSONArray data;

        ApiArrayResult(boolean success, String message, JSONArray data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }

    private HttpRequest.Builder newRequestBuilder() {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        String token = User.getSessionToken();
        if (token != null && !token.isEmpty()) {
            builder.header("X-Auth-Token", token);
        }
        return builder;
    }
}