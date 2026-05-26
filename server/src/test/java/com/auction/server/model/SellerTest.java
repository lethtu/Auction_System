package com.auction.server.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SellerTest {

    @Test
    void constructor_shouldUseNullDefaults() {
        Seller seller = new Seller();

        assertNull(seller.getIsBusiness());
        assertNull(seller.getTaxId());
    }

    @Test
    void settersAndGetters_shouldStoreBusinessSellerFields() {
        Seller seller = new Seller();

        seller.setIsBusiness(Boolean.TRUE);
        seller.setTaxId("TAX-001");

        assertTrue(seller.getIsBusiness());
        assertSame("TAX-001", seller.getTaxId());
    }

    @Test
    void settersAndGetters_shouldAllowFalseAndNullValues() {
        Seller seller = new Seller();

        seller.setIsBusiness(Boolean.FALSE);
        seller.setTaxId(null);

        assertFalse(seller.getIsBusiness());
        assertNull(seller.getTaxId());
    }
}
