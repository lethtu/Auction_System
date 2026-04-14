package com.auction.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("vehicle")
public class Vehicle extends Item {
    // Specific fields for vehicle can be added here
}
