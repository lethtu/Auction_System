package com.auction.client.util;

import com.auction.client.Config;
import javafx.application.Platform;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "asset-cache-worker");
        thread.setDaemon(true);
        return thread;
    });

    private CacheManager() {
    }

    static {
        ensureCacheDirectories();
    }

    private static void ensureCacheDirectories() {
        try {
            Files.createDirectories(Paths.get(Config.CACHE_3D_DIR));
            Files.createDirectories(Paths.get(Config.CACHE_IMAGES_DIR));
        } catch (IOException e) {
            logger.warn("Failed to create cache directories", e);
        }
    }

    public static Image getCachedImage(String imageUrl, Consumer<Image> onImageLoaded) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        ensureCacheDirectories();

        String key = md5(imageUrl);
        String ext = resolveExtension(imageUrl, "png");
        Path cachedFile = Paths.get(Config.CACHE_IMAGES_DIR, key + "." + ext);
        Path metaFile = Paths.get(Config.CACHE_IMAGES_DIR, key + ".meta");

        Image cachedImage = null;
        boolean hasLocal = Files.exists(cachedFile) && Files.exists(metaFile);

        if (hasLocal) {
            try {
                cachedImage = new Image(cachedFile.toUri().toString(), true);
            } catch (Exception e) {
                logger.warn("Failed to load cached image {}", cachedFile, e);
                hasLocal = false;
            }
        }

        validateAndDownloadAsync(imageUrl, cachedFile, metaFile, updated -> {
            if (updated && onImageLoaded != null) {
                try {
                    Image updatedImage = new Image(cachedFile.toUri().toString(), true);
                    Platform.runLater(() -> onImageLoaded.accept(updatedImage));
                } catch (Exception e) {
                    logger.warn("Failed to load updated cached image {}", cachedFile, e);
                }
            }
        });

        return hasLocal ? cachedImage : new Image(imageUrl, true);
    }

    public static Path getCachedModel(String modelUrl, String itemUuid, Runnable onUpdated) {
        if (modelUrl == null || modelUrl.isBlank() || itemUuid == null || itemUuid.isBlank()) {
            return null;
        }

        ensureCacheDirectories();

        String safeUuid = itemUuid.replaceAll("[^0-9a-fA-F-]", "");
        if (safeUuid.isBlank()) {
            return null;
        }

        Path cachedFile = Paths.get(Config.CACHE_3D_DIR, safeUuid + ".glb");
        Path metaFile = Paths.get(Config.CACHE_3D_DIR, safeUuid + ".meta");
        boolean hasLocal = Files.exists(cachedFile) && Files.exists(metaFile);

        validateAndDownloadAsync(modelUrl, cachedFile, metaFile, updated -> {
            if (updated && onUpdated != null) {
                Platform.runLater(onUpdated);
            }
        });

        return hasLocal ? cachedFile : null;
    }

    private static void validateAndDownloadAsync(String url, Path cachedFile, Path metaFile, Consumer<Boolean> onComplete) {
        EXECUTOR.submit(() -> {
            try {
                AssetMeta remoteMeta = fetchRemoteMeta(url);
                boolean needsDownload = needsDownload(cachedFile, metaFile, remoteMeta);

                if (needsDownload) {
                    downloadAndCache(url, cachedFile, metaFile, remoteMeta, onComplete);
                } else {
                    onComplete.accept(false);
                }
            } catch (Exception e) {
                logger.debug("Cache validation failed for {}: {}", url, e.getMessage());
                if (!Files.exists(cachedFile)) {
                    downloadAndCache(url, cachedFile, metaFile, AssetMeta.empty(), onComplete);
                } else {
                    onComplete.accept(false);
                }
            }
        });
    }

    private static AssetMeta fetchRemoteMeta(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(4))
                .build();

        HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HEAD returned status " + response.statusCode());
        }

        return new AssetMeta(
                response.headers().firstValue("Last-Modified").orElse(""),
                response.headers().firstValue("Content-Length").orElse(""),
                response.headers().firstValue("ETag").orElse("")
        );
    }

    private static boolean needsDownload(Path cachedFile, Path metaFile, AssetMeta remoteMeta) throws IOException {
        if (!Files.exists(cachedFile) || !Files.exists(metaFile)) {
            return true;
        }

        AssetMeta localMeta = readMeta(metaFile);
        boolean sizeMatches = remoteMeta.contentLength.isBlank() || remoteMeta.contentLength.equals(localMeta.contentLength);
        boolean etagMatches = remoteMeta.etag.isBlank() || remoteMeta.etag.equals(localMeta.etag);
        boolean dateMatches = remoteMeta.lastModified.isBlank() || remoteMeta.lastModified.equals(localMeta.lastModified);

        return !(sizeMatches && etagMatches && dateMatches);
    }

    private static void downloadAndCache(String url, Path cachedFile, Path metaFile, AssetMeta meta, Consumer<Boolean> onComplete) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.debug("Download returned status {} for {}", response.statusCode(), url);
                onComplete.accept(false);
                return;
            }

            byte[] bytes = response.body();
            Files.createDirectories(cachedFile.getParent());
            Files.write(cachedFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            AssetMeta finalMeta = new AssetMeta(
                    meta.lastModified.isBlank() ? response.headers().firstValue("Last-Modified").orElse("") : meta.lastModified,
                    meta.contentLength.isBlank() ? response.headers().firstValue("Content-Length").orElse(String.valueOf(bytes.length)) : meta.contentLength,
                    meta.etag.isBlank() ? response.headers().firstValue("ETag").orElse("") : meta.etag
            );
            writeMeta(metaFile, finalMeta);
            onComplete.accept(true);
        } catch (Exception e) {
            logger.debug("Failed to download/cache {}: {}", url, e.getMessage());
            onComplete.accept(false);
        }
    }

    private static AssetMeta readMeta(Path metaFile) throws IOException {
        String lastModified = "";
        String contentLength = "";
        String etag = "";

        for (String line : Files.readAllLines(metaFile, StandardCharsets.UTF_8)) {
            if (line.startsWith("lastModified=")) {
                lastModified = line.substring("lastModified=".length());
            } else if (line.startsWith("contentLength=")) {
                contentLength = line.substring("contentLength=".length());
            } else if (line.startsWith("etag=")) {
                etag = line.substring("etag=".length());
            }
        }

        return new AssetMeta(lastModified, contentLength, etag);
    }

    private static void writeMeta(Path metaFile, AssetMeta meta) throws IOException {
        Files.write(metaFile, List.of(
                        "lastModified=" + meta.lastModified,
                        "contentLength=" + meta.contentLength,
                        "etag=" + meta.etag
                ), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String resolveExtension(String url, String fallback) {
        String path = url.split("\\?")[0];
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        if (lastDot > lastSlash && lastDot >= 0 && lastDot + 1 < path.length()) {
            String ext = path.substring(lastDot + 1).toLowerCase();
            if (ext.matches("[a-z0-9]{1,5}")) {
                return ext;
            }
        }
        return fallback;
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private record AssetMeta(String lastModified, String contentLength, String etag) {
        static AssetMeta empty() {
            return new AssetMeta("", "", "");
        }
    }
}
