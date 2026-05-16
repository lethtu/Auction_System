package com.auction.server.mapper;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SessionResponseMapperTest {

    @Test
    void toDTO_fullSession_mapsAllFields() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);
        LocalDateTime approvedAt = LocalDateTime.now();
        LocalDateTime rejectedAt = LocalDateTime.now().minusHours(2);

        TestItem item = new TestItem();
        item.setId(100);
        item.setName("Laptop Gaming");
        item.setType("electronics");
        item.setDescription("Máy còn tốt");
        item.setImagePath("upload/images/laptop.png");

        Seller seller = new Seller();
        seller.setId(1);
        seller.setUsername("seller01");
        seller.setFullname("Nguyễn Văn Seller");

        AuctionSession session = new AuctionSession();
        session.setId(10);
        session.setItem(item);
        session.setSeller(seller);
        session.setStartingPrice(new BigDecimal("1000000"));
        session.setCurrentPrice(new BigDecimal("1500000"));
        session.setStepPrice(new BigDecimal("100000"));
        session.setReservePrice(new BigDecimal("2000000"));
        session.setHighestBidderId(14);
        session.setCreatedAt(createdAt);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setApprovedAt(approvedAt);
        session.setRejectedAt(rejectedAt);
        session.setStatus(AuctionStatus.REJECTED);
        session.setRejectReason("Sai thông tin");
        session.setApprovedByAdminId(2);
        session.setRejectedByAdminId(3);

        SessionResponseDTO dto = SessionResponseMapper.toDTO(session, 7);

        assertEquals(10, dto.getId());
        assertEquals(100, dto.getProductId());
        assertEquals("Laptop Gaming", dto.getProductName());
        assertEquals("electronics", dto.getProductType());
        assertEquals("Máy còn tốt", dto.getDescription());
        assertEquals("upload/images/laptop.png", dto.getImagePath());
        assertEquals(1, dto.getSellerId());
        assertEquals("seller01", dto.getSellerUsername());
        assertEquals("Nguyễn Văn Seller", dto.getSellerFullname());
        assertEquals(new BigDecimal("1000000"), dto.getStartingPrice());
        assertEquals(new BigDecimal("1500000"), dto.getCurrentPrice());
        assertEquals(new BigDecimal("100000"), dto.getStepPrice());
        assertEquals(new BigDecimal("2000000"), dto.getReservePrice());
        assertEquals(14, dto.getHighestBidderId());
        assertEquals(7, dto.getBidCount());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(startTime, dto.getStartTime());
        assertEquals(endTime, dto.getEndTime());
        assertEquals(approvedAt, dto.getApprovedAt());
        assertEquals(rejectedAt, dto.getRejectedAt());
        assertEquals("REJECTED", dto.getStatus());
        assertEquals("Sai thông tin", dto.getRejectReason());
        assertEquals(2, dto.getApprovedByAdminId());
        assertEquals(3, dto.getRejectedByAdminId());
    }

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
