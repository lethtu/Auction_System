package com.auction.server.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class item {

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

    private String status;

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

    public String getStatus(){
        return status;
    }
}