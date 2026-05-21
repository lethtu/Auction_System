package com.auction.server.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateAuctionRequest {

    private String name;
    private String type;
    private String description;
    private String imagePath;

    private Integer sellerId;

    private BigDecimal startingPrice;
    private BigDecimal stepPrice;
    private BigDecimal reservePrice;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Boolean applyMinRate;
    private BigDecimal minRate;

    public CreateAuctionRequest() {
    }

    public CreateAuctionRequest(
            String name,
            String type,
            String description,
            String imagePath,
            Integer sellerId,
            BigDecimal startingPrice,
            BigDecimal stepPrice,
            BigDecimal reservePrice,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.imagePath = imagePath;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.stepPrice = stepPrice;
        this.reservePrice = reservePrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getStepPrice() {
        return stepPrice;
    }

    public void setStepPrice(BigDecimal stepPrice) {
        this.stepPrice = stepPrice;
    }

    public BigDecimal getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getApplyMinRate() {
        return applyMinRate;
    }

    public void setApplyMinRate(Boolean applyMinRate) {
        this.applyMinRate = applyMinRate;
    }

    public BigDecimal getMinRate() {
        return minRate;
    }

    public void setMinRate(BigDecimal minRate) {
        this.minRate = minRate;
    }
}