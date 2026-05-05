package com.auction.client.parser;

import com.auction.client.dto.ApiArrayResult;
import com.auction.client.dto.ApiResult;
import com.auction.client.model.SessionItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SellerResponseParser {
    public static ApiResult parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        if (body == null || body.isBlank()) {
            return new ApiResult(isSuccess(httpStatus), defaultMessage(httpStatus, defaultSuccessMessage));
        }

        try {
            JSONObject obj = new JSONObject(body.trim());
            int status = obj.optInt("status", httpStatus);
            String message = obj.optString("message", defaultMessage(status, defaultSuccessMessage));

            return new ApiResult(isSuccess(status), message);
        } catch (Exception e) {
            return new ApiResult(isSuccess(httpStatus), defaultMessage(httpStatus, defaultSuccessMessage));
        }
    }

    public static ApiArrayResult extractDataArray(String body, int httpStatus) {
        if (body == null || body.isBlank()) {
            return new ApiArrayResult(false, "Không có dữ liệu từ server.", new JSONArray());
        }

        try {
            JSONObject obj = new JSONObject(body.trim());
            int status = obj.optInt("status", httpStatus);
            String message = obj.optString("message", defaultMessage(status, "OK"));

            if (!isSuccess(status)) {
                return new ApiArrayResult(false, message, new JSONArray());
            }

            JSONArray data = obj.optJSONArray("data");
            return new ApiArrayResult(true, message, data == null ? new JSONArray() : data);

        } catch (Exception e) {
            return new ApiArrayResult(false, "Không đọc được dữ liệu từ server.", new JSONArray());
        }
    }

    public static List<SessionItem> parseSessions(JSONArray data) {
        List<SessionItem> sessions = new ArrayList<>();

        for (int i = 0; i < data.length(); i++) {
            sessions.add(parseSession(data.getJSONObject(i)));
        }

        return sessions;
    }

    private static SessionItem parseSession(JSONObject item) {
        SessionItem s = new SessionItem();

        s.id = item.optInt("id", 0);
        s.productName = item.optString("productName", "Không rõ");
        s.productType = item.optString("productType", "");
        s.imageUrl = item.optString("imageUrl", "");
        s.description = item.optString("description", "");
        s.startingPrice = parseBigDecimal(item, "startingPrice");
        s.currentPrice = parseBigDecimal(item, "currentPrice");
        s.stepPrice = parseBigDecimal(item, "stepPrice");
        s.endTime = item.optString("endTime", "");
        s.status = item.optString("status", "UNKNOWN");

        return s;
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
        return isSuccess(status) ? successMessage : "Có lỗi xảy ra từ server.";
    }
}