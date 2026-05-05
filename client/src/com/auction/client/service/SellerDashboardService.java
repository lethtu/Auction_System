package com.auction.client.service;

import com.auction.client.api.SellerApiClient;
import com.auction.client.dto.ApiArrayResult;
import com.auction.client.dto.ApiResult;
import com.auction.client.dto.CreateAuctionRequest;
import com.auction.client.model.SessionItem;
import com.auction.client.parser.SellerResponseParser;
import org.json.JSONObject;

import java.net.http.HttpResponse;
import java.util.List;

public class SellerDashboardService {
    private final SellerApiClient sellerApiClient = new SellerApiClient();

    public ApiResult createAuction(CreateAuctionRequest request) throws Exception {
        JSONObject body = buildCreateAuctionBody(request);
        HttpResponse<String> response = sellerApiClient.createAuction(body);

        return SellerResponseParser.parseApiResponse(
                response.body(),
                response.statusCode(),
                "Tạo phiên đấu giá thành công."
        );
    }

    public ApiResult updateSession(int sessionId, int sellerId, CreateAuctionRequest request) throws Exception {
        JSONObject body = buildCreateAuctionBody(request);
        HttpResponse<String> response = sellerApiClient.updateSession(sessionId, sellerId, body);

        return SellerResponseParser.parseApiResponse(
                response.body(),
                response.statusCode(),
                "Đã cập nhật phiên thành công."
        );
    }

    public List<SessionItem> getMySessions(int sellerId) throws Exception {
        HttpResponse<String> response = sellerApiClient.getMySessions(sellerId);
        return parseSessionList(response);
    }

    public List<SessionItem> getMySessions(int sellerId, String status) throws Exception {
        HttpResponse<String> response = sellerApiClient.getMySessions(sellerId, status);
        return parseSessionList(response);
    }

    public ApiResult cancelSession(int sessionId, int sellerId) throws Exception {
        HttpResponse<String> response = sellerApiClient.cancelSession(sessionId, sellerId);

        return SellerResponseParser.parseApiResponse(
                response.body(),
                response.statusCode(),
                "Đã hủy phiên thành công."
        );
    }

    private List<SessionItem> parseSessionList(HttpResponse<String> response) {
        ApiArrayResult api = SellerResponseParser.extractDataArray(
                response.body(),
                response.statusCode()
        );

        if (!api.success) {
            throw new IllegalStateException(api.message);
        }

        return SellerResponseParser.parseSessions(api.data);
    }

    private JSONObject buildCreateAuctionBody(CreateAuctionRequest request) {
        JSONObject body = new JSONObject();

        body.put("name", request.productName);
        body.put("type", request.productType);
        body.put("imagePath", request.imageUrl);
        body.put("description", request.description);
        body.put("startingPrice", request.startingPrice);
        body.put("stepPrice", request.stepPrice);
        body.put("startTime", request.startTime);
        body.put("endTime", request.endTime);
        body.put("sellerId", request.sellerId);

        return body;
    }
}