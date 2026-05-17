package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.HttpClientSingleton;
import com.auction.client.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.util.StringConverter;

public class SellerDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(SellerDashboardController.class);

    @FXML private StackPane modalOverlay;
    @FXML private VBox modalDialog;
    @FXML private Label modalTitle;
    @FXML private Button btnSubmit;
    private SessionItem editingSession = null;
    @FXML private ComboBox<String> productTypeCombo;
    @FXML private TextField productNameField;
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startingPriceField;
    @FXML private VBox imageUploadArea;
    @FXML private Label lblImageFileName;
    @FXML private DatePicker datePickerStart;
    @FXML private TextField txtStartDay;
    @FXML private TextField txtStartMonth;
    @FXML private TextField txtStartYear;
    @FXML private TextField txtStartHour;
    @FXML private TextField txtStartMin;

    @FXML private DatePicker datePickerEnd;
    @FXML private TextField txtEndDay;
    @FXML private TextField txtEndMonth;
    @FXML private TextField txtEndYear;
    @FXML private TextField txtEndHour;
    @FXML private TextField txtEndMin;

    @FXML private Label lblTotalRevenue;
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblTotalBids;
    
    @FXML private TableView<SessionItem> sessionsTable;
    @FXML private TableColumn<SessionItem, SessionItem> colItem;
    @FXML private TableColumn<SessionItem, String> colStatus;
    @FXML private TableColumn<SessionItem, BigDecimal> colBid;
    @FXML private TableColumn<SessionItem, String> colTimeLeft;
    @FXML private TableColumn<SessionItem, SessionItem> colActions;

    @FXML private TextField txtSearch;

    @FXML private ScrollPane sidebarContainer;
    @FXML private VBox sidebarContent;
    @FXML private Button btnHamburger;
    private boolean isSidebarCollapsed = false;
    private final java.util.Map<Button, String> sidebarButtonTextMap = new java.util.HashMap<>();

    private final HttpClient httpClient = HttpClientSingleton.getInstance().getHttpClient();
    private final List<SessionItem> allSessions = new ArrayList<>();
    private final ObservableList<SessionItem> displayedSessions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        productTypeCombo.setItems(FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"
        ));
        productTypeCombo.setValue("Electronics");
        
        setupTable();
        setupImageUpload();
        setupSplitDatetimePickers();
        
        if (modalDialog != null && modalOverlay != null) {
            modalOverlay.heightProperty().addListener((obs, oldVal, newVal) -> updateModalMaxHeight());
            modalDialog.widthProperty().addListener((obs, oldVal, newVal) -> updateModalMaxHeight());
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
        if (source == null || target == null) return;
        source.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() >= length) {
                target.requestFocus();
            }
        });
    }
    
    private void setupImageUpload() {
        if (imageUploadArea == null) return;
        
        imageUploadArea.setOnDragOver(event -> {
            if (event.getGestureSource() != imageUploadArea && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                imageUploadArea.setStyle("-fx-border-color: #e040a0; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 32px; -fx-cursor: hand; -fx-background-color: #ffd6ee;");
            }
            event.consume();
        });

        imageUploadArea.setOnDragExited(event -> {
            imageUploadArea.setStyle("-fx-border-color: #dcc8e0; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 32px; -fx-cursor: hand; -fx-background-color: transparent;");
            event.consume();
        });

        imageUploadArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                String path = file.toURI().toString();
                imageUrlField.setText(path);
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
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File selectedFile = fileChooser.showOpenDialog(imageUploadArea.getScene().getWindow());
            if (selectedFile != null) {
                String path = selectedFile.toURI().toString();
                imageUrlField.setText(path);
                lblImageFileName.setText(selectedFile.getName());
            }
        });
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
                    imgContainer.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 24px;");
                    
                    if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                        try {
                            ImageView iv = new ImageView(new Image(item.imageUrl, 48, 48, true, true));
                            Circle clip = new Circle(24, 24, 24);
                            iv.setClip(clip);
                            imgContainer.getChildren().add(iv);
                        } catch (Exception e) {
                            FontIcon icon = new FontIcon("mdi2i-image-outline");
                            icon.setIconSize(24);
                            icon.setIconColor(Color.valueOf("#907898"));
                            imgContainer.getChildren().add(icon);
                        }
                    } else {
                        FontIcon icon = new FontIcon("mdi2i-image-outline");
                        icon.setIconSize(24);
                        icon.setIconColor(Color.valueOf("#907898"));
                        imgContainer.getChildren().add(icon);
                    }

                    VBox vbox = new VBox(2);
                    vbox.setAlignment(Pos.CENTER_LEFT);
                    Label lblName = new Label(item.productName);
                    lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e1a28;");
                    Label lblId = new Label("ID: #AUC-" + item.id);
                    lblId.setStyle("-fx-font-size: 11px; -fx-text-fill: #604868;");
                    
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
                    setGraphic(null);
                } else {
                    Label lblStatus = new Label(status.toUpperCase());
                    lblStatus.getStyleClass().add("badge");
                    
                    switch (status.toUpperCase()) {
                        case "ACTIVE", "LIVE" -> lblStatus.getStyleClass().add("badge-live");
                        case "DRAFT" -> lblStatus.getStyleClass().add("badge-draft");
                        case "COMPLETED", "ENDED" -> lblStatus.getStyleClass().add("badge-ended");
                        case "COMING" -> {
                            lblStatus.setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #7c52aa; -fx-border-color: #dcc8e0; -fx-border-radius: 12px; -fx-background-radius: 12px;");
                        }
                        case "CANCELED" -> {
                            lblStatus.setStyle("-fx-background-color: #ffe8e8; -fx-text-fill: #e53e3e;");
                        }
                        default -> lblStatus.setStyle("-fx-background-color: #f2e8f2; -fx-text-fill: #604868;");
                    }
                    setGraphic(lblStatus);
                }
            }
        });

        colBid.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().currentPrice));
        colBid.setCellFactory(col -> new TableCell<>() {
            private final DecimalFormat df = new DecimalFormat("#,##0.##");
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    if (price.compareTo(BigDecimal.ZERO) == 0) {
                        setText("--");
                        setStyle("-fx-text-fill: #604868; -fx-font-weight: bold; -fx-alignment: center-right;");
                    } else {
                        setText("$" + df.format(price));
                        setStyle("-fx-text-fill: #e040a0; -fx-font-weight: 900; -fx-alignment: center-right;");
                    }
                }
            }
        });

        colTimeLeft.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().endTime));
        colTimeLeft.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String endTime, boolean empty) {
                super.updateItem(endTime, empty);
                if (empty || endTime == null || endTime.isEmpty()) {
                    setText(null);
                } else {
                    try {
                        LocalDateTime end = LocalDateTime.parse(endTime);
                        if (end.isBefore(LocalDateTime.now())) {
                            setText("Ended");
                            setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-alignment: center-right;");
                        } else {
                            Duration d = Duration.between(LocalDateTime.now(), end);
                            long hours = d.toHours();
                            long minutes = d.toMinutesPart();
                            setText(String.format("%02dh %02dm", hours, minutes));
                            setStyle("-fx-text-fill: #604868; -fx-font-weight: bold; -fx-alignment: center-right;");
                        }
                    } catch (Exception e) {
                        setText(endTime);
                    }
                }
            }
        });

        colActions.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(SessionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(8);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    
                    Button btnView = createIconButton("mdi2e-eye", "#0096cc");
                    Button btnEdit = createIconButton("mdi2p-pencil", "#7c52aa");
                    Button btnDelete = createIconButton("mdi2d-delete", "#e53e3e");
                    
                    btnDelete.setOnAction(e -> handleCancelSpecificSession(item));
                    btnEdit.setOnAction(e -> handleShowEditModal(item));
                    
                    // Delete is allowed for ACTIVE or COMING sessions
                    boolean isDeletable = "ACTIVE".equalsIgnoreCase(item.status) || "COMING".equalsIgnoreCase(item.status);
                    if (!isDeletable) {
                        btnDelete.setDisable(true);
                        btnDelete.setOpacity(0.3);
                    }
                    // Edit is allowed for ACTIVE or COMING sessions
                    boolean isEditable = "ACTIVE".equalsIgnoreCase(item.status) || "COMING".equalsIgnoreCase(item.status);
                    if (!isEditable) {
                        btnEdit.setDisable(true);
                        btnEdit.setOpacity(0.3);
                    }

                    hbox.getChildren().addAll(btnView, btnEdit, btnDelete);
                    setGraphic(hbox);
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
        clearForm();
        modalOverlay.setVisible(true);
    }

    @FXML
    private void handleCloseModal() {
        modalOverlay.setVisible(false);
    }

    private void handleShowEditModal(SessionItem item) {
        if (item == null) return;
        editingSession = item;
        
        if (modalTitle != null) {
            modalTitle.setText("Edit Listing");
        }
        if (btnSubmit != null) {
            btnSubmit.setText("Save Changes");
        }
        
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
        
        // Populate Date/Time fields
        populateSplitTimeFields(item.startTime, txtStartDay, txtStartMonth, txtStartYear, txtStartHour, txtStartMin);
        populateSplitTimeFields(item.endTime, txtEndDay, txtEndMonth, txtEndYear, txtEndHour, txtEndMin);
        
        modalOverlay.setVisible(true);
    }

    private void populateSplitTimeFields(String timeStr, TextField day, TextField month, TextField year, TextField hour, TextField min) {
        if (day == null || month == null || year == null || hour == null || min == null) return;
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

    private void handleUpdateSession() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Không lấy được sellerId từ session");
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String imageUrl = productNameOrEmpty(imageUrlField);
        String description = productNameOrEmpty(descriptionArea);
        String startingPriceText = productNameOrEmpty(startingPriceField);

        if (productName.isEmpty() || productType == null || startingPriceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại và giá khởi điểm.");
            return;
        }

        try {
            BigDecimal startingPrice = new BigDecimal(startingPriceText.trim());

            JSONObject body = new JSONObject();

            body.put("name", productName);
            body.put("type", productType);
            body.put("imagePath", imageUrl);
            body.put("description", description);
            body.put("startingPrice", startingPrice);
            body.put("sellerId", sellerId);
            body.put("stepPrice", 10000); // Default step price to avoid database NOT NULL constraint

            LocalDateTime startDT = getLocalDateTimeFromSplitFields(txtStartDay, txtStartMonth, txtStartYear, txtStartHour, txtStartMin);
            if (startDT == null) {
                body.put("startTime", JSONObject.NULL);
            } else {
                body.put("startTime", startDT.toString());
            }

            LocalDateTime endDT = getLocalDateTimeFromSplitFields(txtEndDay, txtEndMonth, txtEndYear, txtEndHour, txtEndMin);
            if (endDT == null) {
                showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng nhập ngày giờ kết thúc hợp lệ!");
                return;
            }

            if (!endDT.isAfter(LocalDateTime.now())) {
                showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phải ở tương lai!");
                return;
            }
            body.put("endTime", endDT.toString());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/update-session/" + editingSession.id + "?sellerId=" + sellerId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(), "Cập nhật phiên đấu giá thành công.");

            if (api.success) {
                clearForm();
                handleCloseModal();
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", api.message);
            } else {
                logger.error("Lỗi api: {}", api.message);
                showAlert(Alert.AlertType.ERROR, "Lỗi", api.message);
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Giá khởi điểm phải là số.");
        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến máy chủ: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
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

    private LocalDateTime getLocalDateTimeFromSplitFields(TextField day, TextField month, TextField year, TextField hour, TextField min) {
        if (day == null || month == null || year == null || hour == null || min == null) return null;
        String dStr = day.getText().trim();
        String mStr = month.getText().trim();
        String yStr = year.getText().trim();
        String hStr = hour.getText().trim();
        String minStr = min.getText().trim();

        if (dStr.isEmpty() || mStr.isEmpty() || yStr.isEmpty()) {
            return null;
        }

        try {
            int d = Integer.parseInt(dStr);
            int m = Integer.parseInt(mStr);
            int y = Integer.parseInt(yStr);
            int hr = hStr.isEmpty() ? 0 : Integer.parseInt(hStr);
            int mn = minStr.isEmpty() ? 0 : Integer.parseInt(minStr);

            return LocalDateTime.of(y, m, d, hr, mn);
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void handleCreateSession() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Không lấy được sellerId từ session");
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        String productName = productNameField.getText().trim();
        String productType = productTypeCombo.getValue();
        String imageUrl = productNameOrEmpty(imageUrlField);
        String description = productNameOrEmpty(descriptionArea);
        String startingPriceText = productNameOrEmpty(startingPriceField);

        if (productName.isEmpty() || productType == null || startingPriceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu",
                    "Vui lòng nhập tên sản phẩm, loại và giá khởi điểm.");
            return;
        }

        try {
            BigDecimal startingPrice = new BigDecimal(startingPriceText.trim());

            JSONObject body = new JSONObject();

            body.put("name", productName);
            body.put("type", productType);
            body.put("imagePath", imageUrl);
            body.put("description", description);
            body.put("startingPrice", startingPrice);
            body.put("sellerId", sellerId);
            body.put("stepPrice", 10000); // Default step price to avoid database NOT NULL constraint

            LocalDateTime startDT = getLocalDateTimeFromSplitFields(txtStartDay, txtStartMonth, txtStartYear, txtStartHour, txtStartMin);
            if (startDT == null) {
                body.put("startTime", JSONObject.NULL);
            } else {
                if (startDT.isBefore(LocalDateTime.now())) {
                    showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian bắt đầu không được ở quá khứ!");
                    return;
                }
                body.put("startTime", startDT.toString());
            }

            LocalDateTime endDT = getLocalDateTimeFromSplitFields(txtEndDay, txtEndMonth, txtEndYear, txtEndHour, txtEndMin);
            if (endDT == null) {
                showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng nhập ngày giờ kết thúc hợp lệ!");
                return;
            }

            if (!endDT.isAfter(LocalDateTime.now())) {
                showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phải ở tương lai!");
                return;
            }
            body.put("endTime", endDT.toString());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/create-auction"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(), "Tạo phiên đấu giá thành công.");

            if (api.success) {
                clearForm();
                handleCloseModal();
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", api.message);
            } else {
                logger.error("Lỗi api: {}", api.message);
                showAlert(Alert.AlertType.ERROR, "Lỗi", api.message);
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Giá khởi điểm phải là số.");
        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến máy chủ: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    private void handleCancelSpecificSession(SessionItem selected) {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn hủy phiên #" + selected.id + " không?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/cancel-session/" + selected.id + "?sellerId=" + sellerId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ApiResult api = parseApiResponse(response.body(), response.statusCode(), "Đã hủy phiên thành công.");

            if (api.success) {
                loadMySessions();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", api.message);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", api.message);
            }
        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến máy chủ: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ!");
        }
    }

    @FXML
    private void goBack(javafx.event.ActionEvent event) throws IOException {
        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 700);
    }

    private void loadMySessions() {
        Integer sellerId = User.getId();
        if (sellerId == null) {
            logger.error("Lỗi không lấy được sellerId từ session");
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được sellerId từ session.");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.API_URL + "/api/seller/my-sessions/" + sellerId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            ApiArrayResult api = extractDataArray(response.body(), response.statusCode());
            if (!api.success) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", api.message);
                return;
            }

            allSessions.clear();
            for (int i = 0; i < api.data.length(); i++) {
                allSessions.add(parseSession(api.data.getJSONObject(i)));
            }

            renderSessions(allSessions);
            updateStats();

        } catch (Exception e) {
            logger.error("Lỗi không thể kết nối đến server: {}", e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể tải dữ liệu seller từ server.");
        }
    }

    private SessionItem parseSession(JSONObject item) {
        SessionItem s = new SessionItem();
        s.id = item.optInt("id", 0);

        if (item.has("productName")) {
            s.productName = item.optString("productName", "Không rõ");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.productName = product != null ? product.optString("name", "Không rõ") : "Không rõ";
        } else {
            s.productName = "Không rõ";
        }

        if (item.has("productType")) {
            s.productType = item.optString("productType", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.productType = product != null ? product.optString("type", "") : "";
        }

        if (item.has("imageUrl")) {
            s.imageUrl = item.optString("imageUrl", "");
        } else if (item.has("product")) {
            JSONObject product = item.optJSONObject("product");
            s.imageUrl = product != null ? product.optString("imageUrl", "") : "";
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
        int totalBidsEstimate = 0; // Since we don't have exact bid counts from session api directly, we might mock this or use size
        BigDecimal revenue = BigDecimal.ZERO;

        for (SessionItem s : allSessions) {
            if (s.status == null) continue;
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
        startingPriceField.clear();
        if (lblImageFileName != null) lblImageFileName.setText("");
        
        txtStartDay.clear();
        txtStartMonth.clear();
        txtStartYear.clear();
        txtStartHour.clear();
        txtStartMin.clear();
        
        txtEndDay.clear();
        txtEndMonth.clear();
        txtEndYear.clear();
        txtEndHour.clear();
        txtEndMin.clear();

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
        if (modalDialog == null || modalOverlay == null) return;
        double parentHeight = modalOverlay.getHeight() * 0.85;
        if (parentHeight <= 0) return;

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

        // Add a 40px safety buffer to completely eliminate the vertical scrollbar when stretched
        double naturalHeight = chromeHeight + contentHeight + 40;
        modalDialog.setMaxHeight(Math.min(parentHeight, naturalHeight));
    }

    @FXML
    private void handleToggleSidebar(javafx.event.ActionEvent event) {
        isSidebarCollapsed = !isSidebarCollapsed;

        if (isSidebarCollapsed) {
            // Collapse
            sidebarContainer.setMinWidth(70);
            sidebarContainer.setPrefWidth(70);
            sidebarContainer.setMaxWidth(70);
            sidebarContent.setPadding(new javafx.geometry.Insets(24, 0, 24, 0));
            sidebarContent.setAlignment(Pos.TOP_CENTER);

            for (javafx.scene.Node node : sidebarContent.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    String currentText = btn.getText();
                    if (currentText != null && !currentText.isEmpty()) {
                        sidebarButtonTextMap.put(btn, currentText);
                    }
                    
                    String tooltipText = sidebarButtonTextMap.get(btn);
                    if (tooltipText != null) {
                        Tooltip tooltip = new Tooltip(tooltipText);
                        tooltip.setStyle("-fx-background-color: #e040a0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 6px 12px; -fx-font-size: 13px;");
                        
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
                        pause.setOnFinished(e -> {
                            if (btn.isHover()) {
                                javafx.geometry.Bounds bounds = btn.localToScreen(btn.getBoundsInLocal());
                                tooltip.show(btn, bounds.getMaxX() + 15, bounds.getMinY() + btn.getHeight() / 2 - 18);
                            }
                        });

                        btn.setOnMouseEntered(e -> pause.playFromStart());
                        btn.setOnMouseExited(e -> {
                            pause.stop();
                            tooltip.hide();
                        });
                    }

                    btn.setTooltip(null); // Tắt Tooltip mặc định của JavaFX

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
            sidebarContent.setPadding(new javafx.geometry.Insets(24, 8, 24, 8));
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

    private String productNameOrEmpty(TextInputControl input) {
        return input == null ? "" : input.getText().trim();
    }

    private String safeMessage(String body) {
        if (body == null || body.isBlank()) return "Có lỗi xảy ra từ server.";
        return body;
    }

    private ApiResult parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        if (body == null || body.isBlank()) {
            return new ApiResult(httpStatus >= 200 && httpStatus < 300,
                    httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage : "Có lỗi xảy ra từ server.");
        }

        try {
            String trimmed = body.trim();
            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                int status = obj.optInt("status", httpStatus);
                String message = obj.optString("message",
                        status >= 200 && status < 300 ? defaultSuccessMessage : "Có lỗi xảy ra từ server.");
                return new ApiResult(status >= 200 && status < 300, message);
            }
        } catch (Exception ignored) {
        }

        return new ApiResult(httpStatus >= 200 && httpStatus < 300,
                httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage : safeMessage(body));
    }

    private ApiArrayResult extractDataArray(String body, int httpStatus) {
        if (body == null || body.isBlank()) {
            return new ApiArrayResult(false, "Không có dữ liệu từ server.", new JSONArray());
        }

        try {
            String trimmed = body.trim();

            if (trimmed.startsWith("[")) {
                return new ApiArrayResult(true, "OK", new JSONArray(trimmed));
            }

            JSONObject obj = new JSONObject(trimmed);
            int status = obj.optInt("status", httpStatus);
            String message = obj.optString("message", "Có lỗi xảy ra từ server.");

            if (status < 200 || status >= 300) {
                return new ApiArrayResult(false, message, new JSONArray());
            }

            Object data = obj.opt("data");
            if (data instanceof JSONArray) {
                return new ApiArrayResult(true, message, (JSONArray) data);
            }

            return new ApiArrayResult(true, message, new JSONArray());
        } catch (Exception e) {
            return new ApiArrayResult(false, "Không đọc được dữ liệu từ server.", new JSONArray());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class SessionItem {
        int id;
        String productName;
        String productType;
        String imageUrl;
        String description;
        BigDecimal startingPrice = BigDecimal.ZERO;
        BigDecimal currentPrice = BigDecimal.ZERO;
        BigDecimal stepPrice = BigDecimal.ZERO;
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
}