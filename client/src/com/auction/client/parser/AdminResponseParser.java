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
            return new ApiResult(isSuccess(httpStatus), getDefaultMessage(httpStatus, defaultSuccessMessage));
        }

        try {
            JSONObject obj = new JSONObject(body.trim());

            int status = obj.optInt("status", httpStatus);
            String message = obj.optString("message", getDefaultMessage(status, defaultSuccessMessage));

            return new ApiResult(isSuccess(status), message);

        } catch (Exception e) {
            logger.error("Không đọc được phản hồi từ server: {}", e.getMessage(), e);
            return new ApiResult(isSuccess(httpStatus), getDefaultMessage(httpStatus, defaultSuccessMessage));
        }
    }

    public static ApiArrayResult extractDataArray(String body, int httpStatus) {
        if (body == null || body.isBlank()) {
            return new ApiArrayResult(false, "Không có dữ liệu từ server.", new JSONArray());
        }

        try {
            JSONObject obj = new JSONObject(body.trim());

            int status = obj.optInt("status", httpStatus);
            String message = obj.optString("message", getDefaultMessage(status, "OK"));

            if (!isSuccess(status)) {
                return new ApiArrayResult(false, message, new JSONArray());
            }

            JSONArray data = obj.optJSONArray("data");

            if (data == null) {
                return new ApiArrayResult(true, message, new JSONArray());
            }

            return new ApiArrayResult(true, message, data);

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
            String productName = item.optString("productName", "Không rõ");
            BigDecimal startingPrice = extractStartingPrice(item);

            rows.add(new PendingSessionRow(id, productName, startingPrice));
        }

        return rows;
    }

    private static BigDecimal extractStartingPrice(JSONObject item) {
        if (!item.has("startingPrice") || item.isNull("startingPrice")) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(item.get("startingPrice").toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static String getDefaultMessage(int status, String successMessage) {
        return isSuccess(status) ? successMessage : "Có lỗi xảy ra từ server.";
    }
}