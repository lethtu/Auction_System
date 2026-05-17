package com.auction.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("USER")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    private String place_of_birth;
    private BigDecimal balance = BigDecimal.ZERO;

    public User() {}

    // Các Getter và Setter
    public void setId(Integer id) { this.id = id; }
    public Integer getId() { return id; }

    public void setUsername(String username) { this.username = username; }
    public String getUsername() { return username; }

    public void setPassword(String password) { this.password = password; }
    public String getPassword() { return password; }

    public void setFullname(String fullname) { this.fullname = fullname; }
    public String getFullname() { return fullname; }

    public void setEmail(String email) { this.email = email; }
    public String getEmail() { return email; }

    public void setDob(String dob) { this.dob = dob; }
    public String getDob() { return dob; }

    public void setPlace_of_birth(String place_of_birth) { this.place_of_birth = place_of_birth; }
    public String getPlace_of_birth() { return place_of_birth; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getBalance() { return balance; }

    /**
     * Trả về loại tài khoản dựa trên discriminator (class thực tế).
     * Không lưu riêng vào DB vì @DiscriminatorColumn "role" đã quản lý.
     */
    @Transient
    public String getAccountType() {
        if (this instanceof Bidder) return "BIDDER";
        if (this instanceof Seller) return "SELLER";
        if (this instanceof Admin) return "admin";
        return "USER";
    }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", username='" + username + '\'' + ", role='" + getAccountType() + '\'' + '}';
    }
}