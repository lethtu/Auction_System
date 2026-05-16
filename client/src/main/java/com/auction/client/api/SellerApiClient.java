package com.auction.client.api;

import com.auction.client.Config;
import com.auction.client.util.HttpRequestUtil;
import org.json.JSONObject;

import java.io.File;
import java.net.http.HttpResponse;

public class SellerApiClient {
    private static final String SELLER_API = Config.API_URL + "/api/seller";
    private static final String FILE_API = Config.API_URL + "/api/files";

    private static final String CREATE_AUCTION_PATH = "/create-auction";
    private static final String UPDATE_SESSION_PATH = "/update-session/";
    private static final String MY_SESSIONS_PATH = "/my-sessions/";
    private static final String CANCEL_SESSION_PATH = "/cancel-session/";
    private static final String UPLOAD_IMAGE_PATH = "/images";

    private static final String POST_METHOD = "POST";
    private static final String PUT_METHOD = "PUT";

    public HttpResponse<String> createAuction(JSONObject body) throws Exception {
        return sendJson(POST_METHOD, CREATE_AUCTION_PATH, body);
    }

    public HttpResponse<String> updateSession(int sessionId, int sellerId, JSONObject body) throws Exception {
        String path = withSellerId(UPDATE_SESSION_PATH + sessionId, sellerId);
        return sendJson(PUT_METHOD, path, body);
    }

    public HttpResponse<String> uploadImage(File imageFile) throws Exception {
        return HttpRequestUtil.uploadImage(FILE_API, UPLOAD_IMAGE_PATH, imageFile);
    }

    public HttpResponse<String> getMySessions(int sellerId) throws Exception {
        return getMySessions(sellerId, null);
    }

    public HttpResponse<String> getMySessions(int sellerId, String status) throws Exception {
        String path = MY_SESSIONS_PATH + sellerId;
        return get(withOptionalQueryParam(path, "status", status));
    }

    public HttpResponse<String> cancelSession(int sessionId, int sellerId) throws Exception {
        String path = withSellerId(CANCEL_SESSION_PATH + sessionId, sellerId);
        return HttpRequestUtil.delete(SELLER_API, path);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpRequestUtil.get(SELLER_API, path);
    }

    private HttpResponse<String> sendJson(String method, String path, JSONObject body) throws Exception {
        return HttpRequestUtil.sendJson(method, SELLER_API, path, body);
    }

    private String withSellerId(String path, int sellerId) {
        return withRequiredQueryParam(path, "sellerId", String.valueOf(sellerId));
    }

    private String withOptionalQueryParam(String path, String name, String value) {
        String normalizedValue = normalize(value);

        if (normalizedValue.isEmpty()) {
            return path;
        }

        return withRequiredQueryParam(path, name, normalizedValue);
    }

    private String withRequiredQueryParam(String path, String name, String value) {
        String separator = path.contains("?") ? "&" : "?";
        return path + separator + name + "=" + HttpRequestUtil.encode(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}