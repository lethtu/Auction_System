package com.auction.client.service;

import com.auction.client.api.AdminApiClient;
import com.auction.client.dto.ApiArrayResult;
import com.auction.client.dto.ApiResult;
import com.auction.client.model.PendingSessionRow;
import com.auction.client.parser.AdminResponseParser;

import java.net.http.HttpResponse;
import java.util.List;

public class AdminDashboardService {
    private final AdminApiClient adminApiClient = new AdminApiClient();

    public List<PendingSessionRow> getPendingSessions() throws Exception {
        HttpResponse<String> response = adminApiClient.getPendingSessions();

        ApiArrayResult api = AdminResponseParser.extractDataArray(
                response.body(),
                response.statusCode()
        );

        if (!api.success) {
            throw new IllegalStateException(api.message);
        }

        return AdminResponseParser.parsePendingSessions(api.data);
    }

    public ApiResult approveSession(int sessionId, int adminId) throws Exception {
        HttpResponse<String> response = adminApiClient.approveSession(sessionId, adminId);

        return parseApiResult(response, "Phê duyệt phiên thành công.");
    }

    public ApiResult rejectSession(int sessionId, int adminId, String reason) throws Exception {
        HttpResponse<String> response = adminApiClient.rejectSession(sessionId, adminId, reason);

        return parseApiResult(response, "Đã từ chối phiên đấu giá.");
    }

    private ApiResult parseApiResult(HttpResponse<String> response, String successMessage) {
        return AdminResponseParser.parseApiResponse(
                response.body(),
                response.statusCode(),
                successMessage
        );
    }
}