package com.auction.server.model;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;
    private String password;
    private String fullname;
    private String email;
    private LocalDate dob;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    @Column(name = "role", insertable = false, updatable = false)
    private String role;

    private Double balance = 0.0;

    public User() {}

    public void setId(Integer id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFullname(String fullname) { this.fullname = fullname; }
    public void setEmail(String email) { this.email = email; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public void setPlaceOfBirth(String placeOfBirth) { this.placeOfBirth = placeOfBirth; }
    public void setBalance(Double balance) { this.balance = balance; }

    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullname() { return fullname; }
    public String getEmail() { return email; }
    public LocalDate getDob() { return dob; }
    public String getPlaceOfBirth() { return placeOfBirth; }
    public String getRole() { return role; }
    public Double getBalance() { return balance; }

    @Override
    public String toString(){
        return id + " " + fullname + " " + email + " " + dob + " " + placeOfBirth + " " + username ;
    }
}