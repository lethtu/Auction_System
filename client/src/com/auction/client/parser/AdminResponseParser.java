package com.auction.client.parser;

import com.auction.client.dto.ApiArrayResult;
import com.auction.client.dto.ApiResult;
import com.auction.client.model.PendingSessionRow;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AdminResponseParser {

    private AdminResponseParser() {
    }

    public static ApiResult parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        return ApiResponseParser.parseApiResponse(body, httpStatus, defaultSuccessMessage);
    }

    public static ApiArrayResult extractDataArray(String body, int httpStatus) {
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
                rows.add(parsePendingSession(item));
            }
        }

        return rows;
    }

    private static PendingSessionRow parsePendingSession(JSONObject item) {
        int id = item.optInt("id", 0);
        String productName = item.optString("productName", "Không rõ");
        BigDecimal startingPrice = parseBigDecimal(item, "startingPrice");

        return new PendingSessionRow(id, productName, startingPrice);
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