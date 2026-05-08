package com.auction.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;
    @JsonIgnore
    private String password;
    private String fullname;
    private String email;
    private String dob;
    private String place_of_birth;
    private BigDecimal balance = BigDecimal.ZERO;
    @Column(name = "role", insertable = false, updatable = false)
    private String accountType;

    public User() {}

    public void setId(Integer id) { this.id = id; }

    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) { this.password = password; }

    public void setFullname(String fullname) { this.fullname = fullname; }

    public void setEmail(String email) { this.email = email; }

    public void setDob(String dob) { this.dob = dob; }

    public void setPlace_of_birth(String place_of_birth) { this.place_of_birth = place_of_birth; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Integer getId() { return id; }
    public String getFullname() { return fullname; }
    public String getEmail() { return email; }
    public String getDob() { return dob; }
    public String getPlace_of_birth() { return place_of_birth; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }

    @Override
    public String toString(){
        return id + " " + fullname + " " + email + " " + dob + " " + place_of_birth + " " + username + " " + password;
    }
}