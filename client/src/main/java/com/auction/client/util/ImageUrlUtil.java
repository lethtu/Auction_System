package com.auction.client.util;

import com.auction.client.Config;

public final class ImageUrlUtil {
    private static final String API_IMAGE_PREFIX = "/api/files/images/";
    private static final String[] LOCAL_IMAGE_PREFIXES = {
            "server/upload/images/",
            "upload/images/",
            "images/"
    };

    private ImageUrlUtil() {
    }

    public static String buildImageUrl(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }

        String path = rawPath.trim().replace("\\", "/");
        if (isExternalImageUrl(path)) {
            return Config.applyCacheBuster(path);
        }

        int apiIndex = path.indexOf(API_IMAGE_PREFIX);
        if (apiIndex >= 0) {
            path = path.substring(apiIndex + API_IMAGE_PREFIX.length());
        }

        path = removeLeadingSlashes(path);
        path = removeKnownImagePrefix(path);

        if (path.isBlank()) {
            return "";
        }
        return Config.applyCacheBuster(Config.API_URL + API_IMAGE_PREFIX + path);
    }

    private static boolean isExternalImageUrl(String path) {
        return (path.startsWith("http://") || path.startsWith("https://"))
                && !path.contains(API_IMAGE_PREFIX);
    }

    private static String removeLeadingSlashes(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    private static String removeKnownImagePrefix(String path) {
        for (String prefix : LOCAL_IMAGE_PREFIXES) {
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
        }
        return path;
    }
}