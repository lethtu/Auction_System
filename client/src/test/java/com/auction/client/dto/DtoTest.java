package com.auction.client.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class DtoTest {

    @Test
    public void testApiResult() {
        ApiResult<String> r1 = ApiResult.success("Ok");
        assertTrue(r1.success);
        assertEquals(200, r1.status);
        assertEquals("Ok", r1.message);
        assertNull(r1.data);
        assertFalse(r1.hasData());

        ApiResult<String> r2 = ApiResult.success("OkData", "data123");
        assertTrue(r2.success);
        assertEquals("data123", r2.data);
        assertTrue(r2.hasData());

        ApiResult<Object> r3 = ApiResult.error("Err");
        assertFalse(r3.success);
        assertEquals(500, r3.status);
        assertEquals("Err", r3.message);

        ApiResult<Object> r4 = ApiResult.error(404, "Not Found");
        assertFalse(r4.success);
        assertEquals(404, r4.status);
        assertEquals("Not Found", r4.message);
    }

    @Test
    public void testBidRequest() {
        BidRequest req = new BidRequest(10, 20, new BigDecimal("100.50"));
        assertEquals(10, req.getAuctionId());
        assertEquals(20, req.getBidderId());
        assertEquals(new BigDecimal("100.50"), req.getBidAmount());
        assertNotNull(req.toString());
    }

    @Test
    public void testBidResponse() {
        BidResponse res = new BidResponse(true, "Success", new BigDecimal("150"));
        assertTrue(res.isSuccess());
        assertEquals("Success", res.getMessage());
        assertEquals(new BigDecimal("150"), res.getCurrentPrice());
    }

    @Test
    public void testCreateAuctionRequest() {
        CreateAuctionRequest req1 = new CreateAuctionRequest("Name", "Type", "Desc", new BigDecimal("10"), new BigDecimal("1"), "2026-05-25T12:00:00", "2026-05-25T13:00:00", 5);
        assertEquals("Name", req1.productName);
        assertNull(req1.imagePath);
        assertFalse(req1.hasImagePath());
        assertFalse(req1.hasReservePrice());

        CreateAuctionRequest req2 = new CreateAuctionRequest("Name", "Type", "Desc", "path/to/img", new BigDecimal("10"), new BigDecimal("1"), "2026-05-25T12:00:00", "2026-05-25T13:00:00", 5);
        assertTrue(req2.hasImagePath());
        assertEquals("path/to/img", req2.imagePath);
        assertFalse(req2.hasReservePrice());

        CreateAuctionRequest req3 = new CreateAuctionRequest("Name", "Type", "Desc", "path/to/img", new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("20"), "2026-05-25T12:00:00", "2026-05-25T13:00:00", 5);
        assertTrue(req3.hasReservePrice());
        assertEquals(new BigDecimal("20"), req3.reservePrice);

        CreateAuctionRequest req4 = new CreateAuctionRequest("Name", "Type", "Desc", "path/to/img", new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("20"), "2026-05-25T12:00:00", "2026-05-25T13:00:00", 5, true, new BigDecimal("0.05"));
        assertTrue(req4.applyMinRate);
        assertEquals(new BigDecimal("0.05"), req4.minRate);
    }
}
