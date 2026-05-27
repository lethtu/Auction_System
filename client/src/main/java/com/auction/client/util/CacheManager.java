package com.auction.client.util;

import com.auction.client.Config;
import javafx.application.Platform;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();
    private static final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "cache-manager-worker");
        thread.setDaemon(true);
        return thread;
    });
    private static final java.util.Map<String, java.util.concurrent.CompletableFuture<Boolean>> activeDownloads =
            new java.util.concurrent.ConcurrentHashMap<>();

    static {
        try {
            Files.createDirectories(Paths.get(Config.CACHE_3D_DIR));
            Files.createDirectories(Paths.get(Config.CACHE_IMAGES_DIR));
        } catch (IOException e) {
            logger.error("Failed to create cache directories", e);
        }
    }

    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }

    /**
     * Resolves the extension from a URL or defaults to png for images.
     */
    private static String getExtension(String url, String defaultExt) {
        if (url == null) return defaultExt;
        String path = url.split("\\?")[0];
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot > path.lastIndexOf('/')) {
            String ext = path.substring(lastDot + 1).toLowerCase();
            if (ext.length() <= 4 && ext.matches("[a-zA-Z0-9]+")) {
                return ext;
            }
        }
        return defaultExt;
    }

    /**
     * Retrieves an image from the local cache if available, while asynchronously
     * verifying its freshness against the server. Updates the cache and triggers 
     * onImageLoaded callback on the JavaFX Thread if a newer image is downloaded.
     */
    public static Image getCachedImage(String imageUrl, Consumer<Image> onImageLoaded) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String stableUrl = stableCacheKeyUrl(imageUrl);
        String key = getMd5(stableUrl);
        String ext = getExtension(stableUrl, "png");
        Path cachedFile = Paths.get(Config.CACHE_IMAGES_DIR, key + "." + ext);
        Path metaFile = Paths.get(Config.CACHE_IMAGES_DIR, key + ".meta");

        Image cachedImage = null;
        boolean hasLocal = Files.exists(cachedFile) && Files.exists(metaFile);

        if (hasLocal) {
            try {
                logger.info("Loading image from local cache: {}", cachedFile.toAbsolutePath());
                cachedImage = new Image(cachedFile.toUri().toString(), false);
            } catch (Exception e) {
                logger.warn("Failed to load cached image from {}, falling back to remote", cachedFile, e);
                hasLocal = false;
            }
        }

        final boolean finalHasLocal = hasLocal;

        // Asynchronously check for server updates and download/reload if needed
        validateAndDownloadAsync(imageUrl, cachedFile, metaFile, isUpdated -> {
            if (isUpdated || !finalHasLocal) {
                logger.info("Local image cache updated or loaded for the first time, reloading: {}", cachedFile.toAbsolutePath());
                try {
                    Image newImage = new Image(cachedFile.toUri().toString(), false);
                    Platform.runLater(() -> onImageLoaded.accept(newImage));
                } catch (Exception e) {
                    logger.error("Failed to load updated image from local file", e);
                }
            }
        });

        if (finalHasLocal) {
            return cachedImage;
        } else {
            // Return 1x1 transparent image to avoid duplicate download & lag; callback will set the real image once downloaded
            return new javafx.scene.image.WritableImage(1, 1);
        }
    }

    private static String stableCacheKeyUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }

        String stable = url.trim();
        stable = stable.replaceAll("/v\\d+/", "/");
        stable = stable.replaceAll("([?&])t=\\d+(&?)", "$1");
        stable = stable.replaceAll("[?&]$", "");
        stable = stable.replace("?&", "?");
        return stable;
    }

    /**
     * Retrieves the local cached 3D model path if it exists, and starts an asynchronous
     * server validation check. Triggers onUpdated callback on JavaFX Thread if updated.
     */
    public static Path getCachedModel(String modelUrl, String itemUuid, Runnable onUpdated) {
        if (modelUrl == null || modelUrl.isBlank() || itemUuid == null || itemUuid.isBlank()) {
            return null;
        }

        Path cachedFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".glb");
        Path metaFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".meta");

        boolean hasLocal = Files.exists(cachedFile) && Files.exists(metaFile);

        validateAndDownloadAsync(modelUrl, cachedFile, metaFile, isUpdated -> {
            if (isUpdated) {
                logger.info("Local 3D model cache updated: {}", cachedFile.toAbsolutePath());
                Platform.runLater(onUpdated);
            }
        });

        return hasLocal ? cachedFile : null;
    }

    /**
     * Asynchronously gets the cached 3D model path, waiting for download if currently in progress.
     */
    public static java.util.concurrent.CompletableFuture<Path> getModelAsync(String modelUrl, String itemUuid) {
        java.util.concurrent.CompletableFuture<Path> result = new java.util.concurrent.CompletableFuture<>();

        Path cachedFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".glb");
        Path metaFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".meta");

        if (Files.exists(cachedFile) && Files.exists(metaFile)) {
            result.complete(cachedFile);
            validateAndDownloadAsync(modelUrl, cachedFile, metaFile, isUpdated -> {});
            return result;
        }

        validateAndDownloadAsync(modelUrl, cachedFile, metaFile, isUpdated -> {
            if (Files.exists(cachedFile)) {
                result.complete(cachedFile);
            } else {
                result.complete(null);
            }
        });

        return result;
    }

    /**
     * Sends a HEAD request to check for updates. If outdated or doesn't exist, downloads and updates.
     */
    private static void validateAndDownloadAsync(String fileUrl, Path cachedFile, Path metaFile, Consumer<Boolean> onComplete) {
        java.util.concurrent.CompletableFuture<Boolean> future = activeDownloads.get(fileUrl);
        if (future != null) {
            logger.info("Asset is already downloading: {}", fileUrl);
            future.thenAccept(onComplete);
            return;
        }

        java.util.concurrent.CompletableFuture<Boolean> newFuture = new java.util.concurrent.CompletableFuture<>();
        java.util.concurrent.CompletableFuture<Boolean> existingFuture = activeDownloads.putIfAbsent(fileUrl, newFuture);
        if (existingFuture != null) {
            logger.info("Asset is already downloading: {}", fileUrl);
            existingFuture.thenAccept(onComplete);
            return;
        }

        executor.submit(() -> {
            try {
                // Get remote metadata via HTTP HEAD request
                HttpRequest headRequest = HttpRequest.newBuilder()
                        .uri(URI.create(fileUrl))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(java.time.Duration.ofSeconds(4))
                        .build();

                HttpResponse<Void> headResponse = httpClient.send(headRequest, HttpResponse.BodyHandlers.discarding());
                if (headResponse.statusCode() != 200) {
                    logger.warn("HEAD request returned status {}, downloading directly", headResponse.statusCode());
                    downloadAndCache(fileUrl, cachedFile, metaFile, isUpdated -> {
                        newFuture.complete(isUpdated);
                        onComplete.accept(isUpdated);
                    });
                    return;
                }

                String remoteLastModified = headResponse.headers().firstValue("Last-Modified").orElse("");
                String remoteContentLength = headResponse.headers().firstValue("Content-Length").orElse("");
                String remoteETag = headResponse.headers().firstValue("ETag").orElse("");

                boolean needsDownload = true;

                if (Files.exists(cachedFile) && Files.exists(metaFile)) {
                    List<String> metaLines = Files.readAllLines(metaFile, StandardCharsets.UTF_8);
                    String localLastModified = "";
                    String localContentLength = "";
                    String localETag = "";

                    for (String line : metaLines) {
                        if (line.startsWith("lastModified=")) {
                            localLastModified = line.substring("lastModified=".length());
                        } else if (line.startsWith("contentLength=")) {
                            localContentLength = line.substring("contentLength=".length());
                        } else if (line.startsWith("etag=")) {
                            localETag = line.substring("etag=".length());
                        }
                    }

                    // Compare metadata
                    boolean sizeMatches = remoteContentLength.isEmpty() || remoteContentLength.equals(localContentLength);
                    boolean eTagMatches = remoteETag.isEmpty() || remoteETag.equals(localETag);
                    boolean dateMatches = remoteLastModified.isEmpty() || remoteLastModified.equals(localLastModified);

                    if (sizeMatches && eTagMatches && dateMatches) {
                        needsDownload = false;
                    }
                }

                if (needsDownload) {
                    downloadAndCache(fileUrl, cachedFile, metaFile, remoteLastModified, remoteContentLength, remoteETag, isUpdated -> {
                        newFuture.complete(isUpdated);
                        onComplete.accept(isUpdated);
                    });
                } else {
                    newFuture.complete(false);
                    onComplete.accept(false);
                }

            } catch (Exception e) {
                logger.warn("Failed to validate cache for URL {}: {}", fileUrl, e.getMessage());
                if (!Files.exists(cachedFile)) {
                    downloadAndCache(fileUrl, cachedFile, metaFile, isUpdated -> {
                        newFuture.complete(isUpdated);
                        onComplete.accept(isUpdated);
                    });
                } else {
                    newFuture.complete(false);
                    onComplete.accept(false);
                }
            } finally {
                activeDownloads.remove(fileUrl);
            }
        });
    }

    private static void downloadAndCache(String fileUrl, Path cachedFile, Path metaFile, Consumer<Boolean> onComplete) {
        downloadAndCache(fileUrl, cachedFile, metaFile, "", "", "", onComplete);
    }

    private static void downloadAndCache(String fileUrl, Path cachedFile, Path metaFile, 
                                         String lastModified, String contentLength, String etag, 
                                         Consumer<Boolean> onComplete) {
        try {
            logger.info("Downloading and caching asset from: {}", fileUrl);
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .GET()
                    .build();

            HttpResponse<byte[]> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (getResponse.statusCode() == 200) {
                byte[] bytes = getResponse.body();
                
                // Write asset file
                Files.createDirectories(cachedFile.getParent());
                Files.write(cachedFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // Extract headers from GET response if HEAD was not populated
                String finalLastModified = lastModified.isEmpty() ? getResponse.headers().firstValue("Last-Modified").orElse("") : lastModified;
                String finalContentLength = contentLength.isEmpty() ? getResponse.headers().firstValue("Content-Length").orElse("") : contentLength;
                if (finalContentLength.isEmpty()) {
                    finalContentLength = String.valueOf(bytes.length);
                }
                String finalETag = etag.isEmpty() ? getResponse.headers().firstValue("ETag").orElse("") : etag;

                // Write metadata file
                List<String> metaLines = List.of(
                        "lastModified=" + finalLastModified,
                        "contentLength=" + finalContentLength,
                        "etag=" + finalETag
                );
                Files.write(metaFile, metaLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                logger.info("Successfully cached asset to: {}", cachedFile.toAbsolutePath());
                onComplete.accept(true);
            } else {
                logger.warn("Download request failed with status {}", getResponse.statusCode());
                onComplete.accept(false);
            }
        } catch (Exception e) {
            logger.error("Error downloading/caching asset from {}", fileUrl, e);
            onComplete.accept(false);
        }
    }

    /**
     * Invalidates (deletes) the local cache files for a given item UUID and its image URL.
     */
    public static void invalidateCache(String itemUuid, String imageUrl) {
        if (itemUuid != null && !itemUuid.isBlank()) {
            try {
                Path modelFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".glb");
                Path modelMetaFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".meta");
                Files.deleteIfExists(modelFile);
                Files.deleteIfExists(modelMetaFile);
                logger.info("Invalidated local 3D model cache for item: {}", itemUuid);
            } catch (Exception e) {
                logger.warn("Failed to delete 3D model cache", e);
            }
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                String stableUrl = stableCacheKeyUrl(imageUrl);
                String key = getMd5(stableUrl);
                Path cachedFile = Paths.get(Config.CACHE_IMAGES_DIR, key + "." + getExtension(stableUrl, "png"));
                Path metaFile = Paths.get(Config.CACHE_IMAGES_DIR, key + ".meta");
                Files.deleteIfExists(cachedFile);
                Files.deleteIfExists(metaFile);
                logger.info("Invalidated local image cache for URL: {}", imageUrl);
            } catch (Exception e) {
                logger.warn("Failed to delete image cache for {}", imageUrl, e);
            }
        }
    }
}
