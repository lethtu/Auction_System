package com.auction.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("art")
public class Art extends Item {
    // Specific fields for art can be added here
}
