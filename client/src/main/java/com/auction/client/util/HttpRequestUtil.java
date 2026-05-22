package com.auction.client.util;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

public final class HttpRequestUtil {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data; boundary=";
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private static final String CONTENT_TYPE_IMAGE_PNG = "image/png";
    private static final String CONTENT_TYPE_IMAGE_JPEG = "image/jpeg";
    private static final String CONTENT_TYPE_IMAGE_GIF = "image/gif";
    private static final String CONTENT_TYPE_IMAGE_WEBP = "image/webp";
    private static final String CONTENT_TYPE_IMAGE_BMP = "image/bmp";

    private static final String DEFAULT_IMAGE_FILE_NAME = "image.png";
    private static final String MULTIPART_FILE_FIELD_NAME = "file";
    private static final String MULTIPART_BOUNDARY_PREFIX = "----AuctionBoundary";
    private static final String LINE_BREAK = "\r\n";

    private HttpRequestUtil() {
    }

    public static HttpResponse<String> get(String baseUrl, String path) throws Exception {
        HttpRequest request = requestBuilder(baseUrl, path)
                .GET()
                .build();

        return send(request);
    }

    public static HttpResponse<String> delete(String baseUrl, String path) throws Exception {
        HttpRequest request = requestBuilder(baseUrl, path)
                .DELETE()
                .build();

        return send(request);
    }

    public static HttpResponse<String> postNoBody(String baseUrl, String path) throws Exception {
        HttpRequest request = requestBuilder(baseUrl, path)
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
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
        Objects.requireNonNull(method, "HTTP method must not be null.");
        Objects.requireNonNull(body, "JSON body must not be null.");

        HttpRequest request = requestBuilder(baseUrl, path)
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                .method(method, HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return send(request);
    }

    public static HttpResponse<String> uploadImage(String baseUrl, String path, File imageFile) throws Exception {
        Objects.requireNonNull(imageFile, "Image file must not be null.");

        String boundary = createBoundary();
        byte[] body = buildImageMultipartBody(boundary, imageFile);

        HttpRequest request = requestBuilder(baseUrl, path)
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_FORM_DATA + boundary)
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

    private static HttpRequest.Builder requestBuilder(String baseUrl, String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri(baseUrl, path));
        String token = com.auction.client.model.User.getSessionToken();
        if (token != null && !token.isEmpty()) {
            builder.header("X-Auth-Token", token);
        }
        return builder;
    }

    private static URI uri(String baseUrl, String path) {
        Objects.requireNonNull(baseUrl, "Base URL must not be null.");
        Objects.requireNonNull(path, "Path must not be null.");

        return URI.create(baseUrl + path);
    }

    private static String createBoundary() {
        return MULTIPART_BOUNDARY_PREFIX + UUID.randomUUID();
    }

    private static byte[] buildImageMultipartBody(String boundary, File imageFile) throws Exception {
        String fileName = safeFileName(imageFile.getName());
        String contentType = detectContentType(imageFile);
        byte[] fileBytes = Files.readAllBytes(imageFile.toPath());

        return buildMultipartBody(boundary, MULTIPART_FILE_FIELD_NAME, fileName, contentType, fileBytes);
    }

    private static String detectContentType(File imageFile) throws Exception {
        String contentType = Files.probeContentType(imageFile.toPath());

        if (hasText(contentType)) {
            return contentType;
        }

        return detectContentTypeFromFileName(imageFile.getName());
    }

    private static String detectContentTypeFromFileName(String fileName) {
        String lowerCaseFileName = fileName == null ? "" : fileName.toLowerCase();

        if (lowerCaseFileName.endsWith(".png")) {
            return CONTENT_TYPE_IMAGE_PNG;
        }

        if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return CONTENT_TYPE_IMAGE_JPEG;
        }

        if (lowerCaseFileName.endsWith(".gif")) {
            return CONTENT_TYPE_IMAGE_GIF;
        }

        if (lowerCaseFileName.endsWith(".webp")) {
            return CONTENT_TYPE_IMAGE_WEBP;
        }

        if (lowerCaseFileName.endsWith(".bmp")) {
            return CONTENT_TYPE_IMAGE_BMP;
        }

        return CONTENT_TYPE_OCTET_STREAM;
    }

    private static String safeFileName(String fileName) {
        if (!hasText(fileName)) {
            return DEFAULT_IMAGE_FILE_NAME;
        }

        return fileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\"", "_");
    }

    private static byte[] buildMultipartBody(
            String boundary,
            String fieldName,
            String fileName,
            String contentType,
            byte[] fileBytes
    ) {
        byte[] headerBytes = buildMultipartHeader(boundary, fieldName, fileName, contentType)
                .getBytes(StandardCharsets.UTF_8);
        byte[] endingBytes = buildMultipartEnding(boundary)
                .getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream output = new ByteArrayOutputStream(
                headerBytes.length + fileBytes.length + endingBytes.length
        );

        output.writeBytes(headerBytes);
        output.writeBytes(fileBytes);
        output.writeBytes(endingBytes);

        return output.toByteArray();
    }

    private static String buildMultipartHeader(
            String boundary,
            String fieldName,
            String fileName,
            String contentType
    ) {
        return "--" + boundary + LINE_BREAK
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"" + LINE_BREAK
                + HEADER_CONTENT_TYPE + ": " + contentType + LINE_BREAK
                + LINE_BREAK;
    }

    private static String buildMultipartEnding(String boundary) {
        return LINE_BREAK + "--" + boundary + "--" + LINE_BREAK;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}