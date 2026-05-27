package com.auction.client.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class SessionItemTest {

    @Test
    public void testToDisplayText() {
        SessionItem item = new SessionItem();
        item.id = 42;
        item.productName = "Vase";
        item.status = "ACTIVE";
        item.currentPrice = new BigDecimal("500000");
        item.stepPrice = new BigDecimal("20000");
        item.reservePrice = new BigDecimal("1000000");
        item.applyMinRate = true;
        item.minRate = new BigDecimal("0.05");

        String display = item.toDisplayText();
        assertTrue(display.contains("Session #42"));
        assertTrue(display.contains("Vase"));
        assertTrue(display.contains("ACTIVE"));
        assertTrue(display.contains("MinRate:"));
        assertTrue(display.contains("Current Price: 500.000 VND"));
        assertTrue(display.contains("Step Price: 20.000 VND"));
        assertTrue(display.contains("Reserve Price: 1.000.000 VND"));

        display = item.toDisplayText(3);
        assertTrue(display.contains("Index 3 | Session #42"));
    }

    @Test
    public void testNullFields() {
        SessionItem item = new SessionItem();
        item.id = 50;
        item.productName = null;
        item.status = null;
        item.currentPrice = null;
        item.stepPrice = null;
        item.reservePrice = null;
        item.applyMinRate = false;

        String display = item.toDisplayText();
        assertTrue(display.contains("Unknown"));
        assertTrue(display.contains("Current Price: 0 VND"));
    }
}
