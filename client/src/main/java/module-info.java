module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires org.json;
    requires java.net.http;
    requires org.slf4j;

    opens com.auction.client.controller to javafx.fxml, javafx.base;
    opens com.auction.client.model to javafx.base;

    exports com.auction.client;
}