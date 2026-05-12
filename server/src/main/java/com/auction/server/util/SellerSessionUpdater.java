package com.auction.server.util;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.Item;

public final class SellerSessionUpdater {

    private SellerSessionUpdater() {
    }

    public static void updateItemFromRequest(Item item, CreateAuctionRequest request) {
        item.setName(request.getName());
        item.setType(request.getType());
        item.setDescription(request.getDescription());
    }

    public static void updateSessionFromRequest(AuctionSession session, CreateAuctionRequest request) {
        session.setStartingPrice(request.getStartingPrice());
        session.setCurrentPrice(request.getStartingPrice());
        session.setStepPrice(request.getStepPrice());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
    }

    public static void resetApprovalInfo(AuctionSession session) {
        session.setApprovedAt(null);
        session.setRejectedAt(null);
        session.setRejectReason(null);
        session.setApprovedByAdminId(null);
        session.setRejectedByAdminId(null);
    }
}