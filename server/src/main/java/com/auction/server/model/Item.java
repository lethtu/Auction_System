package com.auction.server.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String type;

    @Column(name = "current_price")
    private Double currentPrice;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "image_path")
    private String imagePath;

    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    public Integer getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public double getCurrentPrice(){
        return currentPrice;
    }

    public LocalDateTime getStartTime(){
        return startTime;
    }

    public LocalDateTime getEndTime(){
        return endTime;
    }

    public String getImagePath(){
        return imagePath;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}