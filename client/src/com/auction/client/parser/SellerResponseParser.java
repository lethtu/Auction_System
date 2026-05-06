package com.auction.client.parser;

import com.auction.client.dto.ApiArrayResult;
import com.auction.client.dto.ApiResult;
import com.auction.client.model.SessionItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class SellerResponseParser {
    private static final String DEFAULT_ERROR_MESSAGE = "Có lỗi xảy ra từ server.";

    private SellerResponseParser() {
    }

    public static ApiResult parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        JSONObject obj = parseJson(body);

        if (obj == null) {
            return new ApiResult(isSuccess(httpStatus), defaultMessage(httpStatus, defaultSuccessMessage));
        }

        int status = readStatus(obj, httpStatus);
        String message = readMessage(obj, status, defaultSuccessMessage);

        return new ApiResult(isSuccess(status), message);
    }

    public static ApiArrayResult extractDataArray(String body, int httpStatus) {
        JSONObject obj = parseJson(body);

        if (obj == null) {
            return new ApiArrayResult(false, "Không có dữ liệu từ server.", new JSONArray());
        }

        int status = readStatus(obj, httpStatus);
        String message = readMessage(obj, status, "OK");

        if (!isSuccess(status)) {
            return new ApiArrayResult(false, message, new JSONArray());
        }

        JSONArray data = obj.optJSONArray("data");
        return new ApiArrayResult(true, message, data == null ? new JSONArray() : data);
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

    private static JSONObject parseJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        try {
            return new JSONObject(body.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static int readStatus(JSONObject obj, int fallbackStatus) {
        return obj.optInt("status", fallbackStatus);
    }

    private static String readMessage(JSONObject obj, int status, String successMessage) {
        return obj.optString("message", defaultMessage(status, successMessage));
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

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static String defaultMessage(int status, String successMessage) {
        return isSuccess(status) ? successMessage : DEFAULT_ERROR_MESSAGE;
    }
}