package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.util.NotificationBellBinder;
import com.auction.client.util.AlertUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
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

    @FXML private TopbarController topbarController;

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
        if (topbarController != null) {
            topbarController.setSearchVisible(false);
            if (sidebarController != null) {
                topbarController.setSidebarController(sidebarController);
            }
        }

        if (topbarController != null && topbarController.getTxtSearch() != null) {
            topbarController.getTxtSearch().setOnAction(e -> {
                try {
                    String query = topbarController.getTxtSearch().getText();
                    if (query != null && !query.trim().isEmpty()) {
                        MainController.initialHomeFilterMode = "SEARCH:" + query.trim();
                        SceneSwitcher.switchScene(e, "MainTemplate.fxml", 1280, 800);
                    }
                } catch (IOException ex) {
                    logger.error("Error switching page: ", ex);
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
        String defaultStyle = "-fx-background-color: #f8eef8; -fx-text-fill: #2e1a28; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 2px; -fx-border-radius: 12px;";
        btnAmount50.setStyle(defaultStyle);
        btnAmount100.setStyle(defaultStyle);
        btnAmount500.setStyle(defaultStyle);
        btnAmount1000.setStyle(defaultStyle);
    }

    private void setActiveButtonStyle(Button button) {
        button.setStyle("-fx-background-color: #fef7ff; -fx-text-fill: #e040a0; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: #e040a0; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.2), 10, 0, 0, 2);");
    }

    private void updateSummary(BigDecimal amount) {
        String formatted = "\u20ab " + formatPrice(amount);
        lblSummaryAmount.setText(formatted);
        lblSummaryTotal.setText(formatted);
    }

    private void updateWalletBalanceDisplay() {
        lblWalletBalance.setText("\u20ab " + formatPrice(User.getBalance()));
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
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + User.getId()))
                        .GET();
                if (User.getSessionToken() != null) {
                    builder.header("X-Auth-Token", User.getSessionToken());
                }
                HttpRequest request = builder.build();

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
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody());
                if (User.getSessionToken() != null) {
                    builder.header("X-Auth-Token", User.getSessionToken());
                }
                HttpRequest request = builder.build();
                
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
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showAlert(type, title, message));
            return;
        }
        AlertUtil.show(type, title, message);
    }

    private void openMainTemplateFromCurrentWindow() throws IOException {
        Window window = lblWalletBalance != null && lblWalletBalance.getScene() != null
                ? lblWalletBalance.getScene().getWindow()
                : null;

        if (window instanceof Stage stage) {
            boolean wasMaximized = stage.isMaximized();
            int width = Math.max(1280, (int) Math.round(stage.getWidth()));
            int height = Math.max(800, (int) Math.round(stage.getHeight()));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/MainTemplate.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root, width, height));
            stage.show();

            if (wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
            return;
        }

        SceneSwitcher.switchScene(new ActionEvent(lblWalletBalance, lblWalletBalance), "MainTemplate.fxml", 1280, 800);
    }
}