package com.auction.client.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AdminUserRow {
    private final IntegerProperty id;
    private final StringProperty username;
    private final StringProperty fullname;
    private final StringProperty email;
    private final StringProperty role;
    private final BooleanProperty banned;

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

    public String getUsername() {
        return username.get();
    }

    public String getFullname() {
        return fullname.get();
    }

    public String getEmail() {
        return email.get();
    }

    public String getRole() {
        return role.get();
    }

    public boolean isBanned() {
        return banned.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty fullnameProperty() {
        return fullname;
    }

    public StringProperty emailProperty() {
        return email;
    }

    public StringProperty roleProperty() {
        return role;
    }

    public BooleanProperty bannedProperty() {
        return banned;
    }
}