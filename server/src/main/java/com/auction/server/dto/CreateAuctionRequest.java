package com.auction.server.dto;

import java.time.LocalDateTime;

public class CreateAuctionRequest {
    // Thông tin dành cho Product
    private String name;
    private String type;
    private String imagePath;

    // Thông tin dành cho AuctionSession
    private Double startingPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}