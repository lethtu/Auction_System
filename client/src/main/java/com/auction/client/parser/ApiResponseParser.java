package com.auction.client.parser;

import com.auction.client.dto.ApiResult;
import org.json.JSONArray;
import org.json.JSONObject;

public class ApiResponseParser {

    public static ApiResult<Void> parseApiResponse(String body, int httpStatus, String defaultSuccessMessage) {
        if (body == null || body.isBlank()) {
            return new ApiResult<>(
                    isSuccess(httpStatus),
                    httpStatus,
                    defaultMessage(httpStatus, defaultSuccessMessage),
                    null
            );
        }

        JSONObject json = new JSONObject(body);
        int status = json.optInt("status", httpStatus);
        String message = json.optString("message", defaultMessage(status, defaultSuccessMessage));

        return new ApiResult<>(
                isSuccess(status),
                status,
                message,
                null
        );
    }

    public static ApiResult<JSONArray> extractDataArray(String body, int httpStatus) {
        if (body == null || body.isBlank()) {
            return new ApiResult<>(
                    false,
                    httpStatus,
                    "Không có dữ liệu từ server.",
                    new JSONArray()
            );
        }

        JSONObject json = new JSONObject(body);
        int status = json.optInt("status", httpStatus);
        String message = json.optString("message", defaultMessage(status, "Lấy dữ liệu thành công."));

        if (!isSuccess(status)) {
            return new ApiResult<>(
                    false,
                    status,
                    message,
                    new JSONArray()
            );
        }

        JSONArray data = json.optJSONArray("data");

        return new ApiResult<>(
                true,
                status,
                message,
                data == null ? new JSONArray() : data
        );
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static String defaultMessage(int status, String defaultSuccessMessage) {
        if (isSuccess(status)) {
            return defaultSuccessMessage;
        }

        return "Thao tác thất bại.";
    }
}