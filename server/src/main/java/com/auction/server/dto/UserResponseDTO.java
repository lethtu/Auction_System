package com.auction.server.dto;

import java.math.BigDecimal;

public final class UserResponseDTO {

    private Integer id;
    private String username;
    private String fullname;
    private String email;
    private String accountType;
    private BigDecimal balance;
    private boolean banned;

    public UserResponseDTO() {
    }

    public UserResponseDTO(
            Integer id,
            String username,
            String fullname,
            String email,
            String accountType,
            BigDecimal balance,
            boolean banned
    ) {
        this.id = id;
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.accountType = accountType;
        this.balance = balance;
        this.banned = banned;
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
        this.username = username;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}