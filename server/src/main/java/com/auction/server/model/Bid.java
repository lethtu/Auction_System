package com.auction.server.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "bidder_id")
    private Integer bidderId;

    @Column(name = "bid_amount")
    private Double bidAmount;

    @Column(name = "bid_time")
    private LocalDateTime bidTime;

}