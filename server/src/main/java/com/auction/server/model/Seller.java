package com.auction.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SELLER")
public class Seller extends User {

    private String shopName;
    private Boolean isBusiness;
    private String taxId;

    public Seller() {
    }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public Boolean getIsBusiness() { return isBusiness; }
    public void setIsBusiness(Boolean isBusiness) { this.isBusiness = isBusiness; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
}