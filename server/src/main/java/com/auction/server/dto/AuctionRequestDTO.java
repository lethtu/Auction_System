package com.auction.server.dto;

import java.time.LocalDateTime;

public class AuctionRequestDTO {
    private String productName;
    private String productType;
    private String imageUrl;
    private String description;
    private Double startingPrice;
    private Double stepPrice;
    private LocalDateTime endTime;
    private Integer sellerId;

    public AuctionRequestDTO() {
    }

    public AuctionRequestDTO(String productName, String productType, String imageUrl, String description,
                             Double startingPrice, Double stepPrice, LocalDateTime endTime, Integer sellerId) {
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

    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }

    public Double getStepPrice() { return stepPrice; }
    public void setStepPrice(Double stepPrice) { this.stepPrice = stepPrice; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }
}