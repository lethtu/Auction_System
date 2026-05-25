package com.auction.client.service;

import com.auction.client.api.SellerApiClient;
import com.auction.client.dto.ApiResult;
import com.auction.client.dto.CreateAuctionRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLSession;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SellerDashboardServiceImageUploadTest {

    @TempDir
    Path tempDir;

    private final FakeSellerApiClient sellerApiClient = new FakeSellerApiClient();
    private final SellerDashboardService service = new SellerDashboardService(sellerApiClient);

    @Test
    void createAuction_withValidImage_uploadsImageAndSendsReturnedImagePath() throws Exception {
        File imageFile = createTempImage();

        sellerApiClient.uploadResponse = response(200, """
                {
                  "status": 200,
                  "message": "Upload thành công",
                  "data": {
                    "imagePath": "upload/images/laptop.png"
                  }
                }
                """);
        sellerApiClient.createResponse = response(200, "{\"status\":200,\"message\":\"Tạo thành công\"}");

        ApiResult<Void> result = service.createAuction(validRequest(), imageFile);

        assertTrue(result.success);
        assertEquals("Tạo thành công", result.message);
        assertEquals(imageFile, sellerApiClient.lastUploadedFile);
        assertNotNull(sellerApiClient.lastCreateBody);
        assertEquals("upload/images/laptop.png", sellerApiClient.lastCreateBody.getString("imagePath"));
    }

    @Test
    void createAuction_uploadSuccessButMissingImagePath_throwsException() throws Exception {
        File imageFile = createTempImage();

        sellerApiClient.uploadResponse = response(200, """
                {
                  "status": 200,
                  "message": "Upload thành công",
                  "data": {}
                }
                """);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createAuction(validRequest(), imageFile)
        );

        assertEquals("Server received the image but did not return an image path.", ex.getMessage());
        assertEquals(imageFile, sellerApiClient.lastUploadedFile);
        assertNull(sellerApiClient.lastCreateBody);
    }

    @Test
    void createAuction_missingImageFile_throwsBeforeCallingApi() {
        File missingFile = tempDir.resolve("missing.png").toFile();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createAuction(validRequest(), missingFile)
        );

        assertEquals("Image file does not exist or is invalid.", ex.getMessage());
        assertNull(sellerApiClient.lastUploadedFile);
        assertNull(sellerApiClient.lastCreateBody);
    }

    @Test
    void createAuction_uploadTooLarge_throwsFriendlyMessage() throws Exception {
        File imageFile = createTempImage();

        sellerApiClient.uploadResponse = response(413, """
                {
                  "status": 413,
                  "message": "Payload Too Large"
                }
                """);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createAuction(validRequest(), imageFile)
        );

        assertEquals(
                "Image is too large. Please select a smaller image or increase the server upload limit.",
                ex.getMessage()
        );
        assertEquals(imageFile, sellerApiClient.lastUploadedFile);
        assertNull(sellerApiClient.lastCreateBody);
    }

    @Test
    void createAuction_uploadServerError_usesServerMessage() throws Exception {
        File imageFile = createTempImage();

        sellerApiClient.uploadResponse = response(500, """
                {
                  "status": 500,
                  "message": "Upload ảnh lỗi"
                }
                """);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createAuction(validRequest(), imageFile)
        );

        assertEquals("Upload ảnh lỗi", ex.getMessage());
        assertEquals(imageFile, sellerApiClient.lastUploadedFile);
        assertNull(sellerApiClient.lastCreateBody);
    }

    private File createTempImage() throws Exception {
        Path imagePath = tempDir.resolve("item.png");
        Files.writeString(imagePath, "fake image content");
        return imagePath.toFile();
    }

    private CreateAuctionRequest validRequest() {
        return new CreateAuctionRequest(
                "Laptop",
                "Electronics",
                "Gaming laptop",
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                "2026-05-12T10:00:00",
                "2026-05-20T10:00:00",
                2
        );
    }

    private HttpResponse<String> response(int status, String body) {
        return new FakeHttpResponse(status, body);
    }

    private static class FakeSellerApiClient extends SellerApiClient {
        private HttpResponse<String> uploadResponse;
        private HttpResponse<String> createResponse;

        private File lastUploadedFile;
        private JSONObject lastCreateBody;

        @Override
        public HttpResponse<String> uploadImage(File imageFile) {
            this.lastUploadedFile = imageFile;
            return uploadResponse;
        }

        @Override
        public HttpResponse<String> createAuction(JSONObject body) {
            this.lastCreateBody = body;
            return createResponse;
        }
    }

    private record FakeHttpResponse(int statusCode, String body) implements HttpResponse<String> {
        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("http://localhost");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}