package com.auction.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import com.auction.client.util.ResizeHelper;
import com.auction.client.service.AppStyleManager;
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
            java.io.InputStream iconStream = getClass().getResourceAsStream(Config.LOGO_PATH);
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            } else {
                logger.warn("Application icon not found: {}", Config.LOGO_PATH);
            }
        } catch (Exception e) {
            logger.warn("Cannot load application icon", e);
        }
Scene scene = new Scene(root, 1000, 700);
        scene.setFill(Color.TRANSPARENT);
        // Auth screens stay light, but still sync the selected accent color.
        AppStyleManager.applyAuthAccentStyle(scene);
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
        try {
            com.auction.client.service.NotificationSocketService.getInstance().stop();
        } catch (Exception ignored) {
        }
        super.stop();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
