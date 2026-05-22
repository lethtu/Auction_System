package com.auction.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import com.auction.client.util.ResizeHelper;
import java.util.Objects;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Đã khởi động Client thành công");
        Parent root = FXMLLoader
                .load(Objects.requireNonNull(getClass().getResource("/com/auction/client/view/Login.fxml")));
        primaryStage.setTitle("Hệ thống đấu giá");
        Scene scene = new Scene(root, 1100, 700);
        scene.setFill(Color.TRANSPARENT);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setResizable(true);
        ResizeHelper.install(primaryStage, root);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}