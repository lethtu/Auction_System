package com.auction.client.service;

import com.auction.client.api.SellerApiClient;
import com.auction.client.dto.ApiResult;
import com.auction.client.dto.CreateAuctionRequest;
import com.auction.client.model.SessionItem;
import com.auction.client.parser.SellerResponseParser;
import com.auction.client.util.SellerRequestBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.http.HttpResponse;
import java.util.List;

public class SellerDashboardService {
    private final SellerApiClient sellerApiClient;

    public SellerDashboardService() {
        this(new SellerApiClient());
    }

    SellerDashboardService(SellerApiClient sellerApiClient) {
        this.sellerApiClient = sellerApiClient;
    }

    public ApiResult<Void> createAuction(CreateAuctionRequest request) throws Exception {
        return createAuction(request, null);
    }

    public ApiResult<Void> createAuction(CreateAuctionRequest request, File imageFile) throws Exception {
        CreateAuctionRequest finalRequest = withUploadedImage(request, imageFile);
        HttpResponse<String> response = sellerApiClient.createAuction(
                SellerRequestBuilder.buildAuctionBody(finalRequest)
        );

        return parseApiResult(response, "Tạo phiên đấu giá thành công.");
    }

    public ApiResult<Void> updateSession(int sessionId, int sellerId, CreateAuctionRequest request) throws Exception {
        return updateSession(sessionId, sellerId, request, null);
    }

    public ApiResult<Void> updateSession(int sessionId, int sellerId, CreateAuctionRequest request, File imageFile) throws Exception {
        CreateAuctionRequest finalRequest = withUploadedImage(request, imageFile);
        HttpResponse<String> response = sellerApiClient.updateSession(
                sessionId,
                sellerId,
                SellerRequestBuilder.buildAuctionBody(finalRequest)
        );

        return parseApiResult(response, "Đã cập nhật phiên thành công.");
    }

    public List<SessionItem> getMySessions(int sellerId) throws Exception {
        HttpResponse<String> response = sellerApiClient.getMySessions(sellerId);
        return parseSessionList(response);
    }

    public List<SessionItem> getMySessions(int sellerId, String status) throws Exception {
        HttpResponse<String> response = sellerApiClient.getMySessions(sellerId, status);
        return parseSessionList(response);
    }

    public ApiResult<Void> cancelSession(int sessionId, int sellerId) throws Exception {
        HttpResponse<String> response = sellerApiClient.cancelSession(sessionId, sellerId);
        return parseApiResult(response, "Đã hủy phiên thành công.");
    }

    private CreateAuctionRequest withUploadedImage(CreateAuctionRequest request, File imageFile) throws Exception {
        if (imageFile == null) {
            return request;
        }

        String uploadedImagePath = uploadImage(imageFile);
        return new CreateAuctionRequest(
                request.productName,
                request.productType,
                request.description,
                uploadedImagePath,
                request.startingPrice,
                request.stepPrice,
                request.startTime,
                request.endTime,
                request.sellerId
        );
    }

    private String uploadImage(File imageFile) throws Exception {
        HttpResponse<String> response = sellerApiClient.uploadImage(imageFile);
        JSONObject json = new JSONObject(response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(json.optString("message", "Upload ảnh thất bại."));
        }

        JSONObject data = json.optJSONObject("data");
        if (data == null) {
            throw new IllegalStateException(json.optString("message", "Không nhận được đường dẫn ảnh."));
        }

        String imagePath = data.optString("imagePath", "");
        if (imagePath.isBlank()) {
            throw new IllegalStateException("Không nhận được đường dẫn ảnh.");
        }

        return imagePath;
    }

    private ApiResult<Void> parseApiResult(HttpResponse<String> response, String successMessage) {
        return SellerResponseParser.parseApiResponse(
                response.body(),
                response.statusCode(),
                successMessage
        );
    }

    private List<SessionItem> parseSessionList(HttpResponse<String> response) {
        ApiResult<JSONArray> api = SellerResponseParser.extractDataArray(
                response.body(),
                response.statusCode()
        );

        if (!api.success) {
            throw new IllegalStateException(api.message);
        }

        return SellerResponseParser.parseSessions(api.data);
    }
}