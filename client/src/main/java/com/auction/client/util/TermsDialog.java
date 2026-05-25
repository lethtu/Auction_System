package com.auction.client.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import com.auction.client.service.AppStyleManager;

public final class TermsDialog {

    private static boolean accepted = false;

    private TermsDialog() {}

    public static boolean show(Window owner, String title, String subtitle, String termsText) {
        accepted = false;
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);

        // Main Dialog Pane (Outer wrapper stackpane to hold drop shadow and outer card)
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(16)); // Space for drop shadow

        VBox card = new VBox();
        card.setPrefWidth(520);
        card.setMaxWidth(520);
        card.setPrefHeight(600);
        card.setMaxHeight(600);
        card.setStyle(
                "-fx-background-color: -app-card;" +
                " -fx-border-color: -app-accent-opacity-38;" +
                " -fx-border-width: 2px;" +
                " -fx-border-radius: 24px;" +
                " -fx-background-radius: 24px;" +
                " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 16, 0, 0, 8);" +
                " -fx-font-family: 'DM Sans';"
        );

        // Header Section (Draggable)
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24, 28, 20, 28));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, -app-accent-opacity-05, transparent);" +
                " -fx-border-color: -app-accent-opacity-15;" +
                " -fx-border-width: 0 0 1px 0;" +
                " -fx-background-radius: 22px 22px 0 0;"
        );

        // Draggable window logic
        double[] dragDelta = new double[2];
        header.setOnMousePressed(e -> {
            dragDelta[0] = stage.getX() - e.getScreenX();
            dragDelta[1] = stage.getY() - e.getScreenY();
        });
        header.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + dragDelta[0]);
            stage.setY(e.getScreenY() + dragDelta[1]);
        });

        Label docIcon = new Label("\uE873"); // Document icon in Material Symbols
        docIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 32px; -fx-text-fill: -fx-accent;");

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -app-text;");
        Label descLabel = new Label(subtitle);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");
        titleBox.getChildren().addAll(titleLabel, descLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Custom Close Button (X) at Top Right
        Button btnClose = new Button("\uE5CD");
        btnClose.setStyle(
                "-fx-font-family: 'Material Symbols Outlined';" +
                " -fx-font-size: 18px;" +
                " -fx-text-fill: -app-text-muted;" +
                " -fx-background-color: transparent;" +
                " -fx-cursor: hand;" +
                " -fx-padding: 4px;" +
                " -fx-background-radius: 50%;"
        );
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: #ef4444; -fx-background-color: rgba(239, 68, 68, 0.1); -fx-cursor: hand; -fx-padding: 4px; -fx-background-radius: 50%;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 18px; -fx-text-fill: -app-text-muted; -fx-background-color: transparent; -fx-padding: 4px; -fx-background-radius: 50%;"));
        btnClose.setOnAction(e -> stage.close());

        header.getChildren().addAll(docIcon, titleBox, spacer, btnClose);

        // Content Area (Terms Text wrapped in a styled ScrollPane)
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Label lblTerms = new Label(termsText);
        lblTerms.setWrapText(true);
        lblTerms.setPadding(new Insets(24, 28, 24, 28));
        lblTerms.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text; -fx-line-spacing: 4px;");
        scrollPane.setContent(lblTerms);

        // Footer Section
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20, 28, 24, 28));
        footer.setStyle("-fx-border-color: -app-accent-opacity-15; -fx-border-width: 1px 0 0 0;");

        Button btnAccept = new Button("Accept & Continue");
        btnAccept.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnAccept, Priority.ALWAYS);
        btnAccept.setPrefHeight(44);
        btnAccept.setDisable(true); // Initially disabled

        // Button styling
        String baseBtnStyle = "-fx-background-color: -fx-accent; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 22px; -fx-cursor: hand; -fx-font-size: 14px;";
        String hoverBtnStyle = "-fx-background-color: -app-accent-hover; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 22px; -fx-cursor: hand; -fx-font-size: 14px; -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-25, 8, 0, 0, 2);";
        String disableBtnStyle = "-fx-background-color: -app-surface-2; -fx-text-fill: -app-text-muted; -fx-font-weight: bold; -fx-background-radius: 22px; -fx-cursor: not-allowed; -fx-font-size: 14px;";

        btnAccept.setStyle(disableBtnStyle);
        btnAccept.disabledProperty().addListener((obs, wasDisabled, isDisabled) -> {
            if (isDisabled) {
                btnAccept.setStyle(disableBtnStyle);
            } else {
                btnAccept.setStyle(baseBtnStyle);
            }
        });

        btnAccept.setOnMouseEntered(e -> {
            if (!btnAccept.isDisable()) {
                btnAccept.setStyle(hoverBtnStyle);
            }
        });
        btnAccept.setOnMouseExited(e -> {
            if (!btnAccept.isDisable()) {
                btnAccept.setStyle(baseBtnStyle);
            }
        });

        btnAccept.setOnAction(e -> {
            accepted = true;
            stage.close();
        });

        footer.getChildren().add(btnAccept);

        // Scroll progress listener to enable Accept button
        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 0.96) {
                btnAccept.setDisable(false);
            }
        });

        // Safe fallback check: if text fits completely without scrolling, enable button immediately after layout
        scrollPane.needsLayoutProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                ScrollBar bar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
                if (bar == null || !bar.isVisible()) {
                    btnAccept.setDisable(false);
                }
            }
        });

        card.getChildren().addAll(header, scrollPane, footer);
        root.getChildren().add(card);

        Scene scene = new Scene(root, Color.TRANSPARENT);
        // Load stylesheets
        java.net.URL stylesUrl = TermsDialog.class.getResource("/com/auction/client/view/styles.css");
        if (stylesUrl != null) {
            scene.getStylesheets().add(stylesUrl.toExternalForm());
        }
        AppStyleManager.applyCurrentStyle(scene.getRoot());

        stage.setScene(scene);
        stage.showAndWait();

        return accepted;
    }
}
