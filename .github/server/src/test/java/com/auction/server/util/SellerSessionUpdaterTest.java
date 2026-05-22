package com.auction.server.util;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SellerSessionUpdaterTest {

    @Test
    void updateItemFromRequest_copiesItemFields() {
        TestItem item = new TestItem();
        CreateAuctionRequest request = validRequest();

        SellerSessionUpdater.updateItemFromRequest(item, request);

        assertEquals("Laptop Gaming", item.getName());
        assertEquals("electronics", item.getType());
        assertEquals("Good condition", item.getDescription());
    }

    @Test
    void updateSessionFromRequest_copiesPriceAndTimeFields() {
        AuctionSession session = new AuctionSession();
        CreateAuctionRequest request = validRequest();

        SellerSessionUpdater.updateSessionFromRequest(session, request);

        assertEquals(new BigDecimal("1000000"), session.getStartingPrice());
        assertEquals(new BigDecimal("1000000"), session.getCurrentPrice());
        assertEquals(new BigDecimal("100000"), session.getStepPrice());
        assertEquals(request.getStartTime(), session.getStartTime());
        assertEquals(request.getEndTime(), session.getEndTime());
    }

    @Test
    void resetApprovalInfo_clearsAdminApprovalAndRejectFields() {
        AuctionSession session = new AuctionSession();
        session.setApprovedAt(LocalDateTime.now());
        session.setRejectedAt(LocalDateTime.now());
        session.setRejectReason("Incorrect info");
        session.setApprovedByAdminId(1);
        session.setRejectedByAdminId(2);

        SellerSessionUpdater.resetApprovalInfo(session);

        assertNull(session.getApprovedAt());
        assertNull(session.getRejectedAt());
        assertNull(session.getRejectReason());
        assertNull(session.getApprovedByAdminId());
        assertNull(session.getRejectedByAdminId());
    }

    private CreateAuctionRequest validRequest() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setName("Laptop Gaming");
        request.setType("electronics");
        request.setDescription("Good condition");
        request.setStartingPrice(new BigDecimal("1000000"));
        request.setStepPrice(new BigDecimal("100000"));
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusDays(1));
        return request;
    }

    private static class TestItem extends Item {
        @Override
        public String getCategoryInfo() {
            return "TEST";
        }
    }
}
