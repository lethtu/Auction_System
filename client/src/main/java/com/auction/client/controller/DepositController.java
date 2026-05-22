package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.util.NotificationBellBinder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ResourceBundle;

public class DepositController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DepositController.class);

    @FXML private Button btnHamburger;

    @FXML private Button btnNotificationBell;
    @FXML private Label notificationBadge;
    @FXML private Button btnSettings;
    @FXML private MenuButton userMenuButton;
    @FXML private Button btnDashboard;
    @FXML private StackPane topBarAvatarPane;

    @FXML private Label lblWalletBalance;
    @FXML private Button btnAmount50;
    @FXML private Button btnAmount100;
    @FXML private Button btnAmount500;
    @FXML private Button btnAmount1000;
    @FXML private TextField txtCustomAmount;
    
    @FXML private Label lblSummaryAmount;
    @FXML private Label lblSummaryTotal;
    @FXML private Button btnConfirmDeposit;
    
    @FXML private SidebarController sidebarController;

    private HttpClient client = HttpClient.newHttpClient();
    private BigDecimal currentDepositAmount = BigDecimal.ZERO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (User.getFullname() != null) {
            createUserOption("Hello, " + User.getFullname());
        }

        
        if (btnNotificationBell != null && notificationBadge != null) {
            NotificationBellBinder.bind(btnNotificationBell, notificationBadge);
        }

        Platform.runLater(() -> updateTopBarAvatar(User.getAvatarUrl()));
        if (btnSettings != null) {
            btnSettings.setOnAction(e -> {
                try {
                    com.auction.client.controller.SceneSwitcher.switchScene(e, "Settings.fxml", 1280, 800);
                } catch (IOException ex) {
                    logger.error("Error switching to Settings.fxml: ", ex);
                }
            });
        }

        txtCustomAmount.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtCustomAmount.setText(newValue.replaceAll("[^\\d]", ""));
            } else {
                handleCustomAmountChange(newValue);
            }
        });

        // Initialize wallet balance
        updateWalletBalanceDisplay();
        fetchLatestBalance();
        
        // Initial summary
        updateSummary(BigDecimal.ZERO);
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

        logoutItem.setOnAction(event -> {
            try {
                handleLogout(event);
            } catch (IOException e) {
                logger.error("Error switching to Login screen!", e);
            }
        });

        // In this page we are already on deposit, but just for consistency
        depositMoney.setOnAction(event -> {
            // Do nothing as we are already here
        });

        userMenuButton.getItems().addAll(accountItem, depositMoney, new SeparatorMenuItem(), logoutItem);
    }

    public void handleLogout(ActionEvent event) throws IOException {
        User.clearSession();
        SceneSwitcher.switchScene(event, "Login.fxml", 1100, 700);
    }

    @FXML
    public void handleToggleSidebar(ActionEvent event) {
        if (sidebarController != null) {
            sidebarController.toggleSidebar();
        }
    }



    @FXML
    public void handleGoBack(MouseEvent event) {
        try {
            ActionEvent actionEvent = new ActionEvent(event.getSource(), event.getTarget());
            SceneSwitcher.switchScene(actionEvent, "MainTemplate.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Error returning to main page: ", e);
        }
    }

    @FXML
    public void handleAmountSelection(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        
        // Reset all buttons style
        resetQuickAmountButtonsStyle();
        
        // Set active style for clicked button
        setActiveButtonStyle(clickedButton);
        
        // Extract amount and update
        BigDecimal amount = BigDecimal.ZERO;
        if (clickedButton == btnAmount50) amount = new BigDecimal("50000");
        else if (clickedButton == btnAmount100) amount = new BigDecimal("100000");
        else if (clickedButton == btnAmount500) amount = new BigDecimal("500000");
        else if (clickedButton == btnAmount1000) amount = new BigDecimal("1000000");

        currentDepositAmount = amount;
        
        // Clear custom input silently without triggering reset
        txtCustomAmount.setText("");
        
        updateSummary(currentDepositAmount);
    }

    private void handleCustomAmountChange(String newValue) {
        resetQuickAmountButtonsStyle();
        if (newValue == null || newValue.isEmpty()) {
            currentDepositAmount = BigDecimal.ZERO;
        } else {
            try {
                currentDepositAmount = new BigDecimal(newValue);
            } catch (NumberFormatException e) {
                currentDepositAmount = BigDecimal.ZERO;
            }
        }
        updateSummary(currentDepositAmount);
    }

    private void resetQuickAmountButtonsStyle() {
        String defaultStyle = "-fx-background-color: #f8eef8;  -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 2px; -fx-border-radius: 12px;";
        btnAmount50.setStyle(defaultStyle);
        btnAmount100.setStyle(defaultStyle);
        btnAmount500.setStyle(defaultStyle);
        btnAmount1000.setStyle(defaultStyle);
    }

    private void setActiveButtonStyle(Button button) {
        button.setStyle("-fx-background-color: #fef7ff; -fx-text-fill: -fx-accent; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: -fx-accent; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.2), 10, 0, 0, 2);");
    }

    private void updateSummary(BigDecimal amount) {
        String formatted = "₫ " + formatPrice(amount);
        lblSummaryAmount.setText(formatted);
        lblSummaryTotal.setText(formatted);
    }

    private void updateWalletBalanceDisplay() {
        lblWalletBalance.setText("₫ " + formatPrice(User.getBalance()));
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("###,###", symbols);
        return df.format(price);
    }

    private void fetchLatestBalance() {
        if (User.getId() == null) return;
        
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + User.getId()))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.body());
                    if (responseJson.optInt("status", 500) == 200) {
                        JSONObject data = responseJson.optJSONObject("data");
                        if (data != null && !data.isNull("balance")) {
                            BigDecimal newBalance = new BigDecimal(data.get("balance").toString());
                            User.setBalance(newBalance);
                            Platform.runLater(this::updateWalletBalanceDisplay);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Cannot get latest balance: {}", e.getMessage());
            }
        }).start();
    }

    @FXML
    public void handleConfirmDeposit(ActionEvent event) {
        if (User.getId() == null) {
            showError("Please log in before depositing.");
            return;
        }

        if (currentDepositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            showWarning("Invalid amount", "Please select or enter an amount greater than 0 to deposit.");
            return;
        }

        btnConfirmDeposit.setDisable(true);
        btnConfirmDeposit.setText("Processing...");

        new Thread(() -> {
            try {
                String url = Config.API_URL + "/api/bidder/deposit?bidderId=" + User.getId() + "&amount=" + currentDepositAmount.toPlainString();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    btnConfirmDeposit.setDisable(false);
                    btnConfirmDeposit.setText("Confirm Deposit");
                    
                    if (response.statusCode() == 200) {
                        showInfo("Deposit Successful", "You have successfully deposited " + formatPrice(currentDepositAmount) + " ₫ to your wallet.");
                        // Reload balance after successful deposit
                        fetchLatestBalance();
                        // Reset form
                        txtCustomAmount.setText("");
                        resetQuickAmountButtonsStyle();
                        currentDepositAmount = BigDecimal.ZERO;
                        updateSummary(currentDepositAmount);
                    } else {
                        showError("Deposit failed. Server returned error: " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                logger.error("Error during deposit: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    btnConfirmDeposit.setDisable(false);
                    btnConfirmDeposit.setText("Confirm Deposit");
                    showError("Cannot connect to the server.");
                });
            }
        }, "deposit-money").start();
    }

    private void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    private void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    private void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Error", message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-family: 'DM Sans'; -fx-background-color: #fcf8ff; -fx-border-color: -fx-accent; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");
        
        alert.showAndWait();
    }

    private void updateTopBarAvatar(String avatarUrl) {
        if (topBarAvatarPane == null) return;
        try {
            topBarAvatarPane.getChildren().clear();
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : Config.API_URL + avatarUrl;
                ImageView imgView = new ImageView(new Image(fullUrl, 36, 36, false, true, true));
                imgView.setFitWidth(36);
                imgView.setFitHeight(36);
                imgView.setSmooth(true);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                imgView.setClip(clip);
                topBarAvatarPane.getChildren().add(imgView);
            } else {
                Label icon = new Label("\uE7FD");
                icon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: white;");
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