package com.auction.server.dto;

import java.math.BigDecimal;

/**
 * Compact DTO for Bid History / Chart data.
 * Avoids returning Bid entity directly (risk of JSON recursion & lazy loading).
 */
public class BidHistoryDTO {

    private Integer bidId;
    private Integer sessionId;
    private Integer bidderId;
    private String bidderName;
    private BigDecimal amount;
    private String bidTime; // ISO-8601

    public BidHistoryDTO() {
    }

    public BidHistoryDTO(Integer bidId, Integer sessionId, Integer bidderId,
                         String bidderName, BigDecimal amount, String bidTime) {
        this.bidId = bidId;
        this.sessionId = sessionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.bidTime = bidTime;
    }

    public Integer getBidId() {
        return bidId;
    }

    public void setBidId(Integer bidId) {
        this.bidId = bidId;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getBidderId() {
        return bidderId;
    }

    public void setBidderId(Integer bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBidTime() {
        return bidTime;
    }

    public void setBidTime(String bidTime) {
        this.bidTime = bidTime;
    }
}
