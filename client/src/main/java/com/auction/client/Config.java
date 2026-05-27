package com.auction.client;

public class Config {
    private static final String DEFAULT_API_URL = "http://127.0.0.1:8080";
    private static final String DEFAULT_SOCKET_HOST = "127.0.0.1";
    private static final int DEFAULT_SOCKET_PORT = 8081;

    public static final String API_URL = normalizeBaseUrl(readTextConfig(
            "auction.api.url",
            "AUCTION_API_URL",
            DEFAULT_API_URL
    ));
    public static final String SOCKET_HOST = readTextConfig(
            "auction.socket.host",
            "AUCTION_SOCKET_HOST",
            DEFAULT_SOCKET_HOST
    );
    public static final String Title = "Auction System";
    public static final String LOGO_PATH = "/com/auction/client/images/logo.png";

    public static final Integer Height = 700;
    public static final Integer Width = 1000;
    public static final Integer PORT_SOCKET = readPositiveIntConfig(
            "auction.socket.port",
            "AUCTION_SOCKET_PORT",
            DEFAULT_SOCKET_PORT
    );

    public static final String CACHE_3D_DIR = "client/cache/models";
    public static final String CACHE_IMAGES_DIR = "client/cache/images";

    // Cache buster for images to bypass JavaFX internal cache when images are updated
    public static volatile long imageCacheBuster = System.currentTimeMillis();

    public static void triggerImageCacheBuster() {
        imageCacheBuster = System.currentTimeMillis();
    }

    static String readTextConfig(String systemPropertyName, String envName, String defaultValue) {
        String systemValue = System.getProperty(systemPropertyName);
        if (hasText(systemValue)) {
            return systemValue.trim();
        }

        String envValue = System.getenv(envName);
        if (hasText(envValue)) {
            return envValue.trim();
        }

        return defaultValue;
    }

    static Integer readPositiveIntConfig(String systemPropertyName, String envName, int defaultValue) {
        String rawValue = readTextConfig(systemPropertyName, envName, String.valueOf(defaultValue));
        try {
            int parsedValue = Integer.parseInt(rawValue.trim());
            return parsedValue > 0 ? parsedValue : defaultValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    static String normalizeBaseUrl(String value) {
        String normalized = hasText(value) ? value.trim() : DEFAULT_API_URL;
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
                return cleanedUrl.replace("/image/upload/", "/image/upload/v" + imageCacheBuster + "/");
            } else if (cleanedUrl.contains("/raw/upload/")) {
                return cleanedUrl.replace("/raw/upload/", "/raw/upload/v" + imageCacheBuster + "/");
            }
            return cleanedUrl;
        }

        // For other URLs, use standard t= query parameter
        return url + (url.contains("?") ? "&" : "?") + "t=" + imageCacheBuster;
    }
}
