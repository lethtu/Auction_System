module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    requires java.net.http;
    opens com.auction.client to javafx.graphics, javafx.fxml;
    opens com.auction.client.controller to javafx.fxml;
    exports com.auction.client;
}