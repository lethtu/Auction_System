package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.client.Config;
import com.auction.client.model.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;
import com.auction.client.util.AlertUtil;

public class UpToSellerController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(UpToSellerController.class);

    @FXML private CheckBox chkAgree;
    @FXML private Label lblMessage;
    @FXML private Button btnUpgrade;

    @FXML private SidebarController sidebarController;
    @FXML private TopbarController topbarController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (topbarController != null) {
            topbarController.setSearchVisible(false);
            if (sidebarController != null) {
                topbarController.setSidebarController(sidebarController);
            }
        }
    }

    @FXML
    public void handleUpgrade(ActionEvent event) {
        if (User.getId() == null) {
            showError("Please log in to upgrade!");
            return;
        }

        if (!chkAgree.isSelected()) {
            showError("Please agree to the BidPop terms of sale!");
            return;
        }

        btnUpgrade.setDisable(true);
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);

        new Thread(() -> {
            try {
                String apiUrl = Config.API_URL + "/api/bidder/up-to-seller?userId=" + User.getId();
                HttpURLConnection conn = (HttpURLConnection) java.net.URI.create(apiUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                if (User.getSessionToken() != null && !User.getSessionToken().isBlank()) {
                    conn.setRequestProperty("X-Auth-Token", User.getSessionToken());
                }

                int responseCode = conn.getResponseCode();
                InputStream stream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                Scanner scanner = new Scanner(stream).useDelimiter("\\A");
                String responseStr = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                JSONObject jsonResponse = new JSONObject(responseStr);

                Platform.runLater(() -> {
                    btnUpgrade.setDisable(false);
                    if (responseCode >= 200 && responseCode < 300 && jsonResponse.optInt("status", 400) == 200) {
                        // Success
                        User.setSession(User.getId(), User.getUsername(), User.getFullname(), User.getEmail(), User.getDob(), User.getPlace_of_birth(), "seller", User.getAvatarUrl());
                        AlertUtil.showInfo("Upgrade successful", "Congratulations, you are now a Seller!\nSelling features are now unlocked. You can now list your own products.");

                        try {
                            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                        } catch (Exception ex) {
                            logger.error("Error switching scenes after upgrade: ", ex);
                        }
                    } else {
                        showError(jsonResponse.optString("message", "Upgrade failed or account is already a SELLER!"));
                    }
                });

            } catch (Exception e) {
                logger.error("Error when upgrading to seller: ", e);
                Platform.runLater(() -> {
                    btnUpgrade.setDisable(false);
                    showError("Server connection error during upgrade!");
                });
            }
        }).start();
    }

    @FXML
    public void handleGoBack(MouseEvent event) {
        try {
            ActionEvent actionEvent = new ActionEvent(event.getSource(), event.getTarget());
            SceneSwitcher.switchScene(actionEvent, "MainTemplate.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Error going back to main page: ", e);
        }
    }

    @FXML
    public void handleGoBack(ActionEvent event) {
        try {
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
        } catch (Exception e) {
            logger.error("Error going back to main page: ", e);
        }
    }

    private void showError(String message) {
        lblMessage.setText(message);
        lblMessage.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}