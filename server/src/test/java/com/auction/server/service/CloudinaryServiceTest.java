package com.auction.server.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void constructorShouldDisableCloudinaryWhenApiKeyOrSecretIsNull() {
        CloudinaryService nullApiKey = new CloudinaryService("demo", null, "secret");
        CloudinaryService nullApiSecret = new CloudinaryService("demo", "key", null);

        assertFalse(nullApiKey.isConfigured());
        assertEquals("demo", nullApiKey.getCloudName());
        assertFalse(nullApiSecret.isConfigured());
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
    void uploadFileWithPublicIdShouldUploadImageWithExpectedParams() throws Exception {
        CloudinaryService service = new CloudinaryService("", "", "");
        Cloudinary cloudinary = mockConfiguredCloudinary(
                Map.of("secure_url", "https://cdn.example.com/image.jpg")
        );
        setCloudinary(service, cloudinary);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {10, 20, 30}
        );

        String result = service.uploadFileWithPublicId(file, "items", "item-1", false);

        assertEquals("https://cdn.example.com/image.jpg", result);
        Uploader uploader = cloudinary.uploader();

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(bytesCaptor.capture(), paramsCaptor.capture());

        assertArrayEquals(new byte[] {10, 20, 30}, bytesCaptor.getValue());
        Map<?, ?> params = paramsCaptor.getValue();
        assertEquals("items", params.get("folder"));
        assertEquals("item-1", params.get("public_id"));
        assertEquals(true, params.get("overwrite"));
        assertEquals(true, params.get("invalidate"));
        assertEquals("image", params.get("resource_type"));
    }

    @Test
    void uploadFileWithPublicIdShouldUploadRawFileWithExpectedParams() throws Exception {
        CloudinaryService service = new CloudinaryService("", "", "");
        Cloudinary cloudinary = mockConfiguredCloudinary(
                Map.of("secure_url", "https://cdn.example.com/model.glb")
        );
        setCloudinary(service, cloudinary);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "model.glb",
                "model/gltf-binary",
                new byte[] {1, 2}
        );

        String result = service.uploadFileWithPublicId(file, "models", "model-1", true);

        assertEquals("https://cdn.example.com/model.glb", result);

        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(cloudinary.uploader()).upload(any(byte[].class), paramsCaptor.capture());

        Map<?, ?> params = paramsCaptor.getValue();
        assertEquals("models", params.get("folder"));
        assertEquals("model-1", params.get("public_id"));
        assertEquals("raw", params.get("resource_type"));
    }

    @Test
    void uploadFileWithPublicIdShouldPropagateFileReadFailure() throws Exception {
        CloudinaryService service = new CloudinaryService("", "", "");
        setCloudinary(service, mockConfiguredCloudinary(Map.of("secure_url", "unused")));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("cannot read file"));

        IOException ex = assertThrows(
                IOException.class,
                () -> service.uploadFileWithPublicId(file, "items", "item-1", false)
        );

        assertEquals("cannot read file", ex.getMessage());
    }

    @Test
    void getDynamicImageUrlShouldUseLocalFallbackWhenUnconfiguredOrUuidMissing() {
        CloudinaryService service = new CloudinaryService("", "", "");

        assertEquals("/api/files/images/item-1/item-1.png", service.getDynamicImageUrl("item-1"));
        assertEquals("/api/files/images/null/null.png", service.getDynamicImageUrl(null));
        assertEquals("/api/files/images/ / .png", service.getDynamicImageUrl(" "));
    }

    @Test
    void getDynamicImageUrlShouldUseLocalFallbackWhenConfiguredButUuidMissing() {
        CloudinaryService service = new CloudinaryService("demo-cloud", "key", "secret");

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
    void getDynamicModel3DUrlShouldUseLocalFallbackWhenConfiguredButUuidMissing() {
        CloudinaryService service = new CloudinaryService("demo-cloud", "key", "secret");

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

    private static Cloudinary mockConfiguredCloudinary(Map<String, Object> uploadResult)
            throws IOException {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(new HashMap<>(uploadResult));
        return cloudinary;
    }

    private static void setCloudinary(CloudinaryService service, Cloudinary cloudinary)
            throws ReflectiveOperationException {
        Field field = CloudinaryService.class.getDeclaredField("cloudinary");
        field.setAccessible(true);
        field.set(service, cloudinary);
    }
}
