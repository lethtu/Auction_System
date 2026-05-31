package com.auction.client;

public class Config {
    public static final String API_URL = "http://localhost:8080";
    public static final String Title = "Auction System";
    public static final String LOGO_PATH = "/com/auction/client/images/logo.png";

    public static final Integer Height = 700;
    public static final Integer Width = 1000;

    public static final String CACHE_3D_DIR = resolveCacheDir("client/cache/models");
    public static final String CACHE_IMAGES_DIR = resolveCacheDir("client/cache/images");

    private static String resolveCacheDir(String defaultPath) {
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            java.io.File file = new java.io.File(userDir);
            if ("client".equals(file.getName())) {
                return defaultPath.replace("client/", "");
            }
        }
        return defaultPath;
    }

    // Cache buster for images to bypass JavaFX internal cache when images are
    // updated
    public static volatile long imageCacheBuster = System.currentTimeMillis();

    public static void triggerImageCacheBuster() {
        imageCacheBuster = System.currentTimeMillis();
    }

    static String normalizeBaseUrl(String value) {
        String normalized = hasText(value) ? value.trim() : API_URL;
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Bypasses client and CDN caching by applying cache busters.
     * For Cloudinary URLs, injects '/v<timestamp>/' version segment in the path.
     * For other URLs, appends '?t=<timestamp>' query parameter.
     */
    public static String applyCacheBuster(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        // If it's a Cloudinary URL (contains res.cloudinary.com)
        if (url.contains("res.cloudinary.com")) {
            // First remove existing version segment like /v1234567/ if present
            String cleanedUrl = url.replaceAll("/v[0-9]+/", "/");

            // Now inject our new version segment based on local cache buster
            if (cleanedUrl.contains("/image/upload/")) {
                return cleanedUrl.replace(
                        "/image/upload/",
                        "/image/upload/v" + imageCacheBuster + "/");
            } else if (cleanedUrl.contains("/raw/upload/")) {
                return cleanedUrl.replace("/raw/upload/", "/raw/upload/v" + imageCacheBuster + "/");
            }
            return cleanedUrl;
        }

        // For other URLs, use standard t= query parameter
        return url + (url.contains("?") ? "&" : "?") + "t=" + imageCacheBuster;
    }
}
