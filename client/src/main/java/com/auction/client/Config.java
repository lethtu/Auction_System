package com.auction.client;

public class Config {
    public static final String API_URL = "http://127.0.0.1:8080";
    public static final String SOCKET_HOST = "127.0.0.1";
    public static final String Title = "Auction System";

    public static final Integer Height = 700;
    public static final Integer Width = 1000;
    public static final Integer PORT_SOCKET = 8081;

    // Cache buster for images to bypass JavaFX internal cache when images are updated
    public static volatile long imageCacheBuster = System.currentTimeMillis();

    public static void triggerImageCacheBuster() {
        imageCacheBuster = System.currentTimeMillis();
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
