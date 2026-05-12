package com.auction.server.mapper;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;

public final class SessionResponseMapper {

    private SessionResponseMapper() {
    }

    public static SessionResponseDTO toDTO(AuctionSession session) {
        SessionResponseDTO dto = new SessionResponseDTO();

        dto.setId(session.getId());

        if (session.getItem() != null) {
            dto.setProductId(session.getItem().getId());
            dto.setProductName(session.getItem().getName());
            dto.setProductType(session.getItem().getType());
            dto.setDescription(session.getItem().getDescription());
        }

        if (session.getSeller() != null) {
            dto.setSellerId(session.getSeller().getId());
            dto.setSellerUsername(session.getSeller().getUsername());
            dto.setSellerFullname(session.getSeller().getFullname());
        }

        dto.setStartingPrice(session.getStartingPrice());
        dto.setCurrentPrice(session.getCurrentPrice());
        dto.setStepPrice(session.getStepPrice());
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