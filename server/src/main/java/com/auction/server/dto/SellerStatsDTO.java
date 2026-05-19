package com.auction.server.dto;

import java.math.BigDecimal;

public final class SellerStatsDTO {
    private final long totalSoldItems;
    private final BigDecimal totalRevenue;

    public SellerStatsDTO(long totalSoldItems, BigDecimal totalRevenue) {
        this.totalSoldItems = totalSoldItems;
        this.totalRevenue = totalRevenue;
    }

    public long getTotalSoldItems() {
        return totalSoldItems;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
}