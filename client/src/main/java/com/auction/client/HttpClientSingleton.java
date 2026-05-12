package com.auction.client;

import java.net.http.HttpClient;

public class HttpClientSingleton {
    private static volatile HttpClientSingleton instance;
    private final HttpClient httpClient;

    private HttpClientSingleton() {
        httpClient = HttpClient.newHttpClient();
    }

    public static HttpClientSingleton getInstance() {
        if (instance == null) {
            synchronized (HttpClientSingleton.class) {
                if (instance == null) {
                    instance = new HttpClientSingleton();
                }
            }
        }
        return instance;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }
}