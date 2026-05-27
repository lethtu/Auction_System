package com.auction.client.service;

import com.auction.client.model.audio.SoundEvent;
import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationSeverity;
import com.auction.client.model.notification.NotificationType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NotificationCenterServiceHelperCoverageTest {

    private final NotificationCenterService service = NotificationCenterService.getInstance();

    @Test
    void extractAuctionArray_supportsDataArrayPagedContentAndInvalidShapes() throws Exception {
        JSONArray directArray = invokeExtractAuctionArray("""
                {"data":[{"id":1},{"id":2}]}
                """);
        assertEquals(2, directArray.length());
        assertEquals(1, directArray.getJSONObject(0).getInt("id"));

        JSONArray pagedContent = invokeExtractAuctionArray("""
                {"data":{"content":[{"id":7}]}}
                """);
        assertEquals(1, pagedContent.length());
        assertEquals(7, pagedContent.getJSONObject(0).getInt("id"));

        assertEquals(0, invokeExtractAuctionArray("{\"data\":{}}").length());
        assertEquals(0, invokeExtractAuctionArray("{\"data\":\"not-an-array\"}").length());
        assertEquals(0, invokeExtractAuctionArray("{\"status\":200}").length());
    }

    @Test
    void hasUserBid_supportsUserIdAndBidderIdAndIgnoresInvalidBids() throws Exception {
        JSONObject userIdSession = new JSONObject("""
                {"bids":[null,{"userId":12},{"userId":99}]}
                """);
        assertTrue(invokeHasUserBid(userIdSession, 12));
        assertFalse(invokeHasUserBid(userIdSession, 10));

        JSONObject bidderIdSession = new JSONObject("""
                {"bids":[{"bidderId":42}]}
                """);
        assertTrue(invokeHasUserBid(bidderIdSession, 42));
        assertFalse(invokeHasUserBid(new JSONObject("{}"), 42));
    }

    @Test
    void optionalInt_returnsNullForMissingAndJsonNullOtherwiseValue() throws Exception {
        JSONObject object = new JSONObject()
                .put("value", 55)
                .put("nullValue", JSONObject.NULL);

        assertEquals(55, invokeOptionalInt(object, "value"));
        assertNull(invokeOptionalInt(object, "missing"));
        assertNull(invokeOptionalInt(object, "nullValue"));
    }

    @Test
    void parseMoney_handlesNumbersStringsNullAndInvalidValues() throws Exception {
        assertEquals(new BigDecimal("120000"), invokeParseMoney(120000));
        assertEquals(new BigDecimal("12345.67"), invokeParseMoney("12345.67"));
        assertEquals(BigDecimal.ZERO, invokeParseMoney(null));
        assertEquals(BigDecimal.ZERO, invokeParseMoney(JSONObject.NULL));
        assertEquals(BigDecimal.ZERO, invokeParseMoney("not-money"));
    }

    @Test
    void mapToSoundEvent_mapsKnownNotificationTypesAndFallsBackToNotification() throws Exception {
        assertEquals(SoundEvent.OUTBID, invokeMapToSoundEvent(NotificationType.OUTBID));
        assertEquals(SoundEvent.BID_SUCCESS, invokeMapToSoundEvent(NotificationType.BID_SUCCESS));
        assertEquals(SoundEvent.BID_ERROR, invokeMapToSoundEvent(NotificationType.BID_FAILED));
        assertEquals(SoundEvent.AUCTION_ENDING_SOON, invokeMapToSoundEvent(NotificationType.ENDING_SOON));
        assertEquals(SoundEvent.AUCTION_WON, invokeMapToSoundEvent(NotificationType.AUCTION_END_WIN));
        assertEquals(SoundEvent.AUCTION_WON, invokeMapToSoundEvent(NotificationType.AUCTION_WON));
        assertEquals(SoundEvent.AUCTION_LOST, invokeMapToSoundEvent(NotificationType.AUCTION_END_LOSE));
        assertEquals(SoundEvent.AUCTION_LOST, invokeMapToSoundEvent(NotificationType.AUCTION_LOST));
        assertEquals(SoundEvent.NOTIFICATION, invokeMapToSoundEvent(NotificationType.GENERAL));
        assertEquals(SoundEvent.NOTIFICATION, invokeMapToSoundEvent(null));
    }

    @Test
    void buildDedupKey_usesTypeAuctionAndTitleButSkipsSystemError() throws Exception {
        AppNotification outbid = new AppNotification(
                NotificationType.OUTBID,
                NotificationSeverity.WARNING,
                "Price changed",
                "You are no longer highest bidder");
        outbid.setAuctionId(77);
        assertEquals("OUTBID_77_Price changed", invokeBuildDedupKey(outbid));

        AppNotification general = new AppNotification(
                NotificationType.GENERAL,
                NotificationSeverity.INFO,
                "Welcome",
                "Hello");
        assertEquals("GENERAL_none_Welcome", invokeBuildDedupKey(general));

        AppNotification error = new AppNotification(
                NotificationType.SYSTEM_ERROR,
                NotificationSeverity.DANGER,
                "Network error",
                "Retry later");
        assertNull(invokeBuildDedupKey(error));
    }

    private JSONArray invokeExtractAuctionArray(String responseBody) throws Exception {
        Method method = NotificationCenterService.class.getDeclaredMethod("extractAuctionArray", String.class);
        method.setAccessible(true);
        return (JSONArray) method.invoke(service, responseBody);
    }

    private boolean invokeHasUserBid(JSONObject session, int userId) throws Exception {
        Method method = NotificationCenterService.class.getDeclaredMethod("hasUserBid", JSONObject.class, int.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, session, userId);
    }

    private Integer invokeOptionalInt(JSONObject object, String key) throws Exception {
        Method method = NotificationCenterService.class.getDeclaredMethod("optionalInt", JSONObject.class, String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(service, object, key);
    }

    private BigDecimal invokeParseMoney(Object value) throws Exception {
        Method method = NotificationCenterService.class.getDeclaredMethod("parseMoney", Object.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, value);
    }

    private SoundEvent invokeMapToSoundEvent(NotificationType type) throws Exception {
        Method method = NotificationCenterService.class.getDeclaredMethod("mapToSoundEvent", NotificationType.class);
        method.setAccessible(true);
        return (SoundEvent) method.invoke(service, type);
    }

    private String invokeBuildDedupKey(AppNotification notification) throws Exception {
        Method method = NotificationCenterService.class.getDeclaredMethod("buildDedupKey", AppNotification.class);
        method.setAccessible(true);
        return (String) method.invoke(service, notification);
    }
}
