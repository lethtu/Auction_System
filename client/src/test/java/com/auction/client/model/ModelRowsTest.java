package com.auction.client.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ModelRowsTest {

    @Test
    public void testAdminSessionRow() {
        AdminSessionRow row = new AdminSessionRow(
                10,
                20,
                "Laptop",
                "seller1",
                new BigDecimal("100.0"),
                new BigDecimal("150.0"),
                "ACTIVE",
                true
        );

        assertEquals(10, row.getId());
        assertEquals(20, row.getProductId());
        assertEquals("Laptop", row.getProductName());
        assertEquals("seller1", row.getSellerUsername());
        assertEquals(new BigDecimal("100.0"), row.getStartingPrice());
        assertEquals(new BigDecimal("150.0"), row.getCurrentPrice());
        assertEquals("ACTIVE", row.getStatus());
        assertTrue(row.isProductVisible());

        assertNotNull(row.idProperty());
        assertNotNull(row.productIdProperty());
        assertNotNull(row.productNameProperty());
        assertNotNull(row.sellerUsernameProperty());
        assertNotNull(row.startingPriceProperty());
        assertNotNull(row.currentPriceProperty());
        assertNotNull(row.statusProperty());
        assertNotNull(row.productVisibleProperty());

        // Test overloads
        AdminSessionRow row2 = new AdminSessionRow(
                11,
                "Phone",
                "seller2",
                new BigDecimal("50.0"),
                "PENDING"
        );
        assertEquals(11, row2.getId());
        assertEquals(0, row2.getProductId());
        assertEquals("Phone", row2.getProductName());
        assertEquals(new BigDecimal("50.0"), row2.getCurrentPrice());
        assertTrue(row2.isProductVisible());

        AdminSessionRow row3 = new AdminSessionRow(
                12,
                30,
                null,
                null,
                null,
                null,
                null,
                false
        );
        assertEquals("", row3.getProductName());
        assertEquals("", row3.getSellerUsername());
        assertEquals(BigDecimal.ZERO, row3.getStartingPrice());
        assertEquals(BigDecimal.ZERO, row3.getCurrentPrice());
        assertEquals("", row3.getStatus());
        assertFalse(row3.isProductVisible());
    }

    @Test
    public void testAdminUserRow() {
        AdminUserRow row = new AdminUserRow(1, "john", "John Doe", "john@gmail.com", "ADMIN", true);
        assertEquals(1, row.getId());
        assertEquals("john", row.getUsername());
        assertEquals("John Doe", row.getFullname());
        assertEquals("john@gmail.com", row.getEmail());
        assertEquals("ADMIN", row.getRole());
        assertTrue(row.isBanned());

        assertNotNull(row.idProperty());
        assertNotNull(row.usernameProperty());
        assertNotNull(row.fullnameProperty());
        assertNotNull(row.emailProperty());
        assertNotNull(row.roleProperty());
        assertNotNull(row.bannedProperty());

        AdminUserRow nullRow = new AdminUserRow(2, null, null, null, null, false);
        assertEquals("", nullRow.getUsername());
        assertEquals("", nullRow.getFullname());
        assertEquals("", nullRow.getEmail());
        assertEquals("", nullRow.getRole());
    }

    @Test
    public void testBidChartPoint() {
        BidChartPoint point = new BidChartPoint(5, new BigDecimal("1200"), "2026-05-25T12:00:00", 1716638400000L, 8, "#A9B3", true);
        assertEquals(5, point.getBidId());
        assertEquals(new BigDecimal("1200"), point.getAmount());
        assertEquals("2026-05-25T12:00:00", point.getBidTime());
        assertEquals(1716638400000L, point.getEpochMillis());
        assertEquals(8, point.getBidderId());
        assertEquals("#A9B3", point.getMaskedBidderCode());
        assertTrue(point.isMine());
        assertEquals("You", point.getDisplayName());
        assertEquals("", point.getRelativeTime());

        point.setRelativeTime("3m ago");
        assertEquals("3m ago", point.getRelativeTime());

        BidChartPoint pointOther = new BidChartPoint(6, new BigDecimal("1300"), "2026-05-25T12:01:00", 1716638460000L, 9, "#A9B4", false);
        assertFalse(pointOther.isMine());
        assertEquals("#A9B4", pointOther.getDisplayName());
    }

    @Test
    public void testPendingSessionRow() {
        PendingSessionRow row = new PendingSessionRow(100, "Tablet", new BigDecimal("450"));
        assertEquals(100, row.getId());
        assertEquals("Tablet", row.getProductName());
        assertEquals(new BigDecimal("450"), row.getStartingPrice());

        assertNotNull(row.idProperty());
        assertNotNull(row.productNameProperty());
        assertNotNull(row.startingPriceProperty());

        PendingSessionRow nullRow = new PendingSessionRow(101, null, null);
        assertEquals("", nullRow.getProductName());
        assertEquals(BigDecimal.ZERO, nullRow.getStartingPrice());
    }

    @Test
    public void testBidderSubclass() {
        Bidder bidder = new Bidder();
        assertNotNull(bidder);
    }
}
