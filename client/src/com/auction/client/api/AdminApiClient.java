package com.auction.client.api;

import com.auction.client.Config;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class AdminApiClient {
    private static final String ADMIN_API = Config.API_URL + "/api/admin";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final HttpClient client = HttpClient.newHttpClient();

    public HttpResponse<String> getPendingSessions() throws Exception {
        return sendGet("/pending");
    }

    public HttpResponse<String> getAllSessions(String status) throws Exception {
        String path = "/sessions";

        if (status != null && !status.trim().isEmpty()) {
            path += "?status=" + encode(status.trim());
        }

        return sendGet(path);
    }

    public HttpResponse<String> approveSession(int sessionId, int adminId) throws Exception {
        return sendPost("/approve/" + sessionId + "?adminId=" + adminId);
    }

    public HttpResponse<String> rejectSession(int sessionId, int adminId, String reason) throws Exception {
        return sendPost("/reject/" + sessionId
                + "?adminId=" + adminId
                + "&reason=" + encode(reason));
    }

    public HttpResponse<String> getAllUsers(String role) throws Exception {
        String path = "/users";

        if (role != null && !role.trim().isEmpty()) {
            path += "?role=" + encode(role.trim());
        }

        return sendGet(path);
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(path))
                .GET()
                .build();

        return send(request);
    }

    private HttpResponse<String> sendPost(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(path))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return send(request);
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create(ADMIN_API + path);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}