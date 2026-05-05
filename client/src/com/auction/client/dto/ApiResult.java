package com.auction.client.dto;

public class ApiResult {
    public final boolean success;
    public final String message;

    public ApiResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}