package com.auction.server.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private AuctionSession session;

    @ManyToOne
    @JoinColumn(name = "bidder_id")
    private User bidder;

    private Double amount;
    private LocalDateTime time;

    public Bid() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public AuctionSession getSession() { return session; }
    public void setSession(AuctionSession session) { this.session = session; }
    public User getBidder() { return bidder; }
    public void setBidder(User bidder) { this.bidder = bidder; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
}