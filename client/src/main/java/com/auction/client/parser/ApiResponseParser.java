package com.auction.client.parser;

import com.auction.client.dto.ApiResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ApiResponseParser {
    private static final String INVALID_RESPONSE_MESSAGE = "Phản hồi từ server không hợp lệ.";

    private ApiResponseParser() {
    }

    public static ApiResult<Void> parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        if (isBlank(body)) {
            return new ApiResult<>(
                    isSuccess(httpStatus),
                    httpStatus,
                    defaultMessage(httpStatus, defaultSuccessMessage),
                    null
            );
        }

        JSONObject json = parseJsonObject(body);
        if (json == null) {
            return new ApiResult<>(false, httpStatus, INVALID_RESPONSE_MESSAGE, null);
        }

        int status = json.optInt("status", httpStatus);
        String message = json.optString("message", defaultMessage(status, defaultSuccessMessage));

        return new ApiResult<>(isSuccess(status), status, message, null);
    }

    public static ApiResult<JSONArray> extractDataArray(String body, int httpStatus) {
        if (isBlank(body)) {
            return new ApiResult<>(false, httpStatus, "Không có dữ liệu từ server.", new JSONArray());
        }

        JSONObject json = parseJsonObject(body);
        if (json == null) {
            return new ApiResult<>(false, httpStatus, INVALID_RESPONSE_MESSAGE, new JSONArray());
        }

        int status = json.optInt("status", httpStatus);
        String message = json.optString("message", defaultMessage(status, "Lấy dữ liệu thành công."));

        if (!isSuccess(status)) {
            return new ApiResult<>(false, status, message, new JSONArray());
        }

        JSONArray data = json.optJSONArray("data");
        return new ApiResult<>(true, status, message, data == null ? new JSONArray() : data);
    }

    public static ApiResult<JSONObject> extractDataObject(String body, int httpStatus) {
        if (isBlank(body)) {
            return new ApiResult<>(false, httpStatus, "Không có dữ liệu từ server.", new JSONObject());
        }

        JSONObject json = parseJsonObject(body);
        if (json == null) {
            return new ApiResult<>(false, httpStatus, INVALID_RESPONSE_MESSAGE, new JSONObject());
        }

        int status = json.optInt("status", httpStatus);
        String message = json.optString("message", defaultMessage(status, "Lấy dữ liệu thành công."));

        if (!isSuccess(status)) {
            return new ApiResult<>(false, status, message, new JSONObject());
        }

        JSONObject data = json.optJSONObject("data");
        return new ApiResult<>(true, status, message, data == null ? new JSONObject() : data);
    }

    private static JSONObject parseJsonObject(String body) {
        try {
            return new JSONObject(body);
        } catch (JSONException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static String defaultMessage(int status, String defaultSuccessMessage) {
        return isSuccess(status) ? defaultSuccessMessage : "Thao tác thất bại.";
    }
}
