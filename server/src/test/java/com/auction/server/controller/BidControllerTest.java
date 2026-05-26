package com.auction.server.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class BidControllerTest {

    @Test
    void constructor_shouldCreateController() {
        BidController controller = new BidController();

        assertNotNull(controller);
    }
}
