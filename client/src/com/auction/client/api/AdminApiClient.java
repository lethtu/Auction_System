package com.auction.client.api;

import com.auction.client.Config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminApiClient {
    private final HttpClient client = HttpClient.newHttpClient();

    public HttpResponse<String> getPendingSessions() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/admin/pending"))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> approveSession(int sessionId, int adminId) throws Exception {
        String url = Config.API_URL + "/api/admin/approve/" + sessionId + "?adminId=" + adminId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}