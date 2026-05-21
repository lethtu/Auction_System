package com.auction.client.api;

import com.auction.client.Config;
import com.auction.client.util.HttpRequestUtil;

import java.net.http.HttpResponse;

public class AdminApiClient {
    private static final String ADMIN_API = Config.API_URL + "/api/admin";

    private static final String PENDING_SESSIONS_PATH = "/pending";
    private static final String SESSIONS_PATH = "/sessions";
    private static final String USERS_PATH = "/users";

    private static final String APPROVE_SESSION_PATH = "/approve/";
    private static final String REJECT_SESSION_PATH = "/reject/";
    private static final String BAN_USER_PATH = "/ban-user/";
    private static final String RESTORE_USER_PATH = "/restore-user/";
    private static final String CANCEL_AUCTION_PATH = "/cancel-auction/";
    private static final String HIDE_PRODUCT_PATH = "/hide-product/";
    private static final String SHOW_PRODUCT_PATH = "/show-product/";

    public HttpResponse<String> getPendingSessions() throws Exception {
        return get(PENDING_SESSIONS_PATH);
    }

    public HttpResponse<String> getAllSessions(String status) throws Exception {
        return get(withOptionalQueryParam(SESSIONS_PATH, "status", status));
    }

    public HttpResponse<String> approveSession(int sessionId, int adminId) throws Exception {
        return postNoBody(withAdminId(APPROVE_SESSION_PATH + sessionId, adminId));
    }

    public HttpResponse<String> rejectSession(int sessionId, int adminId, String reason) throws Exception {
        String path = withAdminId(REJECT_SESSION_PATH + sessionId, adminId);
        return postNoBody(withRequiredQueryParam(path, "reason", normalize(reason)));
    }

    public HttpResponse<String> getAllUsers(String role) throws Exception {
        return get(withOptionalQueryParam(USERS_PATH, "role", role));
    }

    public HttpResponse<String> banUser(int userId, int adminId) throws Exception {
        return postNoBody(withAdminId(BAN_USER_PATH + userId, adminId));
    }

    public HttpResponse<String> restoreUser(int userId, int adminId) throws Exception {
        return postNoBody(withAdminId(RESTORE_USER_PATH + userId, adminId));
    }


    public HttpResponse<String> cancelAuction(int sessionId, int adminId) throws Exception {
        return postNoBody(withAdminId(CANCEL_AUCTION_PATH + sessionId, adminId));
    }

    public HttpResponse<String> hideProduct(int productId, int adminId) throws Exception {
        return postNoBody(withAdminId(HIDE_PRODUCT_PATH + productId, adminId));
    }

    public HttpResponse<String> showProduct(int productId, int adminId) throws Exception {
        return postNoBody(withAdminId(SHOW_PRODUCT_PATH + productId, adminId));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpRequestUtil.get(ADMIN_API, path);
    }

    private HttpResponse<String> postNoBody(String path) throws Exception {
        return HttpRequestUtil.postNoBody(ADMIN_API, path);
    }

    private String withAdminId(String path, int adminId) {
        return withRequiredQueryParam(path, "adminId", String.valueOf(adminId));
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