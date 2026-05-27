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
import com.auction.client.util.TermsDialog;

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
        if (chkAgree != null) {
            chkAgree.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                if (!chkAgree.isSelected()) {
                    event.consume();
                    showTermsAndCheck(chkAgree, false);
                }
            });
            chkAgree.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.SPACE && !chkAgree.isSelected()) {
                    event.consume();
                    showTermsAndCheck(chkAgree, false);
                }
            });
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

        Thread upgradeThread = new Thread(() -> {
            try {
                String apiUrl = Config.API_URL + "/api/bidder/up-to-seller?userId=" + User.getId();
                HttpURLConnection conn = (HttpURLConnection) java.net.URI.create(apiUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                if (com.auction.client.model.User.getSessionToken() != null) {
                    conn.setRequestProperty("X-Auth-Token", com.auction.client.model.User.getSessionToken());
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
                        User.setSession(User.getId(), User.getUsername(), User.getFullname(), User.getEmail(), User.getDob(), User.getPlace_of_birth(), "SELLER", User.getAvatarUrl());
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
        }, "up-to-seller-worker");
        upgradeThread.setDaemon(true);
        upgradeThread.start();
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

    private void showTermsAndCheck(javafx.scene.control.CheckBox checkBox, boolean isSignUp) {
        String title, subtitle, termsText;
        if (isSignUp) {
            title = "Terms & Conditions";
            subtitle = "Please read and scroll to the bottom to accept the BidPop terms.";
            termsText = "BidPop Marketplace Terms & Conditions\n\n"
                    + "Welcome to BidPop! By creating an account, you agree to comply with and be bound by the following terms of service. Please review them carefully.\n\n"
                    + "1. Acceptance of Terms\n"
                    + "By accessing or using BidPop, you agree to be bound by these Terms. If you do not agree, please do not use the application.\n\n"
                    + "2. Account Registration\n"
                    + "You must provide accurate and complete information during registration. You are responsible for keeping your credentials secure and confidential.\n\n"
                    + "3. User Conduct\n"
                    + "You agree not to engage in any prohibited activities, including bid shilling, fraud, or violating laws and rights of others.\n\n"
                    + "4. Termination\n"
                    + "We reserve the right to suspend or terminate accounts that violate our policies or engage in disruptive behavior.\n\n"
                    + "5. Liability & Disclaimers\n"
                    + "BidPop is provided 'as is' without warranties. We are not liable for transaction disputes between buyers and sellers.\n\n"
                    + "Please scroll to the bottom to confirm you have read and accepted these terms.";
        } else {
            title = "Seller Policy Agreement";
            subtitle = "Read the merchant terms before selling on BidPop.";
            termsText = "BidPop Seller Terms & Policies\n\n"
                    + "Welcome to the BidPop Seller Program! To upgrade your account and start selling, you must agree to these Seller Policies.\n\n"
                    + "1. Product Listings\n"
                    + "Sellers must describe their items accurately. Listing illegal, counterfeit, or prohibited items is strictly forbidden.\n\n"
                    + "2. Auction Rules\n"
                    + "Sellers agree to fulfill orders at the final winning bid. Shill bidding or manipulation is subject to immediate ban.\n\n"
                    + "3. Fees & Commissions\n"
                    + "BidPop charges fees on completed auctions. Detailed fee schedules are published under the Help Center.\n\n"
                    + "4. Dispute Resolution\n"
                    + "Sellers must cooperate with our support team to resolve buyer complaints. Failure to do so will affect your seller rating.\n\n"
                    + "Please scroll to the bottom to confirm you have read and accepted the seller policies.";
        }

        boolean accepted = TermsDialog.show(checkBox.getScene().getWindow(), title, subtitle, termsText);
        if (accepted) {
            checkBox.setSelected(true);
        } else {
            checkBox.setSelected(false);
        }
    }
}