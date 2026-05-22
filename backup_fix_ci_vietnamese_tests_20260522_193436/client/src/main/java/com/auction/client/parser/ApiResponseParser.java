package com.auction.client.parser;

import com.auction.client.dto.ApiResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ApiResponseParser {
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_DATA = "data";

    private static final String INVALID_RESPONSE_MESSAGE = "Invalid response from server.";
    private static final String NO_DATA_MESSAGE = "No data from server.";
    private static final String DEFAULT_DATA_SUCCESS_MESSAGE = "Data retrieved successfully.";
    private static final String DEFAULT_FAILURE_MESSAGE = "Operation failed.";

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

        int status = extractStatus(json, httpStatus);
        String message = extractMessage(json, status, defaultSuccessMessage);

        return new ApiResult<>(isSuccess(status), status, message, null);
    }

    public static ApiResult<JSONArray> extractDataArray(String body, int httpStatus) {
        return extractData(
                body,
                httpStatus,
                JSONArray::new,
                json -> json.optJSONArray(FIELD_DATA)
        );
    }

    public static ApiResult<JSONObject> extractDataObject(String body, int httpStatus) {
        return extractData(
                body,
                httpStatus,
                JSONObject::new,
                json -> json.optJSONObject(FIELD_DATA)
        );
    }

    private static <T> ApiResult<T> extractData(
            String body,
            int httpStatus,
            Supplier<T> emptyDataSupplier,
            Function<JSONObject, T> dataExtractor
    ) {
        if (isBlank(body)) {
            return new ApiResult<>(false, httpStatus, NO_DATA_MESSAGE, emptyDataSupplier.get());
        }

        JSONObject json = parseJsonObject(body);
        if (json == null) {
            return new ApiResult<>(false, httpStatus, INVALID_RESPONSE_MESSAGE, emptyDataSupplier.get());
        }

        int status = extractStatus(json, httpStatus);
        String message = extractMessage(json, status, DEFAULT_DATA_SUCCESS_MESSAGE);

        if (!isSuccess(status)) {
            return new ApiResult<>(false, status, message, emptyDataSupplier.get());
        }

        T data = dataExtractor.apply(json);
        return new ApiResult<>(true, status, message, data == null ? emptyDataSupplier.get() : data);
    }

    private static JSONObject parseJsonObject(String body) {
        try {
            return new JSONObject(body);
        } catch (JSONException e) {
            return null;
        }
    }

    private static int extractStatus(JSONObject json, int defaultStatus) {
        return json.optInt(FIELD_STATUS, defaultStatus);
    }

    private static String extractMessage(JSONObject json, int status, String defaultSuccessMessage) {
        return json.optString(FIELD_MESSAGE, defaultMessage(status, defaultSuccessMessage));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static String defaultMessage(int status, String defaultSuccessMessage) {
        if (isSuccess(status)) {
            return defaultSuccessMessage;
        }

        if (status == 404) {
            return "Operation failed: server does not have this API or server has not been restarted after applying patch. HTTP 404.";
        }

        if (status >= 400) {
            return DEFAULT_FAILURE_MESSAGE + " HTTP " + status + ".";
        }

        return DEFAULT_FAILURE_MESSAGE;
    }
}