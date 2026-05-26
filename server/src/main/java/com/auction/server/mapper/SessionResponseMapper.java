package com.auction.server.mapper;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SessionResponseMapper {

    private CloudinaryService cloudinaryService;

    @Autowired(required = false)
    public void setCloudinaryService(CloudinaryService service) {
        this.cloudinaryService = service;
    }

    public SessionResponseDTO mapToDTO(AuctionSession session) {
        return mapToDTO(session, 0);
    }

    public SessionResponseDTO mapToDTO(AuctionSession session, Integer bidCount) {
        SessionResponseDTO dto = new SessionResponseDTO();

        dto.setId(session.getId());

        if (session.getItem() != null) {
            dto.setProductId(session.getItem().getId());
            dto.setProductName(session.getItem().getName());
            dto.setProductType(session.getItem().getType());
            dto.setDescription(session.getItem().getDescription());

            String imagePath = session.getItem().getImagePath();
            if (imagePath != null && !imagePath.isBlank()) {
                if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    dto.setImagePath(imagePath);
                } else {
                    String uuid = session.getItem().getUuid();
                    if (imagePath.equals(uuid) && isUuid(uuid)) {
                        if (cloudinaryService != null) {
                            dto.setImagePath(cloudinaryService.getDynamicImageUrl(uuid));
                        } else {
                            dto.setImagePath("/api/files/images/" + uuid + "/" + uuid + ".png");
                        }
                    } else {
                        dto.setImagePath(imagePath);
                    }
                }
            } else {
                dto.setImagePath(imagePath);
            }

            dto.setProductVisible(!session.getItem().isHidden());
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
        dto.setApplyMinRate(session.getApplyMinRate());
        dto.setMinRate(session.getMinRate());

        return dto;
    }

    public static SessionResponseDTO toDTO(AuctionSession session) {
        return new SessionResponseMapper().mapToDTO(session, 0);
    }

    public static SessionResponseDTO toDTO(AuctionSession session, Integer bidCount) {
        return new SessionResponseMapper().mapToDTO(session, bidCount);
    }

    private static boolean isUuid(String str) {
        if (str == null || str.length() != 36) {
            return false;
        }
        try {
            java.util.UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
