package com.auction.server.dto;

import java.math.BigDecimal;

public class SellerStatsDTO {
    private long totalSoldItems;
    private BigDecimal totalRevenue;

    public SellerStatsDTO(long totalSoldItems, BigDecimal totalRevenue) {
        this.totalSoldItems = totalSoldItems;
        this.totalRevenue = totalRevenue;
    }

    public long getTotalSoldItems() { return totalSoldItems; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
}