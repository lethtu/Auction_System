package com.auction.client.util;

import com.auction.client.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheManagerCoverageTest {

    private final List<Path> filesToDelete = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedFiles() throws Exception {
        for (Path file : filesToDelete) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void getMd5_returnsStableLowercaseHash() {
        assertEquals("d1e437a86c4120e58a2077631fbff4f8", CacheManager.getMd5("auction"));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", CacheManager.getMd5(""));
        assertEquals(32, CacheManager.getMd5("Auction System").length());
    }

    @Test
    void getCachedImage_returnsNullForBlankInput() {
        assertNull(CacheManager.getCachedImage(null, image -> { }));
        assertNull(CacheManager.getCachedImage("   ", image -> { }));
    }

    @Test
    void getCachedModel_returnsNullForInvalidArguments() {
        assertNull(CacheManager.getCachedModel(null, "item-1", () -> { }));
        assertNull(CacheManager.getCachedModel("http://example.com/model.glb", null, () -> { }));
        assertNull(CacheManager.getCachedModel("   ", "item-1", () -> { }));
        assertNull(CacheManager.getCachedModel("http://example.com/model.glb", "   ", () -> { }));
    }

    @Test
    void getModelAsync_returnsExistingLocalFileImmediately() throws Exception {
        String itemUuid = "cache-test-model-" + System.nanoTime();
        Path modelFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".glb");
        Path metaFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".meta");
        writeFile(modelFile, "glb-data");
        writeFile(metaFile, "lastModified=local\ncontentLength=8\netag=test");

        Path result = CacheManager.getModelAsync("http://127.0.0.1:1/model.glb", itemUuid)
                .get(2, TimeUnit.SECONDS);

        assertEquals(modelFile, result);
        assertTrue(Files.exists(modelFile));
        assertTrue(Files.exists(metaFile));
    }

    @Test
    void invalidateCache_deletesModelAndStableImageCacheFiles() throws Exception {
        String itemUuid = "cache-test-invalidate-" + System.nanoTime();
        Path modelFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".glb");
        Path modelMetaFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".meta");
        writeFile(modelFile, "model");
        writeFile(modelMetaFile, "contentLength=5");

        String imageUrl = "https://cdn.example.com/assets/photo.JPG?t=123456";
        String stableImageUrl = invokeStableCacheKeyUrl(imageUrl);
        String imageKey = CacheManager.getMd5(stableImageUrl);
        String imageExt = invokeGetExtension(stableImageUrl, "png");
        Path imageFile = Paths.get(Config.CACHE_IMAGES_DIR, imageKey + "." + imageExt);
        Path imageMetaFile = Paths.get(Config.CACHE_IMAGES_DIR, imageKey + ".meta");
        writeFile(imageFile, "image");
        writeFile(imageMetaFile, "contentLength=5");

        CacheManager.invalidateCache(itemUuid, imageUrl);

        assertFalse(Files.exists(modelFile));
        assertFalse(Files.exists(modelMetaFile));
        assertFalse(Files.exists(imageFile));
        assertFalse(Files.exists(imageMetaFile));
    }

    @Test
    void invalidateCache_ignoresBlankArguments() {
        assertDoesNotThrow(() -> CacheManager.invalidateCache(null, null));
        assertDoesNotThrow(() -> CacheManager.invalidateCache("   ", "   "));
    }

    @Test
    void stableCacheKeyUrl_removesCloudinaryVersionAndTimestampBustQuery() throws Exception {
        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/products/item.png?size=large",
                invokeStableCacheKeyUrl(" https://res.cloudinary.com/demo/image/upload/v123456/products/item.png?t=999&size=large ")
        );
        assertEquals(
                "https://example.com/images/a.jpg",
                invokeStableCacheKeyUrl("https://example.com/images/a.jpg?t=123")
        );
        assertEquals(
                "https://example.com/images/a.jpg?x=1",
                invokeStableCacheKeyUrl("https://example.com/images/a.jpg?x=1&t=123")
        );
        assertEquals("", invokeStableCacheKeyUrl("   "));
    }

    @Test
    void getExtension_extractsSafeExtensionOrDefault() throws Exception {
        assertEquals("jpg", invokeGetExtension("https://example.com/image.JPG?t=1", "png"));
        assertEquals("glb", invokeGetExtension("https://example.com/model.glb", "bin"));
        assertEquals("png", invokeGetExtension("https://example.com/no-extension", "png"));
        assertEquals("png", invokeGetExtension("https://example.com/file.toolong", "png"));
        assertEquals("png", invokeGetExtension(null, "png"));
    }

    private void writeFile(Path file, String content) throws Exception {
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