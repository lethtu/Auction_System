package com.auction.server.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final String cloudName;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret
    ) {
        this.cloudName = trimToEmpty(cloudName);
        String safeApiKey = trimToEmpty(apiKey);
        String safeApiSecret = trimToEmpty(apiSecret);

        if (hasText(this.cloudName) && hasText(safeApiKey) && hasText(safeApiSecret)) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", this.cloudName,
                    "api_key", safeApiKey,
                    "api_secret", safeApiSecret,
                    "secure", true
            ));
        } else {
            this.cloudinary = null;
        }
    }

    public boolean isConfigured() {
        return cloudinary != null;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String uploadFileWithPublicId(
            MultipartFile file,
            String folder,
            String publicId,
            boolean rawResource
    ) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary is not configured.");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("folder", folder);
        params.put("public_id", publicId);
        params.put("overwrite", true);
        params.put("invalidate", true);
        params.put("resource_type", rawResource ? "raw" : "image");

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        Object secureUrl = uploadResult.get("secure_url");
        if (secureUrl == null || secureUrl.toString().isBlank()) {
            throw new IOException("Cloudinary upload response does not contain secure_url.");
        }

        return secureUrl.toString();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
