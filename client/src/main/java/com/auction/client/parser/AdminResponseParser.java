package com.auction.client.parser;

import com.auction.client.dto.ApiResult;
import com.auction.client.model.AdminSessionRow;
import com.auction.client.model.AdminUserRow;
import com.auction.client.model.PendingSessionRow;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AdminResponseParser {
    private static final String KEY_ID = "id";
    private static final String KEY_PRODUCT_ID = "productId";
    private static final String KEY_PRODUCT_NAME = "productName";
    private static final String KEY_PRODUCT_VISIBLE = "productVisible";
    private static final String KEY_SELLER_USERNAME = "sellerUsername";
    private static final String KEY_STARTING_PRICE = "startingPrice";
    private static final String KEY_CURRENT_PRICE = "currentPrice";
    private static final String KEY_STATUS = "status";

    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ACCOUNT_TYPE = "accountType";
    private static final String KEY_BANNED = "banned";

    private static final String DEFAULT_UNKNOWN_TEXT = "Unknown";
    private static final String DEFAULT_EMPTY_TEXT = "";
    private static final String DEFAULT_UNKNOWN_STATUS = "UNKNOWN";

    private AdminResponseParser() {
    }

    public static ApiResult<Void> parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        return ApiResponseParser.parseApiResponse(body, httpStatus, defaultSuccessMessage);
    }

    public static ApiResult<JSONArray> extractDataArray(String body, int httpStatus) {
        return ApiResponseParser.extractDataArray(body, httpStatus);
    }

    public static List<PendingSessionRow> parsePendingSessions(JSONArray array) {
        return parseRows(array, AdminResponseParser::toPendingSessionRow);
    }

    public static List<AdminSessionRow> parseAllSessions(JSONArray array) {
        return parseRows(array, AdminResponseParser::toAdminSessionRow);
    }

    public static List<AdminUserRow> parseUsers(JSONArray array) {
        return parseRows(array, AdminResponseParser::toAdminUserRow);
    }

    private static PendingSessionRow toPendingSessionRow(JSONObject item) {
        return new PendingSessionRow(
                item.optInt(KEY_ID, 0),
                item.optString(KEY_PRODUCT_NAME, DEFAULT_UNKNOWN_TEXT),
                parseBigDecimal(item, KEY_STARTING_PRICE)
        );
    }

    private static AdminSessionRow toAdminSessionRow(JSONObject item) {
        BigDecimal startingPrice = parseBigDecimal(item, KEY_STARTING_PRICE);
        BigDecimal currentPrice = parseBigDecimal(item, KEY_CURRENT_PRICE);
        if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            currentPrice = startingPrice;
        }

        return new AdminSessionRow(
                item.optInt(KEY_ID, 0),
                item.optInt(KEY_PRODUCT_ID, 0),
                item.optString(KEY_PRODUCT_NAME, DEFAULT_UNKNOWN_TEXT),
                item.optString(KEY_SELLER_USERNAME, DEFAULT_UNKNOWN_TEXT),
                startingPrice,
                currentPrice,
                item.optString(KEY_STATUS, DEFAULT_UNKNOWN_STATUS),
                item.optBoolean(KEY_PRODUCT_VISIBLE, true)
        );
    }

    private static AdminUserRow toAdminUserRow(JSONObject user) {
        return new AdminUserRow(
                user.optInt(KEY_ID, 0),
                user.optString(KEY_USERNAME, DEFAULT_EMPTY_TEXT),
                user.optString(KEY_FULLNAME, DEFAULT_EMPTY_TEXT),
                user.optString(KEY_EMAIL, DEFAULT_EMPTY_TEXT),
                user.optString(KEY_ACCOUNT_TYPE, DEFAULT_EMPTY_TEXT),
                user.optBoolean(KEY_BANNED, false)
        );
    }

    private static <T> List<T> parseRows(JSONArray array, JsonRowMapper<T> mapper) {
        List<T> rows = new ArrayList<>();

        if (array == null) {
            return rows;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);

            if (item == null) {
                continue;
            }

            rows.add(mapper.map(item));
        }

        return rows;
    }

    private static BigDecimal parseBigDecimal(JSONObject item, String key) {
        if (item == null || !item.has(key) || item.isNull(key)) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(item.get(key).toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @FunctionalInterface
    private interface JsonRowMapper<T> {
        T map(JSONObject item);
    }
}