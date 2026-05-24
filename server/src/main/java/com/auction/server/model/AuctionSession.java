package com.auction.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auction_sessions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuctionSession implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relationship: One Item - Many Auction Sessions
    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal startingPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal stepPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal reservePrice;

    private Integer highestBidderId;

    private LocalDateTime createdAt;
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;

    @Transient
    private Integer totalBids;

    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    private Integer approvedByAdminId;
    private Integer rejectedByAdminId;

    @Column(name = "delivery_recipient", length = 150)
    private String deliveryRecipient;

    @Column(name = "delivery_phone", length = 30)
    private String deliveryPhone;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_note", length = 500)
    private String deliveryNote;

    @Column(name = "delivery_submitted_at")
    private LocalDateTime deliverySubmittedAt;

    @Column(name = "apply_min_rate")
    private Boolean applyMinRate;

    @Column(name = "min_rate", precision = 15, scale = 2)
    private BigDecimal minRate;

    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    // OPTIMIZED 1-N RELATIONSHIP (AUCTION_SESSION - BIDS)
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Bid> bids = new ArrayList<>();

    public AuctionSession() {
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // HELPER METHODS: BIDIRECTIONAL SYNC
    public void addBid(Bid bid) {
        bids.add(bid);
        bid.setSession(this);
    }

    public void removeBid(Bid bid) {
        bids.remove(bid);
        bid.setSession(null);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public User getWinner() {
        return winner;
    }

    public void setWinner(User winner) {
        this.winner = winner;
    }

    public String getDeliveryRecipient() {
        return deliveryRecipient;
    }

    public void setDeliveryRecipient(String deliveryRecipient) {
        this.deliveryRecipient = deliveryRecipient;
    }

    public String getDeliveryPhone() {
        return deliveryPhone;
    }

    public void setDeliveryPhone(String deliveryPhone) {
        this.deliveryPhone = deliveryPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getDeliveryNote() {
        return deliveryNote;
    }

    public void setDeliveryNote(String deliveryNote) {
        this.deliveryNote = deliveryNote;
    }

    public LocalDateTime getDeliverySubmittedAt() {
        return deliverySubmittedAt;
    }

    public void setDeliverySubmittedAt(LocalDateTime deliverySubmittedAt) {
        this.deliverySubmittedAt = deliverySubmittedAt;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getStepPrice() {
        return stepPrice;
    }

    public void setStepPrice(BigDecimal stepPrice) {
        this.stepPrice = stepPrice;
    }

    public BigDecimal getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
    }

    public Integer getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(Integer highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Integer getApprovedByAdminId() {
        return approvedByAdminId;
    }

    public void setApprovedByAdminId(Integer approvedByAdminId) {
        this.approvedByAdminId = approvedByAdminId;
    }

    public Integer getRejectedByAdminId() {
        return rejectedByAdminId;
    }

    public void setRejectedByAdminId(Integer rejectedByAdminId) {
        this.rejectedByAdminId = rejectedByAdminId;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Integer getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(Integer totalBids) {
        this.totalBids = totalBids;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }

    public Boolean getApplyMinRate() {
        return applyMinRate;
    }

    public void setApplyMinRate(Boolean applyMinRate) {
        this.applyMinRate = applyMinRate;
    }

    public BigDecimal getMinRate() {
        return minRate;
    }

    public void setMinRate(BigDecimal minRate) {
        this.minRate = minRate;
    }
    @Override
    public String toString() {
        return "AuctionSession{" +
                "id=" + id +
                ", itemId=" + (item != null ? item.getId() : "null") +
                ", sellerId=" + (seller != null ? seller.getId() : "null") +
                ", startingPrice=" + startingPrice +
                ", currentPrice=" + currentPrice +
                ", stepPrice=" + stepPrice +
                ", reservePrice=" + reservePrice +
                ", highestBidderId=" + highestBidderId +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
