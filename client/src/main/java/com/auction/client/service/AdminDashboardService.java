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
import java.util.Objects;
import java.util.function.Function;

public class AdminDashboardService {
    private static final String APPROVE_SUCCESS_MESSAGE = "Phê duyệt phiên thành công.";
    private static final String REJECT_SUCCESS_MESSAGE = "Đã từ chối phiên đấu giá.";
    private static final String BAN_USER_SUCCESS_MESSAGE = "Đã khóa tài khoản user.";
    private static final String CANCEL_AUCTION_SUCCESS_MESSAGE = "Đã hủy phiên đấu giá.";

    private final AdminApiClient adminApiClient;

    public AdminDashboardService() {
        this(new AdminApiClient());
    }

    AdminDashboardService(AdminApiClient adminApiClient) {
        this.adminApiClient = Objects.requireNonNull(adminApiClient, "adminApiClient must not be null");
    }

    public List<PendingSessionRow> getPendingSessions() throws Exception {
        return fetchArrayData(
                adminApiClient::getPendingSessions,
                AdminResponseParser::parsePendingSessions
        );
    }

    public List<AdminSessionRow> getAllSessions() throws Exception {
        return fetchArrayData(
                () -> adminApiClient.getAllSessions(null),
                AdminResponseParser::parseAllSessions
        );
    }

    public List<AdminUserRow> getAllUsers() throws Exception {
        return fetchArrayData(
                () -> adminApiClient.getAllUsers(null),
                AdminResponseParser::parseUsers
        );
    }

    public ApiResult<Void> approveSession(int sessionId, int adminId) throws Exception {
        return executeAction(
                () -> adminApiClient.approveSession(sessionId, adminId),
                APPROVE_SUCCESS_MESSAGE
        );
    }

    public ApiResult<Void> rejectSession(int sessionId, int adminId, String reason) throws Exception {
        return executeAction(
                () -> adminApiClient.rejectSession(sessionId, adminId, reason),
                REJECT_SUCCESS_MESSAGE
        );
    }

    public ApiResult<Void> banUser(int userId, int adminId) throws Exception {
        return executeAction(
                () -> adminApiClient.banUser(userId, adminId),
                BAN_USER_SUCCESS_MESSAGE
        );
    }

    public ApiResult<Void> cancelAuction(int sessionId, int adminId) throws Exception {
        return executeAction(
                () -> adminApiClient.cancelAuction(sessionId, adminId),
                CANCEL_AUCTION_SUCCESS_MESSAGE
        );
    }

    private <T> List<T> fetchArrayData(
            AdminRequest request,
            Function<JSONArray, List<T>> parser
    ) throws Exception {
        HttpResponse<String> response = request.execute();
        ApiResult<JSONArray> api = extractArray(response);
        return parser.apply(api.data);
    }

    private ApiResult<Void> executeAction(AdminRequest request, String successMessage) throws Exception {
        HttpResponse<String> response = request.execute();
        return parseApiResult(response, successMessage);
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

    @FunctionalInterface
    private interface AdminRequest {
        HttpResponse<String> execute() throws Exception;
    }
}