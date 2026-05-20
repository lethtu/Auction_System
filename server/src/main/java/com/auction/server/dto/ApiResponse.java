package com.auction.server.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    private static final Logger logger = LoggerFactory.getLogger(ApiResponse.class);

    public ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        logger.info("Trạng thái phản hồi: {}, tin nhắn: {}", status, message);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}