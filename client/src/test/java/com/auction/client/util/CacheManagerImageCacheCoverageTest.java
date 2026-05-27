package com.auction.client.util;

import com.auction.client.Config;
import javafx.scene.image.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheManagerImageCacheCoverageTest {

    private static final byte[] ONE_BY_ONE_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
    );

    private final List<Path> filesToDelete = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedFiles() throws Exception {
        for (Path file : filesToDelete) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void getCachedImage_returnsOnePixelPlaceholderWhenNoLocalCacheExists() {
        AtomicReference<Image> callbackImage = new AtomicReference<>();

        Image image = CacheManager.getCachedImage("http://[invalid/no-local-image.png?t=123", callbackImage::set);

        assertNotNull(image);
        assertEquals(1.0, image.getWidth());
        assertEquals(1.0, image.getHeight());
        assertNull(callbackImage.get());
    }

    @Test
    void getCachedImage_returnsExistingLocalImageWhenImageAndMetadataExist() throws Exception {
        String imageUrl = "http://[invalid/local-cache-image.png?t=123456";
        String stableImageUrl = invokeStableCacheKeyUrl(imageUrl);
        Path cachedImage = imageCachePath(stableImageUrl);
        Path metaFile = imageMetaPath(stableImageUrl);
        writeBytes(cachedImage, ONE_BY_ONE_PNG);
        writeText(metaFile, "lastModified=local\ncontentLength=" + ONE_BY_ONE_PNG.length + "\netag=local");

        AtomicReference<Image> callbackImage = new AtomicReference<>();

        Image image = CacheManager.getCachedImage(imageUrl, callbackImage::set);

        assertNotNull(image);
        assertTrue(image.getWidth() >= 1.0);
        assertTrue(image.getHeight() >= 1.0);
        assertNull(callbackImage.get());
    }

    @Test
    void getCachedImage_returnsPlaceholderImmediatelyWhenMetadataIsMissing() throws Exception {
        String imageUrl = "http://[invalid/image-without-meta.png?t=777";
        String stableImageUrl = invokeStableCacheKeyUrl(imageUrl);
        Path cachedImage = imageCachePath(stableImageUrl);
        Path metaFile = imageMetaPath(stableImageUrl);
        writeBytes(cachedImage, ONE_BY_ONE_PNG);
        Files.deleteIfExists(metaFile);
        filesToDelete.add(metaFile);

        AtomicReference<Image> callbackImage = new AtomicReference<>();

        Image image = CacheManager.getCachedImage(imageUrl, callbackImage::set);

        assertNotNull(image);
        assertEquals(1.0, image.getWidth());
        assertEquals(1.0, image.getHeight());
    }

    private Path imageCachePath(String stableImageUrl) throws Exception {
        String key = CacheManager.getMd5(stableImageUrl);
        String ext = invokeGetExtension(stableImageUrl, "png");
        return Paths.get(Config.CACHE_IMAGES_DIR, key + "." + ext);
    }

    private Path imageMetaPath(String stableImageUrl) {
        return Paths.get(Config.CACHE_IMAGES_DIR, CacheManager.getMd5(stableImageUrl) + ".meta");
    }

    private void writeBytes(Path file, byte[] content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, content);
        filesToDelete.add(file);
    }

    private void writeText(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        filesToDelete.add(file);
    }

    private static String invokeStableCacheKeyUrl(String url) throws Exception {
        Method method = CacheManager.class.getDeclaredMethod("stableCacheKeyUrl", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, url);
    }

    private static String invokeGetExtension(String url, String defaultExt) throws Exception {
        Method method = CacheManager.class.getDeclaredMethod("getExtension", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, url, defaultExt);
    }
}
