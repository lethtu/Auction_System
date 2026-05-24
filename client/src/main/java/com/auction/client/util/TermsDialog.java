package com.auction.client.util;

import com.auction.client.service.AppStyleManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class TermsDialog {
    private static boolean accepted;

    private TermsDialog() {
    }

    public static boolean show(Window owner, String title, String subtitle, String termsText) {
        accepted = false;

        Stage stage = new Stage(StageStyle.TRANSPARENT);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(16));

        VBox card = new VBox();
        card.setPrefWidth(540);
        card.setMaxWidth(540);
        card.setPrefHeight(600);
        card.setMaxHeight(600);
        card.setStyle(
                "-fx-background-color: -app-card;" +
                "-fx-border-color: -app-accent-opacity-38;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 24px;" +
                "-fx-background-radius: 24px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.40), 18, 0, 0, 8);" +
                "-fx-font-family: 'DM Sans';"
        );

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24, 28, 18, 28));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, -app-accent-opacity-08, transparent);" +
                "-fx-border-color: -app-accent-opacity-15;" +
                "-fx-border-width: 0 0 1px 0;" +
                "-fx-background-radius: 22px 22px 0 0;"
        );

        double[] dragDelta = new double[2];
        header.setOnMousePressed(e -> {
            dragDelta[0] = stage.getX() - e.getScreenX();
            dragDelta[1] = stage.getY() - e.getScreenY();
        });
        header.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + dragDelta[0]);
            stage.setY(e.getScreenY() + dragDelta[1]);
        });

        Label icon = new Label("!" );
        icon.setStyle(
                "-fx-background-color: -fx-accent;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: 900;" +
                "-fx-alignment: center;" +
                "-fx-min-width: 34px;" +
                "-fx-min-height: 34px;" +
                "-fx-background-radius: 17px;"
        );

        VBox titleBox = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 21px; -fx-font-weight: 900; -fx-text-fill: -app-text;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("X");
        close.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: -app-text-muted;" +
                "-fx-font-weight: 900;" +
                "-fx-font-size: 14px;" +
                "-fx-cursor: hand;" +
                "-fx-background-radius: 999px;" +
                "-fx-padding: 6px 10px;"
        );
        close.setOnAction(e -> stage.close());
        header.getChildren().addAll(icon, titleBox, spacer, close);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Label content = new Label(termsText);
        content.setWrapText(true);
        content.setPadding(new Insets(22, 28, 22, 28));
        content.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text; -fx-line-spacing: 4px;");
        scrollPane.setContent(content);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(18, 28, 24, 28));
        footer.setStyle("-fx-border-color: -app-accent-opacity-15; -fx-border-width: 1px 0 0 0;");

        Button accept = new Button("Accept & Continue");
        accept.setMaxWidth(Double.MAX_VALUE);
        accept.setPrefHeight(44);
        HBox.setHgrow(accept, Priority.ALWAYS);

        String disabledStyle = "-fx-background-color: -app-surface-2; -fx-text-fill: -app-text-muted; -fx-font-weight: 900; -fx-background-radius: 999px; -fx-cursor: default;";
        String enabledStyle = "-fx-background-color: -fx-accent; -fx-text-fill: white; -fx-font-weight: 900; -fx-background-radius: 999px; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: -app-accent-hover; -fx-text-fill: white; -fx-font-weight: 900; -fx-background-radius: 999px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, -app-accent-opacity-30, 12, 0, 0, 3);";

        accept.setDisable(true);
        accept.setStyle(disabledStyle);
        accept.disabledProperty().addListener((obs, oldValue, disabled) -> accept.setStyle(disabled ? disabledStyle : enabledStyle));
        accept.setOnMouseEntered(e -> {
            if (!accept.isDisable()) {
                accept.setStyle(hoverStyle);
            }
        });
        accept.setOnMouseExited(e -> {
            if (!accept.isDisable()) {
                accept.setStyle(enabledStyle);
            }
        });
        accept.setOnAction(e -> {
            accepted = true;
            stage.close();
        });

        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 0.96) {
                accept.setDisable(false);
            }
        });
        Platform.runLater(() -> {
            ScrollBar verticalBar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
            if (verticalBar == null || !verticalBar.isVisible()) {
                accept.setDisable(false);
            }
        });

        footer.getChildren().add(accept);
        card.getChildren().addAll(header, scrollPane, footer);
        root.getChildren().add(card);

        Scene scene = new Scene(root, Color.TRANSPARENT);
        java.net.URL globalCss = TermsDialog.class.getResource("/com/auction/client/view/styles.css");
        if (globalCss != null) {
            scene.getStylesheets().add(globalCss.toExternalForm());
        }
        java.net.URL authCss = TermsDialog.class.getResource("/com/auction/client/css/auth-theme.css");
        if (authCss != null) {
            scene.getStylesheets().add(authCss.toExternalForm());
        }
        AppStyleManager.applyCurrentStyle(scene.getRoot());

        stage.setScene(scene);
        stage.showAndWait();
        return accepted;
    }
}