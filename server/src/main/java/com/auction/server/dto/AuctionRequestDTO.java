package com.auction.server.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class AuctionRequestDTO {
    private String productName;
    private String productType;
    private String imageUrl;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal stepPrice;
    private LocalDateTime endTime;
    private Integer sellerId;

    public AuctionRequestDTO() {
    }

    public AuctionRequestDTO(String productName, String productType, String imageUrl, String description,
                             BigDecimal startingPrice, BigDecimal stepPrice, LocalDateTime endTime, Integer sellerId) {
        this.productName = productName;
        this.productType = productType;
        this.imageUrl = imageUrl;
        this.description = description;
        this.startingPrice = startingPrice;
        this.stepPrice = stepPrice;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public BigDecimal getStepPrice() { return stepPrice; }
    public void setStepPrice(BigDecimal stepPrice) { this.stepPrice = stepPrice; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }
}