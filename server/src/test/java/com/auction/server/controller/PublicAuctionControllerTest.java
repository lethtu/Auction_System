package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Electronics;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicAuctionControllerTest {

    @Mock
    private AuctionSessionRepository auctionSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void getAllSessions_returnsDtoListForMainAuctionScreen() {
        AuctionSession session = new AuctionSession();
        session.setId(21);
        session.setStatus(AuctionStatus.ACTIVE);
        session.setStartingPrice(new BigDecimal("18000000"));
        session.setCurrentPrice(new BigDecimal("18500000"));
        session.setStepPrice(new BigDecimal("500000"));
        session.setReservePrice(new BigDecimal("18000000"));

        Electronics item = new Electronics();
        item.setName("Laptop test");
        item.setType("Electronics");
        item.setDescription("Test item");
        item.setImagePath("laptop.png");
        session.setItem(item);

        when(auctionSessionRepository.findAll()).thenReturn(List.of(session));

        PublicAuctionController controller =
                new PublicAuctionController(auctionSessionRepository, userRepository);

        ResponseEntity<ApiResponse<List<SessionResponseDTO>>> response =
                controller.getAllSessions();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getStatus());
        assertEquals(1, response.getBody().getData().size());

        SessionResponseDTO dto = response.getBody().getData().get(0);
        assertEquals(21, dto.getId());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals("Laptop test", dto.getProductName());
        assertEquals("Electronics", dto.getProductType());
        assertEquals(new BigDecimal("18500000"), dto.getCurrentPrice());
        assertEquals(new BigDecimal("500000"), dto.getStepPrice());
    }
}