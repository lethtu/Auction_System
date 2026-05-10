package com.auction.client.dto;

public class ApiResult<T> {
    public final boolean success;
    public final int status;
    public final String message;
    public final T data;

    public ApiResult(boolean success, String message) {
        this.success = success;
        this.status = success ? 200 : 500;
        this.message = message;
        this.data = null;
    }

    public ApiResult(boolean success, String message, T data) {
        this.success = success;
        this.status = success ? 200 : 500;
        this.message = message;
        this.data = data;
    }

    public ApiResult(boolean success, int status, String message, T data) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
    }
}