package com.auction.client;

import java.net.http.HttpClient;

public class HttpClientSingleton {
    private static volatile HttpClientSingleton instance;
    private HttpClient httpClient;

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

    public static void setInstance(HttpClientSingleton mockInstance) {
        instance = mockInstance;
    }

    public void setHttpClient(HttpClient client) {
        this.httpClient = client;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }
}