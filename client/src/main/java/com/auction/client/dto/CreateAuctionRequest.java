package com.auction.client.dto;

import java.math.BigDecimal;

public class CreateAuctionRequest {
    public final String productName;
    public final String productType;
    public final String description;
    public final String imagePath;
    public final BigDecimal startingPrice;
    public final BigDecimal stepPrice;
    public final String startTime;
    public final String endTime;
    public final int sellerId;

    public CreateAuctionRequest(
            String productName,
            String productType,
            String description,
            BigDecimal startingPrice,
            BigDecimal stepPrice,
            String startTime,
            String endTime,
            int sellerId
    ) {
        this(productName, productType, description, null, startingPrice, stepPrice, startTime, endTime, sellerId);
    }

    public CreateAuctionRequest(
            String productName,
            String productType,
            String description,
            String imagePath,
            BigDecimal startingPrice,
            BigDecimal stepPrice,
            String startTime,
            String endTime,
            int sellerId
    ) {
        this.productName = productName;
        this.productType = productType;
        this.description = description;
        this.imagePath = imagePath;
        this.startingPrice = startingPrice;
        this.stepPrice = stepPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }
}
