package com.auction.server.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateAuctionRequest {
    // Thông tin dành cho Item
    private String name;
    private String type;
    private String imagePath;
    private String description; // Thêm mô tả sản phẩm

    // Thông tin dành cho AuctionSession
    private Integer sellerId;         // Thêm ID người bán
    private BigDecimal startingPrice; // Đổi chuẩn sang BigDecimal
    private BigDecimal stepPrice;     // Thêm bước giá
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    // --- Getters and Setters ---

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public BigDecimal getStepPrice() { return stepPrice; }
    public void setStepPrice(BigDecimal stepPrice) { this.stepPrice = stepPrice; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}