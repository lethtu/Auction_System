package com.auction.client.parser;

import com.auction.client.dto.ApiArrayResult;
import com.auction.client.dto.ApiResult;
import com.auction.client.model.PendingSessionRow;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdminResponseParser {
    private static final Logger logger = LoggerFactory.getLogger(AdminResponseParser.class);

    public static ApiResult parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        if (body == null || body.isBlank()) {
            return new ApiResult(
                    httpStatus >= 200 && httpStatus < 300,
                    httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage : "Có lỗi xảy ra từ server."
            );
        }

        try {
            String trimmed = body.trim();

            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                int status = obj.optInt("status", httpStatus);
                String message = obj.optString(
                        "message",
                        status >= 200 && status < 300 ? defaultSuccessMessage : "Có lỗi xảy ra từ server."
                );

                return new ApiResult(status >= 200 && status < 300, message);
            }
        } catch (Exception ignored) {
        }

        return new ApiResult(
                httpStatus >= 200 && httpStatus < 300,
                httpStatus >= 200 && httpStatus < 300 ? defaultSuccessMessage : body
        );
    }

    public static ApiArrayResult extractDataArray(String body, int httpStatus) {
        if (body == null || body.isBlank()) {
            return new ApiArrayResult(false, "Không có dữ liệu từ server.", new JSONArray());
        }

        try {
            String trimmed = body.trim();

            if (trimmed.startsWith("[")) {
                return new ApiArrayResult(true, "OK", new JSONArray(trimmed));
            }

            JSONObject obj = new JSONObject(trimmed);
            int status = obj.optInt("status", httpStatus);
            String message = obj.optString("message", "Có lỗi xảy ra từ server.");

            if (status < 200 || status >= 300) {
                return new ApiArrayResult(false, message, new JSONArray());
            }

            Object data = obj.opt("data");

            if (data instanceof JSONArray) {
                return new ApiArrayResult(true, message, (JSONArray) data);
            }

            return new ApiArrayResult(true, message, new JSONArray());

        } catch (Exception e) {
            logger.error("Không đọc được dữ liệu từ server: {}", e.getMessage(), e);
            return new ApiArrayResult(false, "Không đọc được dữ liệu từ server.", new JSONArray());
        }
    }

    public static List<PendingSessionRow> parsePendingSessions(JSONArray array) {
        List<PendingSessionRow> rows = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);

            int id = item.optInt("id", 0);
            String productName = extractProductName(item);
            BigDecimal startingPrice = extractStartingPrice(item);

            rows.add(new PendingSessionRow(id, productName, startingPrice));
        }

        return rows;
    }

    private static String extractProductName(JSONObject item) {
        if (item.has("productName")) {
            return item.optString("productName", "Không rõ");
        }

        if (item.has("product") && item.get("product") instanceof JSONObject) {
            JSONObject product = item.getJSONObject("product");
            return product.optString("name", "Không rõ");
        }

        return "Không rõ";
    }

    private static BigDecimal extractStartingPrice(JSONObject item) {
        if (!item.has("startingPrice") || item.isNull("startingPrice")) {
            return BigDecimal.ZERO;
        }

        Object value = item.get("startingPrice");

        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}