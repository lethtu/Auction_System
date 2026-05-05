package com.auction.client.api;

import com.auction.client.Config;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SellerApiClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public HttpResponse<String> createAuction(JSONObject body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/seller/create-auction"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> getMySessions(int sellerId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/seller/my-sessions/" + sellerId))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> cancelSession(int sessionId, int sellerId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/seller/cancel-session/" + sessionId + "?sellerId=" + sellerId))
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}