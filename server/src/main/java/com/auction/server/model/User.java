package com.auction.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Locale;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("user")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    private static final String BIDDER_ROLE = "bidder";
    private static final String SELLER_ROLE = "seller";
    private static final String ADMIN_ROLE = "admin";
    private static final String DEFAULT_ROLE = "user";

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

    @Column(name = "frozen_balance")
    private BigDecimal frozenBalance = BigDecimal.ZERO;

    @Column(name = "role", insertable = false, updatable = false)
    private String accountType;

    @Column(nullable = false)
    private boolean banned = false;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "password_set")
    private Boolean passwordSet = true;

    @Transient
    private String sessionToken;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getRole() {
        return getAccountType();
    }

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

    public BigDecimal getFrozenBalance() {
        return frozenBalance == null ? BigDecimal.ZERO : frozenBalance;
    }

    public void setFrozenBalance(BigDecimal frozenBalance) {
        this.frozenBalance = frozenBalance == null ? BigDecimal.ZERO : frozenBalance;
    }

    public BigDecimal getAvailableBalance() {
        return getBalance().subtract(getFrozenBalance());
    }

    public String getAccountType() {
        if (accountType != null && !accountType.isBlank()) {
            return normalizeRole(accountType);
        }

        if (this instanceof Bidder) {
            return BIDDER_ROLE;
        }

        if (this instanceof Seller) {
            return SELLER_ROLE;
        }

        if (this instanceof Admin) {
            return ADMIN_ROLE;
        }

        return DEFAULT_ROLE;
    }

    public void setAccountType(String accountType) {
        this.accountType = normalizeRole(accountType);
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getPasswordSet() {
        return passwordSet == null || passwordSet;
    }

    public void setPasswordSet(Boolean passwordSet) {
        this.passwordSet = passwordSet;
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

    private String normalizeRole(String role) {
        String normalizedRole = trimToNull(role);
        return normalizedRole == null ? DEFAULT_ROLE : normalizedRole.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}