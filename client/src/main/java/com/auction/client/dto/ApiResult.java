package com.auction.client.dto;

public final class ApiResult<T> {
    private static final int DEFAULT_SUCCESS_STATUS = 200;
    private static final int DEFAULT_ERROR_STATUS = 500;

    public final boolean success;
    public final int status;
    public final String message;
    public final T data;

    public ApiResult(boolean success, String message) {
        this(success, defaultStatus(success), message, null);
    }

    public ApiResult(boolean success, String message, T data) {
        this(success, defaultStatus(success), message, data);
    }

    public ApiResult(boolean success, int status, String message, T data) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> success(String message) {
        return new ApiResult<>(true, message);
    }

    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(true, message, data);
    }

    public static <T> ApiResult<T> error(String message) {
        return new ApiResult<>(false, message);
    }

    public static <T> ApiResult<T> error(int status, String message) {
        return new ApiResult<>(false, status, message, null);
    }

    public boolean hasData() {
        return data != null;
    }

    private static int defaultStatus(boolean success) {
        return success ? DEFAULT_SUCCESS_STATUS : DEFAULT_ERROR_STATUS;
    }
}