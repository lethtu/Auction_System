package com.auction.server.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class AuctionSessionTest {

    @Test
    void prePersistSetsCreatedAtOnlyWhenMissing() {
        AuctionSession missingCreatedAt = new AuctionSession();
        AuctionSession existingCreatedAt = new AuctionSession();
        LocalDateTime fixed = LocalDateTime.of(2026, 5, 26, 9, 0);
        existingCreatedAt.setCreatedAt(fixed);

        missingCreatedAt.prePersist();
        existingCreatedAt.prePersist();

        assertNotNull(missingCreatedAt.getCreatedAt());
        assertEquals(fixed, existingCreatedAt.getCreatedAt());
    }

    @Test
    void addBidAndRemoveBidKeepBidirectionalRelationshipInSync() {
        AuctionSession session = new AuctionSession();
        Bid bid = new Bid();

        session.addBid(bid);

        assertTrue(session.getBids().contains(bid));
        assertSame(session, bid.getSession());

        session.removeBid(bid);

        assertFalse(session.getBids().contains(bid));
        assertNull(bid.getSession());
    }

    @Test
    void settersAndGettersStoreAllSessionFields() {
        AuctionSession session = new AuctionSession();
        TestItem item = new TestItem();
        Seller seller = new Seller();
        User winner = new User();
        LocalDateTime created = LocalDateTime.of(2026, 5, 26, 8, 0);
        LocalDateTime start = LocalDateTime.of(2026, 5, 26, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 27, 9, 0);
        LocalDateTime approved = LocalDateTime.of(2026, 5, 26, 8, 30);
        LocalDateTime rejected = LocalDateTime.of(2026, 5, 26, 8, 45);
        LocalDateTime deliverySubmitted = LocalDateTime.of(2026, 5, 28, 10, 0);
        ArrayList<Bid> bids = new ArrayList<>();

        session.setId(1);
        session.setItem(item);
        session.setSeller(seller);
        session.setWinner(winner);
        session.setStartingPrice(BigDecimal.valueOf(100));
        session.setCurrentPrice(BigDecimal.valueOf(150));
        session.setStepPrice(BigDecimal.valueOf(10));
        session.setReservePrice(BigDecimal.valueOf(200));
        session.setHighestBidderId(9);
        session.setCreatedAt(created);
        session.setStartTime(start);
        session.setEndTime(end);
        session.setStatus(AuctionStatus.ACTIVE);
        session.setRejectReason("invalid item");
        session.setApprovedByAdminId(2);
        session.setRejectedByAdminId(3);
        session.setApprovedAt(approved);
        session.setRejectedAt(rejected);
        session.setTotalBids(5);
        session.setBids(bids);
        session.setDeliveryRecipient("Minh");
        session.setDeliveryPhone("0900000000");
        session.setDeliveryAddress("Ha Noi");
        session.setDeliveryNote("Call first");
        session.setDeliverySubmittedAt(deliverySubmitted);
        session.setApplyMinRate(Boolean.TRUE);
        session.setMinRate(BigDecimal.valueOf(120));

        assertEquals(1, session.getId());
        assertSame(item, session.getItem());
        assertSame(seller, session.getSeller());
        assertSame(winner, session.getWinner());
        assertEquals(BigDecimal.valueOf(100), session.getStartingPrice());
        assertEquals(BigDecimal.valueOf(150), session.getCurrentPrice());
        assertEquals(BigDecimal.valueOf(10), session.getStepPrice());
        assertEquals(BigDecimal.valueOf(200), session.getReservePrice());
        assertEquals(9, session.getHighestBidderId());
        assertEquals(created, session.getCreatedAt());
        assertEquals(start, session.getStartTime());
        assertEquals(end, session.getEndTime());
        assertEquals(AuctionStatus.ACTIVE, session.getStatus());
        assertEquals("invalid item", session.getRejectReason());
        assertEquals(2, session.getApprovedByAdminId());
        assertEquals(3, session.getRejectedByAdminId());
        assertEquals(approved, session.getApprovedAt());
        assertEquals(rejected, session.getRejectedAt());
        assertEquals(5, session.getTotalBids());
        assertSame(bids, session.getBids());
        assertEquals("Minh", session.getDeliveryRecipient());
        assertEquals("0900000000", session.getDeliveryPhone());
        assertEquals("Ha Noi", session.getDeliveryAddress());
        assertEquals("Call first", session.getDeliveryNote());
        assertEquals(deliverySubmitted, session.getDeliverySubmittedAt());
        assertTrue(session.getApplyMinRate());
        assertEquals(BigDecimal.valueOf(120), session.getMinRate());
    }

    @Test
    void toStringHandlesNullAndNonNullRelationships() {
        AuctionSession empty = new AuctionSession();
        AuctionSession populated = new AuctionSession();
        TestItem item = new TestItem();
        Seller seller = new Seller();
        item.setId(4);
        seller.setId(8);

        populated.setId(10);
        populated.setItem(item);
        populated.setSeller(seller);
        populated.setStartingPrice(BigDecimal.valueOf(100));
        populated.setCurrentPrice(BigDecimal.valueOf(120));
        populated.setStepPrice(BigDecimal.valueOf(10));
        populated.setReservePrice(BigDecimal.valueOf(150));
        populated.setHighestBidderId(11);
        populated.setStatus(AuctionStatus.ACTIVE);
        populated.setStartTime(LocalDateTime.of(2026, 5, 26, 9, 0));
        populated.setEndTime(LocalDateTime.of(2026, 5, 27, 9, 0));

        assertTrue(empty.toString().contains("itemId=null"));
        assertTrue(empty.toString().contains("sellerId=null"));
        assertTrue(populated.toString().contains("itemId=4"));
        assertTrue(populated.toString().contains("sellerId=8"));
        assertTrue(populated.toString().contains("status=ACTIVE"));
    }

    private static class TestItem extends Item {
        @Override
        public String getCategoryInfo() {
            return "test-category";
        }
    }
}
