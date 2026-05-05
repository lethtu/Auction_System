package com.auction.client.api;

import com.auction.client.Config;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SellerApiClient {
    private static final String SELLER_API = Config.API_URL + "/api/seller";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public HttpResponse<String> createAuction(JSONObject body) throws Exception {
        return sendJson("POST", "/create-auction", body);
    }

    public HttpResponse<String> updateSession(int sessionId, int sellerId, JSONObject body) throws Exception {
        return sendJson("PUT", "/update-session/" + sessionId + "?sellerId=" + sellerId, body);
    }

    public HttpResponse<String> getMySessions(int sellerId) throws Exception {
        return sendGet("/my-sessions/" + sellerId);
    }

    public HttpResponse<String> getMySessions(int sellerId, String status) throws Exception {
        String path = "/my-sessions/" + sellerId;

        if (status != null && !status.trim().isEmpty()) {
            path += "?status=" + status.trim();
        }

        return sendGet(path);
    }

    public HttpResponse<String> cancelSession(int sessionId, int sellerId) throws Exception {
        return sendDelete("/cancel-session/" + sessionId + "?sellerId=" + sellerId);
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(path))
                .GET()
                .build();

        return send(request);
    }

    private HttpResponse<String> sendDelete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(path))
                .DELETE()
                .build();

        return send(request);
    }

    private HttpResponse<String> sendJson(String method, String path, JSONObject body) throws Exception {
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofString(body.toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(path))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .method(method, publisher)
                .build();

        return send(request);
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create(SELLER_API + path);
    }
}