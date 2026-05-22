package com.auction.server.mapper;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SessionResponseMapperTest {


    @Test
    void toDTO_nullItemSellerStatus_mapsSafeNulls() {
        AuctionSession session = new AuctionSession();
        session.setId(10);
        session.setStartingPrice(new BigDecimal("1000000"));
        session.setCurrentPrice(new BigDecimal("1000000"));
        session.setStepPrice(new BigDecimal("100000"));

        SessionResponseDTO dto = SessionResponseMapper.toDTO(session, 7);

        assertEquals(10, dto.getId());
        assertNull(dto.getProductId());
        assertNull(dto.getProductName());
        assertNull(dto.getSellerId());
        assertNull(dto.getSellerUsername());
        assertNull(dto.getStatus());
        assertEquals(new BigDecimal("1000000"), dto.getStartingPrice());
        assertEquals(7, dto.getBidCount());
    }

    private static class TestItem extends Item {
        @Override
        public String getCategoryInfo() {
            return "TEST";
        }
    }
}
