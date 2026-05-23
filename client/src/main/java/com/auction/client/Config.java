package com.auction.client;

public class Config {
    public static final String API_URL = "http://127.0.0.1:8080";
    public static final String SOCKET_HOST = "127.0.0.1";
    public static final String Title = "Auction System";

    public static final Integer Height = 700;
    public static final Integer Width = 1000;
    public static final Integer PORT_SOCKET = 8081;

    public static volatile long imageCacheBuster = System.currentTimeMillis();

    public static void triggerImageCacheBuster() {
        imageCacheBuster = System.currentTimeMillis();
    }

    public static String applyCacheBuster(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        if (url.contains("res.cloudinary.com")) {
            String cleanedUrl = url.replaceAll("/v[0-9]+/", "/");
            if (cleanedUrl.contains("/image/upload/")) {
                return cleanedUrl.replace("/image/upload/", "/image/upload/v" + imageCacheBuster + "/");
            }
            if (cleanedUrl.contains("/raw/upload/")) {
                return cleanedUrl.replace("/raw/upload/", "/raw/upload/v" + imageCacheBuster + "/");
            }
            return cleanedUrl;
        }

        return url + (url.contains("?") ? "&" : "?") + "t=" + imageCacheBuster;
    }
}
