package com.auction.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("vehicle")
public class Vehicle extends Item {

    @Override
    public String getCategoryInfo() {
        return "Vehicle - phương tiện xe cộ";
    }
}
