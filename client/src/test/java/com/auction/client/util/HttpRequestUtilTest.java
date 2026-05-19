package com.auction.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpRequestUtilTest {

    @Test
    void encode_null_returnsEmptyString() {
        assertEquals("", HttpRequestUtil.encode(null));
    }

    @Test
    void encode_vietnameseAndSpaces_returnsUrlEncodedText() {
        assertEquals("Sai+th%C3%B4ng+tin+%C4%91%E1%BA%A5u+gi%C3%A1", HttpRequestUtil.encode("Sai thông tin đấu giá"));
    }
}
