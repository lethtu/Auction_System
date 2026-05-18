package com.auction.client.dto;

import java.math.BigDecimal;

public final class CreateAuctionRequest {
    public final String productName;
    public final String productType;
    public final String description;
    public final String imagePath;
    public final BigDecimal startingPrice;
    public final BigDecimal stepPrice;
    public final BigDecimal reservePrice;
    public final String startTime;
    public final String endTime;
    public final int sellerId;
    public final Boolean applyMinRate;
    public final BigDecimal minRate;

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
        this(
                productName,
                productType,
                description,
                null,
                startingPrice,
                stepPrice,
                null,
                startTime,
                endTime,
                sellerId,
                false,
                null
        );
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
        this(
                productName,
                productType,
                description,
                imagePath,
                startingPrice,
                stepPrice,
                null,
                startTime,
                endTime,
                sellerId,
                false,
                null
        );
    }

    public CreateAuctionRequest(
            String productName,
            String productType,
            String description,
            String imagePath,
            BigDecimal startingPrice,
            BigDecimal stepPrice,
            BigDecimal reservePrice,
            String startTime,
            String endTime,
            int sellerId
    ) {
        this(
                productName,
                productType,
                description,
                imagePath,
                startingPrice,
                stepPrice,
                reservePrice,
                startTime,
                endTime,
                sellerId,
                false,
                null
        );
    }

    public CreateAuctionRequest(
            String productName,
            String productType,
            String description,
            String imagePath,
            BigDecimal startingPrice,
            BigDecimal stepPrice,
            BigDecimal reservePrice,
            String startTime,
            String endTime,
            int sellerId,
            Boolean applyMinRate,
            BigDecimal minRate
    ) {
        this.productName = productName;
        this.productType = productType;
        this.description = description;
        this.imagePath = imagePath;
        this.startingPrice = startingPrice;
        this.stepPrice = stepPrice;
        this.reservePrice = reservePrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sellerId = sellerId;
        this.applyMinRate = applyMinRate != null ? applyMinRate : false;
        this.minRate = minRate;
    }

    public boolean hasImagePath() {
        return imagePath != null && !imagePath.isBlank();
    }

    public boolean hasReservePrice() {
        return reservePrice != null;
    }
}