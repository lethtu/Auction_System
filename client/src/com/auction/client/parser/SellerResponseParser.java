package com.auction.client.parser;

import com.auction.client.dto.ApiResult;
import com.auction.client.model.SessionItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class SellerResponseParser {

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

        session.id = item.optInt("id", 0);
        session.productName = item.optString("productName", "Không rõ");
        session.productType = item.optString("productType", "");
        session.imageUrl = item.optString("imageUrl", "");
        session.description = item.optString("description", "");
        session.startingPrice = parseBigDecimal(item, "startingPrice");
        session.currentPrice = parseBigDecimal(item, "currentPrice");
        session.stepPrice = parseBigDecimal(item, "stepPrice");
        session.endTime = item.optString("endTime", "");
        session.status = item.optString("status", "UNKNOWN");

        return session;
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