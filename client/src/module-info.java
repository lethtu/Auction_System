module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires org.json;
    requires java.net.http;

    opens com.auction.client.model to javafx.base;
    opens com.auction.client.controller to javafx.fxml;

    exports com.auction.client;
}