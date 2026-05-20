package com.auction.server.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "auto_bid_configs")
public class AutoBidConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer sessionId;

    @Column(nullable = false)
    private Integer bidderId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxBid;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal increment;

    @Column(nullable = false)
    private boolean active = true;

    public AutoBidConfig() {
    }

    public AutoBidConfig(Integer sessionId, Integer bidderId, BigDecimal maxBid, BigDecimal increment) {
        this.sessionId = sessionId;
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.active = true;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public BigDecimal getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(BigDecimal maxBid) {
        this.maxBid = maxBid;
    }

    public BigDecimal getIncrement() {
        return increment;
    }

    public void setIncrement(BigDecimal increment) {
        this.increment = increment;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "AutoBidConfig{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", bidderId=" + bidderId +
                ", maxBid=" + maxBid +
                ", increment=" + increment +
                ", active=" + active +
                '}';
    }
}
