package com.auction.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Client started successfully");
        Parent root = FXMLLoader
                .load(Objects.requireNonNull(getClass().getResource("/com/auction/client/view/Login.fxml")));
        primaryStage.setTitle("Auction System");
        primaryStage.setScene(new Scene(root, 1100, 700));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}