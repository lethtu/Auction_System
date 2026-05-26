package com.auction.server.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudinaryServiceTest {

    @Test
    void constructorShouldDisableCloudinaryWhenPropertiesAreMissingOrBlank() {
        CloudinaryService missingCloudName = new CloudinaryService(null, "key", "secret");
        CloudinaryService blankApiKey = new CloudinaryService("demo", " ", "secret");
        CloudinaryService blankApiSecret = new CloudinaryService("demo", "key", "");

        assertFalse(missingCloudName.isConfigured());
        assertEquals("", missingCloudName.getCloudName());
        assertFalse(blankApiKey.isConfigured());
        assertEquals("demo", blankApiKey.getCloudName());
        assertFalse(blankApiSecret.isConfigured());
    }

    @Test
    void constructorShouldTrimCloudNameAndEnableCloudinaryWhenAllPropertiesExist() {
        CloudinaryService service = new CloudinaryService(" demo-cloud ", " key ", " secret ");

        assertTrue(service.isConfigured());
        assertEquals("demo-cloud", service.getCloudName());
    }

    @Test
    void uploadFileWithPublicIdShouldThrowWhenCloudinaryIsNotConfigured() {
        CloudinaryService service = new CloudinaryService("", "", "");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.uploadFileWithPublicId(file, "folder", "public-id", false)
        );

        assertEquals("Cloudinary is not configured.", ex.getMessage());
    }

    @Test
    void getDynamicImageUrlShouldUseLocalFallbackWhenUnconfiguredOrUuidMissing() {
        CloudinaryService service = new CloudinaryService("", "", "");

        assertEquals("/api/files/images/item-1/item-1.png", service.getDynamicImageUrl("item-1"));
        assertEquals("/api/files/images/null/null.png", service.getDynamicImageUrl(null));
        assertEquals("/api/files/images/ / .png", service.getDynamicImageUrl(" "));
    }

    @Test
    void getDynamicImageUrlShouldUseCloudinaryWhenConfigured() {
        CloudinaryService service = new CloudinaryService("demo-cloud", "key", "secret");

        assertEquals(
                "https://res.cloudinary.com/demo-cloud/image/upload/auction_system/items/images/item-1.jpg",
                service.getDynamicImageUrl("item-1")
        );
    }

    @Test
    void getDynamicModel3DUrlShouldUseLocalFallbackWhenUnconfiguredOrUuidMissing() {
        CloudinaryService service = new CloudinaryService("", "", "");

        assertEquals(
                "/api/files/models-3d/model-1/model-1.glb",
                service.getDynamicModel3DUrl("model-1")
        );
        assertEquals("/api/files/models-3d/null/null.glb", service.getDynamicModel3DUrl(null));
        assertEquals("/api/files/models-3d/ / .glb", service.getDynamicModel3DUrl(" "));
    }

    @Test
    void getDynamicModel3DUrlShouldUseCloudinaryWhenConfigured() {
        CloudinaryService service = new CloudinaryService("demo-cloud", "key", "secret");

        assertEquals(
                "https://res.cloudinary.com/demo-cloud/raw/upload/auction_system/items/models_3d/model-1.glb",
                service.getDynamicModel3DUrl("model-1")
        );
    }
}
