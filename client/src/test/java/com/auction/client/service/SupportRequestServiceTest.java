package com.auction.client.service;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SupportRequestServiceTest {

    private final SupportRequestService service = new SupportRequestService();

    @Test
    void buildMailtoUri_trimsAndEncodesSupportRequest() {
        URI uri = service.buildMailtoUri(
                "  minh+test@example.com  ",
                "  Cannot bid / urgent?  ",
                "  Price is wrong: 1,000,000 VND & status = ACTIVE  "
        );

        assertEquals("mailto", uri.getScheme());
        assertTrue(uri.getRawSchemeSpecificPart().startsWith(SupportRequestService.SUPPORT_EMAIL + "?"));
        assertFalse(uri.getRawSchemeSpecificPart().contains(" "), "Mailto URI must not contain raw spaces.");
        assertFalse(uri.getRawSchemeSpecificPart().contains("+"), "Spaces should be encoded as %20, not '+'.");

        Map<String, String> query = parseMailtoQuery(uri);
        assertEquals("[Auction Support] Cannot bid / urgent?", query.get("subject"));
        assertEquals(
                "Contact email: minh+test@example.com\n\nPrice is wrong: 1,000,000 VND & status = ACTIVE",
                query.get("body")
        );
    }

    @Test
    void buildMailtoUri_handlesNullValuesWithoutLiteralNullText() {
        URI uri = service.buildMailtoUri(null, null, null);

        Map<String, String> query = parseMailtoQuery(uri);
        assertEquals("[Auction Support]", query.get("subject"));
        assertEquals("Contact email:", query.get("body"));
        assertFalse(uri.toString().toLowerCase().contains("null"));
    }

    @Test
    void buildManualSendInstruction_trimsSubjectAndKeepsSupportEmailVisible() {
        String instruction = service.buildManualSendInstruction("  Login failed  ");

        assertEquals(
                "No email app is available on this device. Please send your support request manually to "
                        + SupportRequestService.SUPPORT_EMAIL
                        + " with subject: Login failed",
                instruction
        );
    }

    private Map<String, String> parseMailtoQuery(URI uri) {
        String rawSpecificPart = uri.getRawSchemeSpecificPart();
        int queryStart = rawSpecificPart.indexOf('?');
        assertTrue(queryStart > 0, "Mailto URI should contain a query string.");
        assertEquals(SupportRequestService.SUPPORT_EMAIL, rawSpecificPart.substring(0, queryStart));

        String rawQuery = rawSpecificPart.substring(queryStart + 1);
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            int equalsIndex = pair.indexOf('=');
            assertTrue(equalsIndex > 0, "Malformed query pair: " + pair);
            String key = decode(pair.substring(0, equalsIndex));
            String value = decode(pair.substring(equalsIndex + 1));
            result.put(key, value);
        }
        return result;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
