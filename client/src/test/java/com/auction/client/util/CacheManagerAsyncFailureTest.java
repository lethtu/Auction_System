package com.auction.client.util;

import com.auction.client.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class CacheManagerAsyncFailureTest {

    private final List<Path> filesToDelete = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedFiles() throws Exception {
        for (Path file : filesToDelete) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void getCachedModel_returnsExistingLocalModelEvenWhenRemoteValidationUrlIsInvalid() throws Exception {
        String itemUuid = uniqueItem("existing-model");
        Path modelFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".glb");
        Path metaFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".meta");
        writeFile(modelFile, "local glb");
        writeFile(metaFile, "lastModified=local\ncontentLength=9\netag=local");

        Path result = CacheManager.getCachedModel("http://[invalid", itemUuid, () -> {
            throw new AssertionError("Invalid remote validation must not mark an existing local model as updated.");
        });

        assertEquals(modelFile, result);
    }

    @Test
    void getCachedModel_returnsNullWhenModelFileExistsButMetadataIsMissing() throws Exception {
        String itemUuid = uniqueItem("missing-meta");
        Path modelFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".glb");
        writeFile(modelFile, "local glb without metadata");

        Path result = CacheManager.getCachedModel("http://[invalid", itemUuid, () -> {
            throw new AssertionError("Missing metadata should not be treated as an updated cache hit.");
        });

        assertNull(result);
    }

    @Test
    void getModelAsync_completesWithNullWhenRemoteUrlIsInvalidAndNoLocalCacheExists() throws Exception {
        String itemUuid = uniqueItem("invalid-remote");
        Path modelFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".glb");
        Path metaFile = Paths.get(Config.CACHE_3D_DIR, itemUuid + ".meta");
        Files.deleteIfExists(modelFile);
        Files.deleteIfExists(metaFile);
        filesToDelete.add(modelFile);
        filesToDelete.add(metaFile);

        Path result = CacheManager.getModelAsync("http://[invalid", itemUuid)
                .get(2, TimeUnit.SECONDS);

        assertNull(result);
        assertFalse(Files.exists(modelFile));
        assertFalse(Files.exists(metaFile));
    }

    @Test
    void invalidateCache_deletesImageCacheWhenItemUuidIsBlank() throws Exception {
        String imageUrl = "https://cdn.example.com/items/auction-photo.jpg?t=123456";
        String stableImageUrl = "https://cdn.example.com/items/auction-photo.jpg";
        String imageKey = CacheManager.getMd5(stableImageUrl);
        Path imageFile = Paths.get(Config.CACHE_IMAGES_DIR, imageKey + ".jpg");
        Path metaFile = Paths.get(Config.CACHE_IMAGES_DIR, imageKey + ".meta");
        writeFile(imageFile, "image bytes");
        writeFile(metaFile, "contentLength=11");

        CacheManager.invalidateCache("   ", imageUrl);

        assertFalse(Files.exists(imageFile));
        assertFalse(Files.exists(metaFile));
    }

    private String uniqueItem(String prefix) {
        return "cache-manager-" + prefix + "-" + System.nanoTime();
    }

    private void writeFile(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        filesToDelete.add(file);
    }
}