package com.auction.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("USER")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String fullname;
    private String email;
    private String dob;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "role", insertable = false, updatable = false)
    private String accountType;

    @Column(nullable = false)
    private boolean banned = false;

    public User() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = trimToNull(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = trimToNull(fullname);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = trimToNull(email);
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = trimToNull(dob);
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = trimToNull(placeOfBirth);
    }

    public String getPlace_of_birth() {
        return placeOfBirth;
    }

    public void setPlace_of_birth(String placeOfBirth) {
        this.placeOfBirth = trimToNull(placeOfBirth);
    }

    public BigDecimal getBalance() {
        return balance == null ? BigDecimal.ZERO : balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance == null ? BigDecimal.ZERO : balance;
    }

    public String getAccountType() {
        if (accountType != null && !accountType.isBlank()) {
            return accountType;
        }

        if (this instanceof Bidder) {
            return "BIDDER";
        }

        if (this instanceof Seller) {
            return "SELLER";
        }

        if (this instanceof Admin) {
            return "admin";
        }

        return "USER";
    }

    public void setAccountType(String accountType) {
        this.accountType = trimToNull(accountType);
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", username='" + username + '\''
                + ", role='" + getAccountType() + '\''
                + ", banned=" + banned
                + '}';
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}