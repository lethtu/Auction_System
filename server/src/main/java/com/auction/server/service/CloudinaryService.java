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
        this.cloudName = cloudName != null ? cloudName.trim() : "";
        if (hasText(this.cloudName) && hasText(apiKey) && hasText(apiSecret)) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", this.cloudName,
                    "api_key", apiKey.trim(),
                    "api_secret", apiSecret.trim(),
                    "secure", true
            ));
        } else {
            this.cloudinary = null;
        }
    }

    public boolean isConfigured() {
        return this.cloudinary != null;
    }

    public String getCloudName() {
        return cloudName;
    }

    /**
     * Upload a file with a specific folder and publicId.
     */
    public String uploadFileWithPublicId(MultipartFile file, String folder, String publicId, boolean isRaw) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary is not configured.");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("folder", folder);
        params.put("public_id", publicId);
        params.put("overwrite", true);
        params.put("invalidate", true); // Purge CDN cache for overwritten files immediately

        if (isRaw) {
            params.put("resource_type", "raw");
        } else {
            params.put("resource_type", "image");
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return uploadResult.get("secure_url").toString();
    }

    /**
     * Dynamically generate Image URL.
     */
    public String getDynamicImageUrl(String uuid) {
        if (!isConfigured() || !hasText(uuid)) {
            return "/api/files/images/" + uuid + "/" + uuid + ".png";
        }
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/auction_system/items/images/" + uuid + ".jpg";
    }

    /**
     * Dynamically generate 3D model URL.
     */
    public String getDynamicModel3DUrl(String uuid) {
        if (!isConfigured() || !hasText(uuid)) {
            return "/api/files/models-3d/" + uuid + "/" + uuid + ".glb";
        }
        return "https://res.cloudinary.com/" + cloudName + "/raw/upload/auction_system/items/models_3d/" + uuid + ".glb";
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}
