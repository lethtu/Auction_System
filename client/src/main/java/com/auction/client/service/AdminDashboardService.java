package com.auction.client.service;

import com.auction.client.api.AdminApiClient;
import com.auction.client.dto.ApiResult;
import com.auction.client.model.AdminSessionRow;
import com.auction.client.model.AdminUserRow;
import com.auction.client.model.PendingSessionRow;
import com.auction.client.parser.AdminResponseParser;
import org.json.JSONArray;

import java.net.http.HttpResponse;
import java.util.List;

public class AdminDashboardService {
    private final AdminApiClient adminApiClient;

    public AdminDashboardService() {
        this(new AdminApiClient());
    }

    AdminDashboardService(AdminApiClient adminApiClient) {
        this.adminApiClient = adminApiClient;
    }

    public List<PendingSessionRow> getPendingSessions() throws Exception {
        HttpResponse<String> response = adminApiClient.getPendingSessions();
        ApiResult<JSONArray> api = extractArray(response);
        return AdminResponseParser.parsePendingSessions(api.data);
    }

    public List<AdminSessionRow> getAllSessions() throws Exception {
        HttpResponse<String> response = adminApiClient.getAllSessions(null);
        ApiResult<JSONArray> api = extractArray(response);
        return AdminResponseParser.parseAllSessions(api.data);
    }

    public List<AdminUserRow> getAllUsers() throws Exception {
        HttpResponse<String> response = adminApiClient.getAllUsers(null);
        ApiResult<JSONArray> api = extractArray(response);
        return AdminResponseParser.parseUsers(api.data);
    }

    public ApiResult<Void> approveSession(int sessionId, int adminId) throws Exception {
        HttpResponse<String> response = adminApiClient.approveSession(sessionId, adminId);
        return parseApiResult(response, "Phê duyệt phiên thành công.");
    }

    public ApiResult<Void> rejectSession(int sessionId, int adminId, String reason) throws Exception {
        HttpResponse<String> response = adminApiClient.rejectSession(sessionId, adminId, reason);
        return parseApiResult(response, "Đã từ chối phiên đấu giá.");
    }

    public ApiResult<Void> banUser(int userId, int adminId) throws Exception {
        HttpResponse<String> response = adminApiClient.banUser(userId, adminId);
        return parseApiResult(response, "Đã khóa tài khoản user.");
    }

    public ApiResult<Void> cancelAuction(int sessionId, int adminId) throws Exception {
        HttpResponse<String> response = adminApiClient.cancelAuction(sessionId, adminId);
        return parseApiResult(response, "Đã hủy phiên đấu giá.");
    }

    private ApiResult<JSONArray> extractArray(HttpResponse<String> response) {
        ApiResult<JSONArray> api = AdminResponseParser.extractDataArray(response.body(), response.statusCode());

        if (!api.success) {
            throw new IllegalStateException(api.message);
        }

        return api;
    }

    private ApiResult<Void> parseApiResult(HttpResponse<String> response, String successMessage) {
        return AdminResponseParser.parseApiResponse(response.body(), response.statusCode(), successMessage);
    }
}