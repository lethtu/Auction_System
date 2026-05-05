package com.auction.client.dto;

import org.json.JSONArray;

public class ApiArrayResult {
    public final boolean success;
    public final String message;
    public final JSONArray data;

    public ApiArrayResult(boolean success, String message, JSONArray data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}