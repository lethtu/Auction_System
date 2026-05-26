package com.auction.server.mapper;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import com.auction.server.service.CloudinaryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        SessionResponseDTO dto1 = SessionResponseMapper.toDTO(session);
        assertEquals("iphone.jpg", dto1.getImagePath());

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

    @Test
    void mapToDTO_withCloudinaryService_usesDynamicImageUrlForUuidImage() {
        String uuid = java.util.UUID.randomUUID().toString();
        CloudinaryService cloudinaryService = mock(CloudinaryService.class);
        when(cloudinaryService.getDynamicImageUrl(uuid)).thenReturn("https://cdn.example.com/" + uuid + ".png");

        SessionResponseMapper mapper = new SessionResponseMapper();
        mapper.setCloudinaryService(cloudinaryService);

        AuctionSession session = new AuctionSession();
        TestItem item = new TestItem();
        item.setImagePath(uuid);
        session.setItem(item);

        SessionResponseDTO dto = mapper.mapToDTO(session, null);

        assertEquals("https://cdn.example.com/" + uuid + ".png", dto.getImagePath());
        assertEquals(0, dto.getBidCount());
        verify(cloudinaryService).getDynamicImageUrl(uuid);
    }

    @Test
    void mapToDTO_httpImageAndInvalidUuid_areKeptAsProvided() {
        SessionResponseMapper mapper = new SessionResponseMapper();
        AuctionSession session = new AuctionSession();
        TestItem item = new TestItem();
        session.setItem(item);

        item.setImagePath("https://cdn.example.com/image.png");
        assertEquals("https://cdn.example.com/image.png", mapper.mapToDTO(session).getImagePath());

        item.setImagePath("http://cdn.example.com/image.png");
        assertEquals("http://cdn.example.com/image.png", mapper.mapToDTO(session).getImagePath());

        item.setImagePath("not-a-valid-uuid");
        assertEquals("not-a-valid-uuid", mapper.mapToDTO(session).getImagePath());
    }

    @Test
    void mapToDTO_blankImage_negativeBidAndHiddenItem_areHandledSafely() {
        AuctionSession session = new AuctionSession();
        TestItem item = new TestItem();
        item.setImagePath("   ");
        item.setHidden(true);
        session.setItem(item);

        SessionResponseDTO dto = SessionResponseMapper.toDTO(session, -4);

        assertEquals("", dto.getImagePath());
        assertEquals(0, dto.getBidCount());
    }

    @Test
    void mapToDTO_mapsSellerStatusAuditAndTimingFields() {
        AuctionSession session = new AuctionSession();
        Seller seller = new Seller();
        seller.setId(9);
        seller.setUsername("seller9");
        seller.setFullname("Seller Nine");
        session.setSeller(seller);
        session.setStatus(AuctionStatus.ACTIVE);
        session.setStartingPrice(new BigDecimal("100"));
        session.setCurrentPrice(new BigDecimal("150"));
        session.setStepPrice(new BigDecimal("10"));
        session.setReservePrice(new BigDecimal("90"));
        session.setHighestBidderId(88);
        session.setRejectReason("bad data");
        session.setApprovedByAdminId(1);
        session.setRejectedByAdminId(2);
        session.setApplyMinRate(true);
        session.setMinRate(new BigDecimal("80"));
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now.minusDays(1));
        session.setStartTime(now);
        session.setEndTime(now.plusHours(1));
        session.setApprovedAt(now.plusMinutes(1));
        session.setRejectedAt(now.plusMinutes(2));

        SessionResponseDTO dto = SessionResponseMapper.toDTO(session, 3);

        assertEquals(9, dto.getSellerId());
        assertEquals("seller9", dto.getSellerUsername());
        assertEquals("Seller Nine", dto.getSellerFullname());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(new BigDecimal("100"), dto.getStartingPrice());
        assertEquals(new BigDecimal("150"), dto.getCurrentPrice());
        assertEquals(new BigDecimal("10"), dto.getStepPrice());
        assertEquals(new BigDecimal("90"), dto.getReservePrice());
        assertEquals(88, dto.getHighestBidderId());
        assertEquals("bad data", dto.getRejectReason());
        assertEquals(1, dto.getApprovedByAdminId());
        assertEquals(2, dto.getRejectedByAdminId());
        assertEquals(Boolean.TRUE, dto.getApplyMinRate());
        assertEquals(new BigDecimal("80"), dto.getMinRate());
        assertEquals(now.minusDays(1), dto.getCreatedAt());
        assertEquals(now, dto.getStartTime());
        assertEquals(now.plusHours(1), dto.getEndTime());
        assertEquals(now.plusMinutes(1), dto.getApprovedAt());
        assertEquals(now.plusMinutes(2), dto.getRejectedAt());
    }

    private static class TestItem extends Item {
        @Override
        public String getCategoryInfo() {
            return "TEST";
        }
    }
}
