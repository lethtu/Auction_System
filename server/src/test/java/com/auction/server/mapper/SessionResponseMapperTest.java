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

    @Test
    void toDTO_withItem_mapsImagePathAndLegacySafely() {
        AuctionSession session = new AuctionSession();
        TestItem item = new TestItem();
        item.setId(5);
        item.setName("iPhone");
        item.setImagePath("iphone.jpg");
        session.setItem(item);

        // Test legacy image path (not equal to UUID)
        SessionResponseDTO dto1 = SessionResponseMapper.toDTO(session);
        assertEquals("iphone.jpg", dto1.getImagePath());

        // Test new UUID-based image path (equal to UUID)
        String uuid = java.util.UUID.randomUUID().toString();
        item.setImagePath(uuid);
        SessionResponseDTO dto2 = SessionResponseMapper.toDTO(session);
        assertEquals("/api/files/images/" + uuid + "/" + uuid + ".png", dto2.getImagePath());
    }

    @Test
    void toDTO_withValidUuidImagePath_automaticallyDeducesUuid() {
        AuctionSession session = new AuctionSession();
        TestItem item = new TestItem();
        item.setId(6);
        item.setName("MacBook");
        
        String clientUploadedUuid = java.util.UUID.randomUUID().toString();
        item.setImagePath(clientUploadedUuid);
        
        assertEquals(clientUploadedUuid, item.getUuid());
        
        session.setItem(item);
        SessionResponseDTO dto = SessionResponseMapper.toDTO(session);
        assertEquals("/api/files/images/" + clientUploadedUuid + "/" + clientUploadedUuid + ".png", dto.getImagePath());
    }

    private static class TestItem extends Item {
        @Override
        public String getCategoryInfo() {
            return "TEST";
        }
    }
}
