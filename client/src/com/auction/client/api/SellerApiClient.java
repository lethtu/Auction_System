package com.auction.client.api;

import com.auction.client.Config;
import com.auction.client.util.HttpRequestUtil;
import org.json.JSONObject;

import java.net.http.HttpResponse;

public class SellerApiClient {
    private static final String SELLER_API = Config.API_URL + "/api/seller";

    public HttpResponse<String> createAuction(JSONObject body) throws Exception {
        return HttpRequestUtil.sendJson("POST", SELLER_API, "/create-auction", body);
    }

    public HttpResponse<String> updateSession(int sessionId, int sellerId, JSONObject body) throws Exception {
        return HttpRequestUtil.sendJson(
                "PUT",
                SELLER_API,
                "/update-session/" + sessionId + "?sellerId=" + sellerId,
                body
        );
    }

    public HttpResponse<String> getMySessions(int sellerId) throws Exception {
        return HttpRequestUtil.get(SELLER_API, "/my-sessions/" + sellerId);
    }

    public HttpResponse<String> getMySessions(int sellerId, String status) throws Exception {
        String path = "/my-sessions/" + sellerId;

        if (status != null && !status.trim().isEmpty()) {
            path += "?status=" + HttpRequestUtil.encode(status.trim());
        }

        return HttpRequestUtil.get(SELLER_API, path);
    }

    public HttpResponse<String> cancelSession(int sessionId, int sellerId) throws Exception {
        return HttpRequestUtil.delete(
                SELLER_API,
                "/cancel-session/" + sessionId + "?sellerId=" + sellerId
        );
    }
}