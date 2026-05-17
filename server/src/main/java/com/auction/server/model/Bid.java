package com.auction.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "bids")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "session_id")
    private AuctionSession session;

    @ManyToOne
    @JoinColumn(name = "bidder_id")
    private User bidder;

    private BigDecimal amount;
    private LocalDateTime time;

    public Bid() {}

    public Bid(AuctionSession session, User bidder, BigDecimal amount, LocalDateTime time){
        this.session = session;
        this.bidder = bidder;
        this.amount = amount;
        this.time = time;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AuctionSession getSession() {
        return session;
    }

    public void setSession(AuctionSession session) {
        this.session = session;
    }

    public User getBidder() {
        return bidder;
    }

    public void setBidder(User bidder) {
        this.bidder = bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}