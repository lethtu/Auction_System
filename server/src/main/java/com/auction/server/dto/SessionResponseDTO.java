package com.auction.server.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SessionResponseDTO {

    private Integer id;

    private Integer productId;
    private String productName;
    private String productType;
    private String description;
    private String imagePath;
    private Boolean productVisible;

    private Integer sellerId;
    private String sellerUsername;
    private String sellerFullname;

    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private BigDecimal stepPrice;
    private BigDecimal reservePrice;

    private Integer highestBidderId;
    private Integer bidCount;

    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String status;

    private LocalDateTime approvedAt;
    private Integer approvedByAdminId;

    private LocalDateTime rejectedAt;
    private Integer rejectedByAdminId;
    private String rejectReason;

    private Boolean applyMinRate;
    private BigDecimal minRate;
    private java.util.List<com.auction.server.model.Bid> bids;

    public SessionResponseDTO() {
    }

    public java.util.List<com.auction.server.model.Bid> getBids() {
        return bids;
    }

    public void setBids(java.util.List<com.auction.server.model.Bid> bids) {
        this.bids = bids;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
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

    public Boolean getProductVisible() {
        return productVisible;
    }

    public void setProductVisible(Boolean productVisible) {
        this.productVisible = productVisible;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public String getSellerFullname() {
        return sellerFullname;
    }

    public void setSellerFullname(String sellerFullname) {
        this.sellerFullname = sellerFullname;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
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

    public Integer getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(Integer highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public Integer getBidCount() {
        return bidCount;
    }

    public void setBidCount(Integer bidCount) {
        this.bidCount = bidCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Integer getApprovedByAdminId() {
        return approvedByAdminId;
    }

    public void setApprovedByAdminId(Integer approvedByAdminId) {
        this.approvedByAdminId = approvedByAdminId;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Integer getRejectedByAdminId() {
        return rejectedByAdminId;
    }

    public void setRejectedByAdminId(Integer rejectedByAdminId) {
        this.rejectedByAdminId = rejectedByAdminId;
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

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}