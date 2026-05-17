package com.auction.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("electronics")
public class Electronics extends Item {

    @Override
    public String getCategoryInfo() {
        return "Electronics - sản phẩm công nghệ/điện tử";
    }
}
