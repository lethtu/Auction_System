package com.auction.client.util;

import com.auction.client.dto.CreateAuctionRequest;
import org.json.JSONObject;

public final class SellerRequestBuilder {

    private static final String KEY_NAME = "name";
    private static final String KEY_TYPE = "type";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_IMAGE_PATH = "imagePath";
    private static final String KEY_STARTING_PRICE = "startingPrice";
    private static final String KEY_STEP_PRICE = "stepPrice";
    private static final String KEY_RESERVE_PRICE = "reservePrice";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_END_TIME = "endTime";
    private static final String KEY_SELLER_ID = "sellerId";

    private SellerRequestBuilder() {
    }

    public static JSONObject buildAuctionBody(CreateAuctionRequest request) {
        JSONObject body = new JSONObject();

        body.put(KEY_NAME, request.productName);
        body.put(KEY_TYPE, request.productType);
        body.put(KEY_DESCRIPTION, request.description);
        putIfHasText(body, KEY_IMAGE_PATH, request.imagePath);

        body.put(KEY_STARTING_PRICE, request.startingPrice);
        body.put(KEY_STEP_PRICE, request.stepPrice);
        putIfNotNull(body, KEY_RESERVE_PRICE, request.reservePrice);

        body.put(KEY_START_TIME, request.startTime);
        body.put(KEY_END_TIME, request.endTime);
        body.put(KEY_SELLER_ID, request.sellerId);
        body.put("applyMinRate", request.applyMinRate != null ? request.applyMinRate : false);
        putIfNotNull(body, "minRate", request.minRate);

        return body;
    }

    private static void putIfHasText(JSONObject body, String key, String value) {
        String normalizedValue = trimToNull(value);

        if (normalizedValue != null) {
            body.put(key, normalizedValue);
        }
    }

    private static void putIfNotNull(JSONObject body, String key, Object value) {
        if (value != null) {
            body.put(key, value);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}