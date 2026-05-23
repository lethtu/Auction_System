module com.auction.client {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.media;

    requires transitive org.json;
    requires transitive java.net.http;
    requires transitive org.slf4j;
    requires transitive org.kordamp.ikonli.core;
    requires transitive org.kordamp.ikonli.javafx;
    requires transitive org.kordamp.ikonli.materialdesign2;
    requires java.prefs;
    requires jdk.httpserver;
    requires java.desktop;

    opens com.auction.client.model to javafx.base;
    opens com.auction.client.controller to javafx.fxml;

    exports com.auction.client;
    exports com.auction.client.controller;
    exports com.auction.client.model;
    exports com.auction.client.dto;
}
