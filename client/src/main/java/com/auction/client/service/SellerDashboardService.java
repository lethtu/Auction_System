package com.auction.client.service;

import com.auction.client.api.SellerApiClient;
import com.auction.client.dto.ApiResult;
import com.auction.client.dto.CreateAuctionRequest;
import com.auction.client.model.SessionItem;
import com.auction.client.parser.ApiResponseParser;
import com.auction.client.parser.SellerResponseParser;
import com.auction.client.util.SellerRequestBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.http.HttpResponse;
import java.util.List;

public class SellerDashboardService {

    private static final int HTTP_PAYLOAD_TOO_LARGE = 413;

    private static final String IMAGE_PATH_KEY = "imagePath";

    private static final String CREATE_SUCCESS_MESSAGE = "Auction session created successfully.";
    private static final String UPDATE_SUCCESS_MESSAGE = "Session updated successfully.";
    private static final String CANCEL_SUCCESS_MESSAGE = "Session canceled successfully.";

    private static final String NO_IMAGE_SELECTED_MESSAGE = "No image file selected.";
    private static final String INVALID_IMAGE_FILE_MESSAGE = "Image file does not exist or is invalid.";
    private static final String IMAGE_FILE_NOT_READABLE_MESSAGE = "No permission to read the selected image file.";
    private static final String IMAGE_TOO_LARGE_MESSAGE = "Image is too large. Please select a smaller image or increase the server upload limit.";
    private static final String MISSING_IMAGE_PATH_MESSAGE = "Server received the image but did not return an image path.";
    private static final String UPLOAD_FAILED_MESSAGE = "Image upload failed. HTTP status: ";
    private static final String LOAD_SESSIONS_FAILED_MESSAGE = "Unable to load auction sessions list.";

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

        return parseApiResult(response, CREATE_SUCCESS_MESSAGE);
    }

    public ApiResult<Void> updateSession(int sessionId, int sellerId, CreateAuctionRequest request) throws Exception {
        return updateSession(sessionId, sellerId, request, null);
    }

    public ApiResult<Void> updateSession(
            int sessionId,
            int sellerId,
            CreateAuctionRequest request,
            File imageFile
    ) throws Exception {
        CreateAuctionRequest finalRequest = withUploadedImage(request, imageFile);

        HttpResponse<String> response = sellerApiClient.updateSession(
                sessionId,
                sellerId,
                SellerRequestBuilder.buildAuctionBody(finalRequest)
        );

        return parseApiResult(response, UPDATE_SUCCESS_MESSAGE);
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
        return parseApiResult(response, CANCEL_SUCCESS_MESSAGE);
    }

    private CreateAuctionRequest withUploadedImage(CreateAuctionRequest request, File imageFile) throws Exception {
        if (imageFile == null) {
            return request;
        }

        String uploadedImagePath = uploadImage(imageFile);
        return copyRequestWithImagePath(request, uploadedImagePath);
    }

    private CreateAuctionRequest copyRequestWithImagePath(CreateAuctionRequest request, String imagePath) {
        return new CreateAuctionRequest(
                request.productName,
                request.productType,
                request.description,
                imagePath,
                request.startingPrice,
                request.stepPrice,
                request.reservePrice,
                request.startTime,
                request.endTime,
                request.sellerId,
                request.applyMinRate,
                request.minRate
        );
    }

    private String uploadImage(File imageFile) throws Exception {
        validateImageFile(imageFile);

        HttpResponse<String> response = sellerApiClient.uploadImage(imageFile);
        ApiResult<JSONObject> api = ApiResponseParser.extractDataObject(response.body(), response.statusCode());

        if (!api.success) {
            throw new IllegalStateException(buildUploadErrorMessage(response, api));
        }

        return extractUploadedImagePath(api.data);
    }

    private String extractUploadedImagePath(JSONObject data) {
        if (data == null) {
            throw new IllegalStateException(MISSING_IMAGE_PATH_MESSAGE);
        }

        String imagePath = data.optString(IMAGE_PATH_KEY, "").trim();
        if (imagePath.isEmpty()) {
            throw new IllegalStateException(MISSING_IMAGE_PATH_MESSAGE);
        }

        return imagePath;
    }

    private void validateImageFile(File imageFile) {
        if (imageFile == null) {
            throw new IllegalArgumentException(NO_IMAGE_SELECTED_MESSAGE);
        }

        if (!imageFile.exists() || !imageFile.isFile()) {
            throw new IllegalArgumentException(INVALID_IMAGE_FILE_MESSAGE);
        }

        if (!imageFile.canRead()) {
            throw new IllegalArgumentException(IMAGE_FILE_NOT_READABLE_MESSAGE);
        }
    }

    private String buildUploadErrorMessage(HttpResponse<String> response, ApiResult<JSONObject> api) {
        if (response.statusCode() == HTTP_PAYLOAD_TOO_LARGE) {
            return IMAGE_TOO_LARGE_MESSAGE;
        }

        if (hasText(api.message)) {
            return api.message;
        }

        return UPLOAD_FAILED_MESSAGE + response.statusCode();
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
            throw new IllegalStateException(getMessageOrDefault(api.message, LOAD_SESSIONS_FAILED_MESSAGE));
        }

        return SellerResponseParser.parseSessions(api.data);
    }

    private String getMessageOrDefault(String message, String defaultMessage) {
        return hasText(message) ? message : defaultMessage;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}