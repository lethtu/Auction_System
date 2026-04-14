package com.auction.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("electronics")
public class Electronics extends Item {
    // Specific fields for electronics can be added here
}
