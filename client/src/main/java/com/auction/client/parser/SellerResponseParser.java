package com.auction.client.parser;

import com.auction.client.dto.ApiResult;
import com.auction.client.model.SessionItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class SellerResponseParser {

    private static final String KEY_ID = "id";
    private static final String KEY_PRODUCT_NAME = "productName";
    private static final String KEY_PRODUCT_TYPE = "productType";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_IMAGE_PATH = "imagePath";
    private static final String KEY_STARTING_PRICE = "startingPrice";
    private static final String KEY_CURRENT_PRICE = "currentPrice";
    private static final String KEY_STEP_PRICE = "stepPrice";
    private static final String KEY_RESERVE_PRICE = "reservePrice";
    private static final String KEY_HIGHEST_BIDDER_ID = "highestBidderId";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_END_TIME = "endTime";
    private static final String KEY_STATUS = "status";

    private static final String DEFAULT_PRODUCT_NAME = "Không rõ";
    private static final String DEFAULT_STATUS = "UNKNOWN";
    private static final String DEFAULT_TEXT = "";
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.ZERO;

    private SellerResponseParser() {
    }

    public static ApiResult<Void> parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        return ApiResponseParser.parseApiResponse(body, httpStatus, defaultSuccessMessage);
    }

    public static ApiResult<JSONArray> extractDataArray(String body, int httpStatus) {
        return ApiResponseParser.extractDataArray(body, httpStatus);
    }

    public static List<SessionItem> parseSessions(JSONArray data) {
        List<SessionItem> sessions = new ArrayList<>();

        if (data == null) {
            return sessions;
        }

        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);

            if (item != null) {
                sessions.add(parseSession(item));
            }
        }

        return sessions;
    }

    private static SessionItem parseSession(JSONObject item) {
        SessionItem session = new SessionItem();

        session.id = item.optInt(KEY_ID, 0);
        session.productName = item.optString(KEY_PRODUCT_NAME, DEFAULT_PRODUCT_NAME);
        session.productType = item.optString(KEY_PRODUCT_TYPE, DEFAULT_TEXT);
        session.description = item.optString(KEY_DESCRIPTION, DEFAULT_TEXT);
        session.imagePath = item.optString(KEY_IMAGE_PATH, DEFAULT_TEXT);
        session.startingPrice = parseBigDecimal(item, KEY_STARTING_PRICE);
        session.currentPrice = parseBigDecimal(item, KEY_CURRENT_PRICE);
        session.stepPrice = parseBigDecimal(item, KEY_STEP_PRICE);
        session.reservePrice = parseBigDecimal(item, KEY_RESERVE_PRICE);
        session.highestBidderId = parseNullableInteger(item, KEY_HIGHEST_BIDDER_ID);
        session.startTime = item.optString(KEY_START_TIME, DEFAULT_TEXT);
        session.endTime = item.optString(KEY_END_TIME, DEFAULT_TEXT);
        session.status = item.optString(KEY_STATUS, DEFAULT_STATUS);
        session.applyMinRate = item.optBoolean("applyMinRate", false);
        session.minRate = parseBigDecimal(item, "minRate");

        return session;
    }

    private static Integer parseNullableInteger(JSONObject item, String key) {
        if (!hasValue(item, key)) {
            return null;
        }

        return item.optInt(key, 0);
    }

    private static BigDecimal parseBigDecimal(JSONObject item, String key) {
        if (!hasValue(item, key)) {
            return DEFAULT_PRICE;
        }

        try {
            return new BigDecimal(String.valueOf(item.opt(key)).trim());
        } catch (NumberFormatException e) {
            return DEFAULT_PRICE;
        }
    }

    private static boolean hasValue(JSONObject item, String key) {
        return item.has(key) && !item.isNull(key);
    }
}