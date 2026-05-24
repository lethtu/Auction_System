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
import com.auction.client.service.AppStyleManager;
import javafx.scene.image.Image;
import java.util.Objects;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Client started successfully");
        Parent root = FXMLLoader
                .load(Objects.requireNonNull(getClass().getResource("/com/auction/client/view/Login.fxml")));
        primaryStage.setTitle("Auction System");
        try {
            primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream(Config.LOGO_PATH))));
        } catch (Exception e) {
            logger.error("Failed to load application icon", e);
        }
        Scene scene = new Scene(root, 1000, 700);
        scene.setFill(Color.TRANSPARENT);
        AppStyleManager.applyCurrentStyle(scene);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);

        primaryStage.setMinWidth(Config.Width);
        primaryStage.setMinHeight(Config.Height);
        primaryStage.setResizable(false);
        ResizeHelper.install(primaryStage, root);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Client stopping...");
        try {
            com.auction.client.service.NotificationSocketService.getInstance().stop();
        } catch (Exception e) {
            // Ignore
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}