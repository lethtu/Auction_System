package com.auction.client.api;

import com.auction.client.Config;
import com.auction.client.util.HttpRequestUtil;

import java.net.http.HttpResponse;

public class AdminApiClient {
    private static final String ADMIN_API = Config.API_URL + "/api/admin";

    public HttpResponse<String> getPendingSessions() throws Exception {
        return HttpRequestUtil.get(ADMIN_API, "/pending");
    }

    public HttpResponse<String> getAllSessions(String status) throws Exception {
        String path = "/sessions";

        if (status != null && !status.trim().isEmpty()) {
            path += "?status=" + HttpRequestUtil.encode(status.trim());
        }

        return HttpRequestUtil.get(ADMIN_API, path);
    }

    public HttpResponse<String> approveSession(int sessionId, int adminId) throws Exception {
        return HttpRequestUtil.postNoBody(ADMIN_API, "/approve/" + sessionId + "?adminId=" + adminId);
    }

    public HttpResponse<String> rejectSession(int sessionId, int adminId, String reason) throws Exception {
        String safeReason = reason == null ? "" : reason.trim();
        return HttpRequestUtil.postNoBody(
                ADMIN_API,
                "/reject/" + sessionId + "?adminId=" + adminId + "&reason=" + HttpRequestUtil.encode(safeReason)
        );
    }

    public HttpResponse<String> getAllUsers(String role) throws Exception {
        String path = "/users";

        if (role != null && !role.trim().isEmpty()) {
            path += "?role=" + HttpRequestUtil.encode(role.trim());
        }

        return HttpRequestUtil.get(ADMIN_API, path);
    }

    public HttpResponse<String> banUser(int userId, int adminId) throws Exception {
        return HttpRequestUtil.postNoBody(ADMIN_API, "/ban-user/" + userId + "?adminId=" + adminId);
    }

    public HttpResponse<String> cancelAuction(int sessionId, int adminId) throws Exception {
        return HttpRequestUtil.postNoBody(ADMIN_API, "/cancel-auction/" + sessionId + "?adminId=" + adminId);
    }
}
