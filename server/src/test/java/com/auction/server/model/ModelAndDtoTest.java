package com.auction.server.model;

import com.auction.server.dto.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ModelAndDtoTest {

    @Test
    public void testArt() {
        Art art = new Art();
        art.setId(1);
        art.setName("Mona Lisa");
        assertEquals(1, art.getId());
        assertEquals("Mona Lisa", art.getName());
        assertEquals("Art", art.getCategoryInfo());
    }

    @Test
    public void testElectronics() {
        Electronics el = new Electronics();
        el.setId(2);
        el.setName("iPhone");
        assertEquals(2, el.getId());
        assertEquals("iPhone", el.getName());
        assertEquals("Electronics", el.getCategoryInfo());
    }

    @Test
    public void testVehicle() {
        Vehicle v = new Vehicle();
        v.setId(3);
        v.setName("Tesla");
        assertEquals(3, v.getId());
        assertEquals("Tesla", v.getName());
        assertEquals("Vehicle", v.getCategoryInfo());
    }

    @Test
    public void testAutoBidConfig() {
        AutoBidConfig config1 = new AutoBidConfig();
        config1.setId(10);
        config1.setSessionId(20);
        config1.setBidderId(30);
        config1.setMaxBid(BigDecimal.valueOf(1000));
        config1.setIncrement(BigDecimal.valueOf(50));
        config1.setActive(true);

        assertEquals(10, config1.getId());
        assertEquals(20, config1.getSessionId());
        assertEquals(30, config1.getBidderId());
        assertEquals(BigDecimal.valueOf(1000), config1.getMaxBid());
        assertEquals(BigDecimal.valueOf(50), config1.getIncrement());
        assertTrue(config1.isActive());

        AutoBidConfig config2 = new AutoBidConfig(21, 31, BigDecimal.valueOf(2000), BigDecimal.valueOf(100));
        assertEquals(21, config2.getSessionId());
        assertEquals(31, config2.getBidderId());
        assertEquals(BigDecimal.valueOf(2000), config2.getMaxBid());
        assertEquals(BigDecimal.valueOf(100), config2.getIncrement());
        assertTrue(config2.isActive());

        String str = config2.toString();
        assertTrue(str.contains("sessionId=21"));
    }

    @Test
    public void testBid() {
        AuctionSession session = new AuctionSession();
        User bidder = new User();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = BigDecimal.valueOf(500);

        Bid bid1 = new Bid();
        bid1.setId(1);
        bid1.setSession(session);
        bid1.setBidder(bidder);
        bid1.setAmount(amount);
        bid1.setTime(now);

        assertEquals(1, bid1.getId());
        assertEquals(session, bid1.getSession());
        assertEquals(bidder, bid1.getBidder());
        assertEquals(amount, bid1.getAmount());
        assertEquals(now, bid1.getTime());

        Bid bid2 = new Bid(session, bidder, amount, now);
        assertEquals(session, bid2.getSession());
        assertEquals(bidder, bid2.getBidder());
        assertEquals(amount, bid2.getAmount());
        assertEquals(now, bid2.getTime());
    }

    @Test
    public void testBidHistoryDTO() {
        BidHistoryDTO dto1 = new BidHistoryDTO();
        dto1.setBidId(1);
        dto1.setSessionId(2);
        dto1.setBidderId(3);
        dto1.setBidderName("Name");
        dto1.setAmount(BigDecimal.valueOf(100));
        dto1.setBidTime("Time");

        assertEquals(1, dto1.getBidId());
        assertEquals(2, dto1.getSessionId());
        assertEquals(3, dto1.getBidderId());
        assertEquals("Name", dto1.getBidderName());
        assertEquals(BigDecimal.valueOf(100), dto1.getAmount());
        assertEquals("Time", dto1.getBidTime());

        BidHistoryDTO dto2 = new BidHistoryDTO(10, 20, 30, "Name2", BigDecimal.valueOf(200), "Time2");
        assertEquals(10, dto2.getBidId());
        assertEquals(20, dto2.getSessionId());
        assertEquals(30, dto2.getBidderId());
        assertEquals("Name2", dto2.getBidderName());
        assertEquals(BigDecimal.valueOf(200), dto2.getAmount());
        assertEquals("Time2", dto2.getBidTime());
    }

    @Test
    public void testPriceUpdateNotification() {
        PriceUpdateNotification notif = new PriceUpdateNotification(5, BigDecimal.valueOf(1500), "New Price!");
        assertEquals(5, notif.getAuctionSessionId());
        assertEquals(BigDecimal.valueOf(1500), notif.getNewPrice());
        assertEquals("New Price!", notif.getMessage());
        
        String str = notif.toString();
        assertTrue(str.contains("Session 5"));
        assertTrue(str.contains("1500"));
    }

    @Test
    public void testBidResponse() {
        BidResponse response = new BidResponse(true, "Msg", BigDecimal.valueOf(1200), "2026-05-25", 99, 10, "12:00", 1, 98);
        assertTrue(response.isSuccess());
        assertEquals("Msg", response.getMessage());
        assertEquals(BigDecimal.valueOf(1200), response.getCurrentPrice());
        assertEquals("2026-05-25", response.getNewEndTime());
        assertEquals(99, response.getHighestBidderId());
        assertEquals(10, response.getBidCount());
        assertEquals("12:00", response.getBidTime());
        assertEquals(1, response.getBidId());
        assertEquals(98, response.getPreviousHighestBidderId());

        BidResponse response2 = BidResponse.success("Ok", BigDecimal.ONE);
        assertTrue(response2.isSuccess());

        BidResponse response3 = BidResponse.success("Ok", BigDecimal.ONE, "end", 2, 3);
        assertTrue(response3.isSuccess());

        BidResponse response4 = BidResponse.failure("Failed", BigDecimal.ZERO);
        assertFalse(response4.isSuccess());
    }

    @Test
    public void testUserResponseDTO() {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(1);
        dto.setUsername("user");
        dto.setFullname("Full Name");
        dto.setEmail("email@com");
        dto.setAccountType("Role");
        dto.setBalance(BigDecimal.TEN);
        dto.setBanned(true);

        assertEquals(1, dto.getId());
        assertEquals("user", dto.getUsername());
        assertEquals("Full Name", dto.getFullname());
        assertEquals("email@com", dto.getEmail());
        assertEquals("Role", dto.getAccountType());
        assertEquals(BigDecimal.TEN, dto.getBalance());
        assertTrue(dto.isBanned());

        UserResponseDTO dto2 = new UserResponseDTO(2, "user2", "Full Name 2", "email2@com", "Role2", BigDecimal.ONE, false);
        assertEquals(2, dto2.getId());
        assertEquals("user2", dto2.getUsername());
        assertEquals("Full Name 2", dto2.getFullname());
        assertEquals("email2@com", dto2.getEmail());
        assertEquals("Role2", dto2.getAccountType());
        assertEquals(BigDecimal.ONE, dto2.getBalance());
        assertFalse(dto2.isBanned());
    }
}
