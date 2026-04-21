package com.auction.client.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class SellerDashboardController {

    @FXML
    private ListView<String> mySessionsList;

    @FXML
    private ComboBox<String> productTypeCombo;

    @FXML
    private TextField productNameField;

    @FXML
    private TextField imageUrlField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField startingPriceField;

    @FXML
    private TextField stepPriceField;

    @FXML
    private TextField endTimeField;

    @FXML
    private TextArea statsArea;

    @FXML
    public void initialize() {
        productTypeCombo.setItems(FXCollections.observableArrayList(
                "electronics", "art", "vehicle"
        ));
        productTypeCombo.setValue("electronics");

        mySessionsList.setItems(FXCollections.observableArrayList(
                "Session #1 | Laptop Dell XPS 13 | ACTIVE",
                "Session #2 | Tranh sơn dầu | PENDING",
                "Session #3 | Xe máy cũ | REJECTED"
        ));

        statsArea.setText("""
                Tổng số phiên hoàn thành: 2
                Tổng doanh thu: 32,500,000 VND
                Số phiên đang chờ duyệt: 1
                Số phiên đang hoạt động: 1
                """);
    }

    @FXML
    private void handleCreateSession() {
        String name = productNameField.getText().trim();
        String type = productTypeCombo.getValue();
        String price = startingPriceField.getText().trim();

        if (name.isEmpty() || type == null || price.isEmpty()) {
            showAlert("Thiếu dữ liệu", "Bạn cần nhập tối thiểu tên sản phẩm, loại và giá khởi điểm.");
            return;
        }

        mySessionsList.getItems().add(0, "Session mới | " + name + " | PENDING");

        productNameField.clear();
        imageUrlField.clear();
        descriptionArea.clear();
        startingPriceField.clear();
        stepPriceField.clear();
        endTimeField.clear();

        showAlert("Thành công", "Đã tạo khung phiên đấu giá mới trên UI.");
    }

    @FXML
    private void handleShowAllSessions() {
        mySessionsList.setItems(FXCollections.observableArrayList(
                "Session #1 | Laptop Dell XPS 13 | ACTIVE",
                "Session #2 | Tranh sơn dầu | PENDING",
                "Session #3 | Xe máy cũ | REJECTED"
        ));
    }

    @FXML
    private void handleShowPendingSessions() {
        mySessionsList.setItems(FXCollections.observableArrayList(
                "Session #2 | Tranh sơn dầu | PENDING"
        ));
    }

    @FXML
    private void handleShowActiveSessions() {
        mySessionsList.setItems(FXCollections.observableArrayList(
                "Session #1 | Laptop Dell XPS 13 | ACTIVE"
        ));
    }

    @FXML
    private void handleShowRejectedSessions() {
        mySessionsList.setItems(FXCollections.observableArrayList(
                "Session #3 | Xe máy cũ | REJECTED"
        ));
    }

    @FXML
    private void handleEditSelectedSession() {
        String selected = mySessionsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn phiên", "Bạn hãy chọn một phiên để sửa.");
            return;
        }
        showAlert("Sửa phiên", "UI demo: mở form sửa cho:\n" + selected);
    }

    @FXML
    private void handleCancelSelectedSession() {
        String selected = mySessionsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn phiên", "Bạn hãy chọn một phiên để hủy.");
            return;
        }
        mySessionsList.getItems().remove(selected);
        showAlert("Đã hủy", "UI demo: đã hủy phiên được chọn.");
    }

    @FXML
    private void goBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/MainTemplate.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Main Menu");
        stage.setScene(new Scene(root, 1000, 650));
        stage.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}