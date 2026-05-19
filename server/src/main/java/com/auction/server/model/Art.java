package com.auction.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("art")
public class Art extends Item {

    @Override
    public String getCategoryInfo() {
        return "Art - tác phẩm nghệ thuật/sưu tầm";
    }
}
