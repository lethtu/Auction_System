package com.auction.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SELLER")
public class Seller extends User {

    private String shopName; // Tên gian hàng (ai cũng có)
    private Boolean isBusiness; // True nếu là doanh nghiệp, False nếu là cá nhân
    private String taxId; // Mã số thuế (Chỉ cần điền nếu isBusiness = true)

    public Seller() {
    }

    // Getter và Setter
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public Boolean getIsBusiness() { return isBusiness; }
    public void setIsBusiness(Boolean isBusiness) { this.isBusiness = isBusiness; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
}