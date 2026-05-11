package com.auction.client.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class AdminUserRow {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty username;
    private final SimpleStringProperty fullname;
    private final SimpleStringProperty email;
    private final SimpleStringProperty role;
    private final SimpleBooleanProperty banned;

    public AdminUserRow(int id, String username, String fullname, String email, String role, boolean banned) {
        this.id = new SimpleIntegerProperty(id);
        this.username = new SimpleStringProperty(username);
        this.fullname = new SimpleStringProperty(fullname);
        this.email = new SimpleStringProperty(email);
        this.role = new SimpleStringProperty(role);
        this.banned = new SimpleBooleanProperty(banned);
    }

    public int getId() {
        return id.get();
    }

    public boolean isBanned() {
        return banned.get();
    }

    public SimpleIntegerProperty idProperty() {
        return id;
    }

    public SimpleStringProperty usernameProperty() {
        return username;
    }

    public SimpleStringProperty fullnameProperty() {
        return fullname;
    }

    public SimpleStringProperty emailProperty() {
        return email;
    }

    public SimpleStringProperty roleProperty() {
        return role;
    }

    public SimpleBooleanProperty bannedProperty() {
        return banned;
    }
}
