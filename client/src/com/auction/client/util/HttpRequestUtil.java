package com.auction.client.util;

import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class HttpRequestUtil {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private HttpRequestUtil() {
    }

    public static HttpResponse<String> get(String baseUrl, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(baseUrl, path))
                .GET()
                .build();

        return send(request);
    }

    public static HttpResponse<String> delete(String baseUrl, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(baseUrl, path))
                .DELETE()
                .build();

        return send(request);
    }

    public static HttpResponse<String> postNoBody(String baseUrl, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(baseUrl, path))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return send(request);
    }

    public static HttpResponse<String> sendJson(
            String method,
            String baseUrl,
            String path,
            JSONObject body
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(baseUrl, path))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .method(method, HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return send(request);
    }

    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static HttpResponse<String> send(HttpRequest request) throws Exception {
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static URI uri(String baseUrl, String path) {
        return URI.create(baseUrl + path);
    }
}