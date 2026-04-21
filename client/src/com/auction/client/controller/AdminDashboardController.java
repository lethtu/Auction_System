package com.auction.client.controller;

import com.auction.client.model.AuctionSession;
import com.auction.client.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.net.URI;
import java.net.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminDashboardController {

    @FXML private TableView<AuctionSession> tablePending;
    @FXML private TableColumn<AuctionSession, Integer> colId;
    @FXML private TableColumn<AuctionSession, String> colProduct;
    @FXML private TableColumn<AuctionSession, Double> colPrice;

    private final ObservableList<AuctionSession> pendingData = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colProduct.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        colPrice.setCellValueFactory(cellData -> cellData.getValue().startingPriceProperty().asObject());

        loadPendingSessions();
    }

    private void loadPendingSessions() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/admin/pending"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray jsonArray = new JSONArray(response.body());
                pendingData.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    // Lấy tên sản phẩm từ vật thể lồng bên trong
                    String pName = obj.getJSONObject("product").getString("name");
                    pendingData.add(new AuctionSession(
                            obj.getInt("id"), pName, obj.getDouble("startingPrice"), obj.getString("status")
                    ));
                }
                tablePending.setItems(pendingData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleApprove() {
        AuctionSession selected = tablePending.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int adminId = User.getId();

        try {
            String url = String.format("http://localhost:8080/api/admin/approve/%d?adminId=%d", selected.getId(), adminId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Duyệt thành công!");
                loadPendingSessions();
            } else {
                System.out.println("Lỗi: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}