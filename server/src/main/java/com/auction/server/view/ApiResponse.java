package com.auction.server.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiResponse<T>{
    private int status;
    private String message;
    private T data;
    private static final Logger logger = LoggerFactory.getLogger(ApiResponse.class);

    public ApiResponse(int status, String message, T data){
        this.status = status;
        this.message = message;
        this.data = data;
        logger.info("Trạng thái phản hồi: {}, tin nhắn: {}", status, message);
    }

    public int getStatus(){
        return status;
    }

    public String getMessage(){
        return message;
    }

    public T getData(){
        return data;
    }
}
