package com.auction.client.service;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportRequestServiceEdgeCaseTest {

    private final SupportRequestService service = new SupportRequestService();

    @Test
    void buildMailtoUri_encodesReservedCharactersWithoutBreakingQuery() {
        URI uri = service.buildMailtoUri(
                "  user+tag@example.com  ",
                "  Refund & invoice = #42?  ",
                "  Line 1\nLine 2 & total=100%  "
        );

        String raw = uri.toString();
        assertFalse(raw.contains(" "), "Mailto URI should not contain raw spaces.");
        assertTrue(raw.contains("%26"), "Ampersand in content should be encoded, not treated as a query separator.");
        assertTrue(raw.contains("%3D"), "Equals sign in content should be encoded.");
        assertTrue(raw.contains("%23"), "Hash in subject should be encoded.");
        assertTrue(raw.contains("%0A"), "Newlines in body should be encoded.");
        assertTrue(raw.contains("user%2Btag%40example.com"), "Plus sign in email should be preserved as encoded data.");

        Map<String, String> query = parseMailtoQuery(uri);
        assertEquals("[Auction Support] Refund & invoice = #42?", query.get("subject"));
        assertEquals("Contact email: user+tag@example.com\n\nLine 1\nLine 2 & total=100%", query.get("body"));
    }

    @Test
    void buildMailtoUri_blankValuesAreTrimmedToEmptyStrings() {
        URI uri = service.buildMailtoUri("   ", "   ", "   ");

        Map<String, String> query = parseMailtoQuery(uri);
        assertEquals("[Auction Support]", query.get("subject"));
        assertEquals("Contact email:", query.get("body"));
        assertFalse(uri.toString().toLowerCase().contains("null"));
    }

    @Test
    void buildManualSendInstruction_handlesNullAndBlankSubjectWithoutLiteralNull() {
        String nullSubjectInstruction = service.buildManualSendInstruction(null);
        String blankSubjectInstruction = service.buildManualSendInstruction("    ");

        String expected = "No email app is available on this device. Please send your support request manually to "
                + SupportRequestService.SUPPORT_EMAIL
                + " with subject: ";
        assertEquals(expected, nullSubjectInstruction);
        assertEquals(expected, blankSubjectInstruction);
        assertFalse(nullSubjectInstruction.toLowerCase().contains("null"));
        assertFalse(blankSubjectInstruction.toLowerCase().contains("null"));
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
