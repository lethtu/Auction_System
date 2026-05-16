package com.auction.client.common;

public final class AuctionStatus {
    private AuctionStatus() {
    }

    public static final String PENDING = "PENDING";
    public static final String ACTIVE = "ACTIVE";
    public static final String REJECTED = "REJECTED";
    public static final String ENDED = "ENDED";
    public static final String CANCELED = "CANCELED";
}