package com.auction.client.util;

import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

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

    public static HttpResponse<String> uploadImage(String baseUrl, String path, File imageFile) throws Exception {
        String boundary = "----AuctionBoundary" + UUID.randomUUID();
        String fileName = imageFile.getName();
        String contentType = Files.probeContentType(imageFile.toPath());

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
        byte[] body = buildMultipartBody(boundary, fileName, contentType, fileBytes);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(baseUrl, path))
                .header(CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
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

    private static byte[] buildMultipartBody(String boundary, String fileName, String contentType, byte[] fileBytes) {
        String lineBreak = "\r\n";
        String partHeader = "--" + boundary + lineBreak
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineBreak
                + "Content-Type: " + contentType + lineBreak
                + lineBreak;
        String ending = lineBreak + "--" + boundary + "--" + lineBreak;

        byte[] headerBytes = partHeader.getBytes(StandardCharsets.UTF_8);
        byte[] endingBytes = ending.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headerBytes.length + fileBytes.length + endingBytes.length];

        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(endingBytes, 0, body, headerBytes.length + fileBytes.length, endingBytes.length);

        return body;
    }
}