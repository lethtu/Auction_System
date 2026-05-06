package com.auction.client.parser;

import com.auction.client.dto.ApiArrayResult;
import com.auction.client.dto.ApiResult;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ApiResponseParser {
    private static final String DEFAULT_ERROR_MESSAGE = "Có lỗi xảy ra từ server.";

    private ApiResponseParser() {
    }

    public static ApiResult parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        JSONObject obj = parseJson(body);

        if (obj == null) {
            return new ApiResult(isSuccess(httpStatus), defaultMessage(httpStatus, defaultSuccessMessage));
        }

        int status = obj.optInt("status", httpStatus);
        String message = obj.optString("message", defaultMessage(status, defaultSuccessMessage));

        return new ApiResult(isSuccess(status), message);
    }

    public static ApiArrayResult extractDataArray(String body, int httpStatus) {
        JSONObject obj = parseJson(body);

        if (obj == null) {
            return new ApiArrayResult(false, "Không có dữ liệu từ server.", new JSONArray());
        }

        int status = obj.optInt("status", httpStatus);
        String message = obj.optString("message", defaultMessage(status, "OK"));

        if (!isSuccess(status)) {
            return new ApiArrayResult(false, message, new JSONArray());
        }

        JSONArray data = obj.optJSONArray("data");
        return new ApiArrayResult(true, message, data == null ? new JSONArray() : data);
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

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static String defaultMessage(int status, String successMessage) {
        return isSuccess(status) ? successMessage : DEFAULT_ERROR_MESSAGE;
    }
}