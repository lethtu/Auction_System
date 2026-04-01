package com.auction.server.view;

public class ApiResponse<T>{
    private int stauts;
    private String message;
    private T data;

    public ApiResponse(int status, String message, T data){
        this.stauts = status;
        this.message = message;
        this.data = data;
    }

    public int getStatus(){
        return stauts;
    }

    public String getMessage(){
        return message;
    }

    public T getData(){
        return data;
    }
}
