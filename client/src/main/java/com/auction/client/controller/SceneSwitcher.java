package com.auction.client.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;


public class SceneSwitcher {
    private static final Logger logger = LoggerFactory.getLogger(SceneSwitcher.class);

    public static FXMLLoader Switch(ActionEvent event, String fxmlFile, Integer width, Integer height) throws IOException {
        String path = "/com/auction/client/view/" + fxmlFile;
        URL xmlResource = SceneSwitcher.class.getResource(path);

        if (xmlResource == null) {
            throw new RuntimeException("Không tìm thấy file FXML: " + path);
        }

        FXMLLoader loader = new FXMLLoader(xmlResource);
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene;
        if (width == null || height == null) {
            scene = new Scene(root);
        } else {
            scene = new Scene(root, width, height);
        }

        stage.setScene(scene);

        if (width != null) {
            stage.setMinWidth(width);
            stage.setWidth(width);
        }
        if (height != null) {
            stage.setMinHeight(height);
            stage.setHeight(height);
        }
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
        logger.info("Đang chuyển sang: {}", fxmlFile);

        return loader;
    }

    public static FXMLLoader switchScene(ActionEvent event, String fxmlFile) throws IOException {
        return Switch(event, fxmlFile, null, null);
    }

    public static FXMLLoader switchScene(ActionEvent event, String fxmlFile, Integer width, Integer height) throws IOException {
        return Switch(event, fxmlFile, width, height   );
    }
}