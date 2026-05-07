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

    private AdminResponseParser() {
    }

    public static ApiResult<Void> parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        return ApiResponseParser.parseApiResponse(body, httpStatus, defaultSuccessMessage);
    }

    public static ApiResult<JSONArray> extractDataArray(String body, int httpStatus) {
        return ApiResponseParser.extractDataArray(body, httpStatus);
    }

    public static List<PendingSessionRow> parsePendingSessions(JSONArray array) {
        List<PendingSessionRow> rows = new ArrayList<>();

        if (array == null) {
            return rows;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);

            if (item != null) {
                rows.add(new PendingSessionRow(
                        item.optInt("id", 0),
                        item.optString("productName", "Không rõ"),
                        parseBigDecimal(item, "startingPrice")
                ));
            }
        }

        return rows;
    }

    public static List<AdminSessionRow> parseAllSessions(JSONArray array) {
        List<AdminSessionRow> rows = new ArrayList<>();

        if (array == null) {
            return rows;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);

            if (item != null) {
                rows.add(new AdminSessionRow(
                        item.optInt("id", 0),
                        item.optString("productName", "Không rõ"),
                        item.optString("sellerUsername", "Không rõ"),
                        parseBigDecimal(item, "startingPrice"),
                        item.optString("status", "UNKNOWN")
                ));
            }
        }

        return rows;
    }

    public static List<AdminUserRow> parseUsers(JSONArray array) {
        List<AdminUserRow> rows = new ArrayList<>();

        if (array == null) {
            return rows;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject user = array.optJSONObject(i);

            if (user != null) {
                rows.add(new AdminUserRow(
                        user.optInt("id", 0),
                        user.optString("username", ""),
                        user.optString("fullname", ""),
                        user.optString("email", ""),
                        user.optString("accountType", ""),
                        user.optBoolean("banned", false)
                ));
            }
        }

        return rows;
    }

    private static BigDecimal parseBigDecimal(JSONObject item, String key) {
        if (!item.has(key) || item.isNull(key)) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(item.get(key).toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}