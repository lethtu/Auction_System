package com.auction.client.dto;

public class ApiResult<T> {
    public final boolean success;
    public final String message;
    public final T data;

    public ApiResult(boolean success, String message) {
        this(success, message, null);
    }

    public ApiResult(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}