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
import javafx.scene.control.MenuItem;


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

        Stage stage = null;
        Object source = event.getSource();

        if (source instanceof Node) {
            // Trường hợp 1: Chuyển cảnh từ Button, Label, AnchorPane...
            stage = (Stage) ((Node) source).getScene().getWindow();
        } else if (source instanceof MenuItem) {
            // Trường hợp 2: Chuyển cảnh từ menu xổ xuống (Dropdown Menu)
            MenuItem menuItem = (MenuItem) source;
            stage = (Stage) menuItem.getParentPopup().getOwnerWindow();
        }

        // Bắt lỗi nếu gọi hàm từ một nguồn không hợp lệ
        if (stage == null) {
            throw new RuntimeException("Không thể xác định được Stage hiện tại để chuyển cảnh!");
        }

        Scene scene;
        if (width == null || height == null) {
            scene = new Scene(root);
        } else {
            scene = new Scene(root, width, height);
        }

        String cssPath = "/com/auction/client/controller/style.css";
        URL cssResource = SceneSwitcher.class.getResource(cssPath);
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        stage.setScene(scene);

        if (width != null) {
            stage.setMinWidth(width);
            if (Double.isNaN(stage.getWidth()) || stage.getWidth() < width) {
                stage.setWidth(width);
            }
        }
        if (height != null) {
            stage.setMinHeight(height);
            if (Double.isNaN(stage.getHeight()) || stage.getHeight() < height) {
                stage.setHeight(height);
            }
        }
        stage.setResizable(true);
        stage.show();
        stage.centerOnScreen();
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