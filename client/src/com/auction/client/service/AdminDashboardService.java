package com.auction.client.service;

import com.auction.client.api.AdminApiClient;
import com.auction.client.dto.ApiArrayResult;
import com.auction.client.dto.ApiResult;
import com.auction.client.model.AdminSessionRow;
import com.auction.client.model.AdminUserRow;
import com.auction.client.model.PendingSessionRow;
import com.auction.client.parser.AdminResponseParser;

import java.net.http.HttpResponse;
import java.util.List;

public class AdminDashboardService {
    private final AdminApiClient adminApiClient = new AdminApiClient();

    public List<PendingSessionRow> getPendingSessions() throws Exception {
        HttpResponse<String> response = adminApiClient.getPendingSessions();
        ApiArrayResult api = extractArray(response);
        return AdminResponseParser.parsePendingSessions(api.data);
    }

    public List<AdminSessionRow> getAllSessions() throws Exception {
        HttpResponse<String> response = adminApiClient.getAllSessions(null);
        ApiArrayResult api = extractArray(response);
        return AdminResponseParser.parseAllSessions(api.data);
    }

    public List<AdminUserRow> getAllUsers() throws Exception {
        HttpResponse<String> response = adminApiClient.getAllUsers(null);
        ApiArrayResult api = extractArray(response);
        return AdminResponseParser.parseUsers(api.data);
    }

    public ApiResult approveSession(int sessionId, int adminId) throws Exception {
        HttpResponse<String> response = adminApiClient.approveSession(sessionId, adminId);
        return parseApiResult(response, "Phê duyệt phiên thành công.");
    }

    public ApiResult rejectSession(int sessionId, int adminId, String reason) throws Exception {
        HttpResponse<String> response = adminApiClient.rejectSession(sessionId, adminId, reason);
        return parseApiResult(response, "Đã từ chối phiên đấu giá.");
    }

    public ApiResult banUser(int userId, int adminId) throws Exception {
        HttpResponse<String> response = adminApiClient.banUser(userId, adminId);
        return parseApiResult(response, "Đã khóa tài khoản user.");
    }

    public ApiResult cancelAuction(int sessionId, int adminId) throws Exception {
        HttpResponse<String> response = adminApiClient.cancelAuction(sessionId, adminId);
        return parseApiResult(response, "Đã hủy phiên đấu giá.");
    }

    private ApiArrayResult extractArray(HttpResponse<String> response) {
        ApiArrayResult api = AdminResponseParser.extractDataArray(response.body(), response.statusCode());

        if (!api.success) {
            throw new IllegalStateException(api.message);
        }

        return api;
    }

    private ApiResult parseApiResult(HttpResponse<String> response, String successMessage) {
        return AdminResponseParser.parseApiResponse(response.body(), response.statusCode(), successMessage);
    }
}
