package com.auction.server.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("admin")
public class Admin extends User {
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_role", nullable = false, length = 50)
    private AdminRole role;
    @Column(name = "employee_code", unique = true, nullable = false, length = 20)
    private String employeeCode;
    public Admin() {
        super();
    }

    public Admin(AdminRole role, String employeeCode) {
        super();
        this.role = role;
        this.employeeCode = employeeCode;
    }
    public AdminRole getRole() { return role; }
    public void setRole(AdminRole role) { this.role = role; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
}