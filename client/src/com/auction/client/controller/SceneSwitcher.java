package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

public class SceneSwitcher {
    public static void switchScene(ActionEvent event, String fxmlFile, double width, double height) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(SceneSwitcher.class.getResource("/com/auction/client/view/"+fxmlFile)));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}