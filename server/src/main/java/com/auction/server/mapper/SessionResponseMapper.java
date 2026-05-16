package com.auction.server.mapper;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;

public final class SessionResponseMapper {

    private SessionResponseMapper() {
    }

    public static SessionResponseDTO toDTO(AuctionSession session) {
        return toDTO(session, 0);
    }

    public static SessionResponseDTO toDTO(AuctionSession session, Integer bidCount) {
        SessionResponseDTO dto = new SessionResponseDTO();

        dto.setId(session.getId());

        if (session.getItem() != null) {
            dto.setProductId(session.getItem().getId());
            dto.setProductName(session.getItem().getName());
            dto.setProductType(session.getItem().getType());
            dto.setDescription(session.getItem().getDescription());
            dto.setImagePath(session.getItem().getImagePath());
        }

        if (session.getSeller() != null) {
            dto.setSellerId(session.getSeller().getId());
            dto.setSellerUsername(session.getSeller().getUsername());
            dto.setSellerFullname(session.getSeller().getFullname());
        }

        dto.setStartingPrice(session.getStartingPrice());
        dto.setCurrentPrice(session.getCurrentPrice());
        dto.setStepPrice(session.getStepPrice());
        dto.setReservePrice(session.getReservePrice());
        dto.setHighestBidderId(session.getHighestBidderId());
        dto.setBidCount(bidCount == null ? 0 : Math.max(0, bidCount));
        dto.setCreatedAt(session.getCreatedAt());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setApprovedAt(session.getApprovedAt());
        dto.setRejectedAt(session.getRejectedAt());

        if (session.getStatus() != null) {
            dto.setStatus(session.getStatus().name());
        }

        dto.setRejectReason(session.getRejectReason());
        dto.setApprovedByAdminId(session.getApprovedByAdminId());
        dto.setRejectedByAdminId(session.getRejectedByAdminId());

        return dto;
    }
}