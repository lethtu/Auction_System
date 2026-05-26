package com.auction.server.controller;

import com.auction.server.service.CloudinaryService;
import com.auction.server.util.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthGetImage.class)
public class AuthGetImageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthGetImage authGetImageController;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private CloudinaryService cloudinaryService;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() {
        System.setProperty("auction.upload.dir", tempDir.toAbsolutePath().toString());
    }

    @AfterEach
    public void cleanup() throws Exception {
        System.clearProperty("auction.upload.dir");
        deleteIfExists(Paths.get("upload", "models_3d", "phase13-model"));
        deleteIfExists(Paths.get("upload", "models_3d", "phase13-model-fallback"));
        deleteIfExists(Paths.get("upload", "models_3d", "phase13-serve-model"));
        deleteIfExists(Paths.get("upload", "models_3d", "phase26-model-io"));
        Files.deleteIfExists(Paths.get("upload", "avatar", "phase13-avatar.png"));
    }

    @Test
    @DisplayName("API UploadImage: Upload áº£nh thÃ nh cÃ´ng -> Tráº£ vá» 200 vÃ  imagePath")
    public void testUploadImageSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake_image_data_123".getBytes()
        );

        mockMvc.perform(multipart("/api/files/images").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.imagePath").isNotEmpty());
    }

    @Test
    @DisplayName("API UploadImage: File khÃ´ng pháº£i áº£nh -> Tráº£ vá» 400")
    public void testUploadImageInvalidContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/files/images").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("API UploadImage: File cÃ³ Content-Type application/octet-stream nhÆ°ng Ä‘uÃ´i .png -> Tráº£ vá» 200")
    public void testUploadImageOctetStreamWithImageExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "application/octet-stream",
                "fake_image_data_123".getBytes()
        );

        mockMvc.perform(multipart("/api/files/images").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.imagePath").isNotEmpty());
    }

    @Test
    @DisplayName("API UploadImage: File rá»—ng -> Tráº£ vá» 400")
    public void testUploadImageEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/files/images").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Image file is empty."));
    }

    @Test
    @DisplayName("API UploadImage: Octet-stream vá»›i Ä‘uÃ´i khÃ´ng an toÃ n -> Tráº£ vá» 400")
    public void testUploadImageOctetStreamWithUnsafeExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.exe",
                "application/octet-stream",
                "not_an_image".getBytes()
        );

        mockMvc.perform(multipart("/api/files/images").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Only image files are accepted."));
    }

    @Test
    @DisplayName("API UploadImage: UUID truyá»n vÃ o Ä‘Æ°á»£c trim vÃ  dÃ¹ng cho local URL")
    public void testUploadImageUsesTrimmedUuidForLocalStorage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake_image_data_123".getBytes()
        );

        mockMvc.perform(multipart("/api/files/images")
                        .file(file)
                        .param("uuid", " phase13-image "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.imagePath").value("phase13-image"))
                .andExpect(jsonPath("$.data.imageUrl").value("/api/files/images/phase13-image/phase13-image.png"));

        assertTrue(Files.exists(tempDir.resolve("phase13-image").resolve("phase13-image.png")));
    }

    @Test
    @DisplayName("API UploadImage: Cloudinary lá»—i thÃ¬ fallback local")
    public void testUploadImageCloudinaryFailureFallsBackToLocalStorage() throws Exception {
        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.uploadFileWithPublicId(
                any(),
                eq("auction_system/items/images"),
                anyString(),
                eq(false)
        )).thenThrow(new RuntimeException("cloudinary down"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake_image_data_123".getBytes()
        );

        mockMvc.perform(multipart("/api/files/images")
                        .file(file)
                        .param("uuid", "phase13-fallback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.imagePath").value("phase13-fallback"))
                .andExpect(jsonPath("$.data.imageUrl").value("/api/files/images/phase13-fallback/phase13-fallback.png"));

        assertTrue(Files.exists(tempDir.resolve("phase13-fallback").resolve("phase13-fallback.png")));
    }

    @Test
    @DisplayName("API GetImage: File tá»“n táº¡i -> Tráº£ vá» 200 vÃ  ná»™i dung file")
    public void testServeFileSuccess() throws Exception {
        String fileName = "avatar.png";
        Path fakeFile = tempDir.resolve(fileName);
        Files.writeString(fakeFile, "fake_image_data_123");

        mockMvc.perform(get("/api/files/images/" + fileName))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"avatar.png\""))
                .andExpect(content().string("fake_image_data_123"));
    }

    @Test
    @DisplayName("API GetImage: File trong folder tá»“n táº¡i -> Tráº£ vá» 200")
    public void testServeFileInFolderSuccess() throws Exception {
        Path folder = tempDir.resolve("phase13-folder");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("avatar.png"), "folder_image_data");

        mockMvc.perform(get("/api/files/images/phase13-folder/avatar.png"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"avatar.png\""))
                .andExpect(content().string("folder_image_data"));
    }

    @Test
    @DisplayName("API GetImage: File khÃ´ng tá»“n táº¡i -> Tráº£ vá» 404")
    public void testServeFileNotFound() throws Exception {
        mockMvc.perform(get("/api/files/images/tung_pro_123.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("API GetImage: Lá»—i I/O ná»™i bá»™ -> Tráº£ vá» 500")
    public void testServeFileInternalServerError() throws Exception {
        System.setProperty("auction.upload.dir", "invalid\0dir");

        mockMvc.perform(get("/api/files/images/test.jpg"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("API UploadImage: Upload áº£nh thÃ nh cÃ´ng lÃªn Cloudinary -> Tráº£ vá» 200 vÃ  imagePath/imageUrl")
    public void testUploadImageCloudinarySuccess() throws Exception {
        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.uploadFileWithPublicId(
                any(),
                eq("auction_system/items/images"),
                anyString(),
                eq(false)
        )).thenReturn("https://res.cloudinary.com/testcloud/image/upload/auction_system/items/images/test-uuid.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake_image_data_123".getBytes()
        );

        mockMvc.perform(multipart("/api/files/images").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.imagePath").isNotEmpty())
                .andExpect(jsonPath("$.data.imageUrl").value("https://res.cloudinary.com/testcloud/image/upload/auction_system/items/images/test-uuid.jpg"));
    }

    @Test
    @DisplayName("API UploadModel3D: Upload file 3D thÃ nh cÃ´ng lÃªn Cloudinary -> Tráº£ vá» 200")
    public void testUploadModel3DCloudinarySuccess() throws Exception {
        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.uploadFileWithPublicId(
                any(),
                eq("auction_system/items/models_3d"),
                anyString(),
                eq(true)
        )).thenReturn("https://res.cloudinary.com/testcloud/raw/upload/auction_system/items/models_3d/test-uuid.glb");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "model.glb",
                "application/octet-stream",
                "fake_3d_data_123".getBytes()
        );

        mockMvc.perform(multipart("/api/files/models-3d").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.model3dPath").isNotEmpty())
                .andExpect(jsonPath("$.data.model3dUrl").value("https://res.cloudinary.com/testcloud/raw/upload/auction_system/items/models_3d/test-uuid.glb"));
    }

    @Test
    @DisplayName("API UploadModel3D: File rá»—ng -> Tráº£ vá» 400")
    public void testUploadModel3DEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "model.glb",
                "application/octet-stream",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/files/models-3d").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("File 3D is empty."));
    }

    @Test
    @DisplayName("API UploadModel3D: Upload local thÃ nh cÃ´ng khi khÃ´ng cáº¥u hÃ¬nh Cloudinary")
    public void testUploadModel3DLocalSuccessWithUuid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "model.glb",
                "application/octet-stream",
                "fake_3d_data_123".getBytes()
        );

        mockMvc.perform(multipart("/api/files/models-3d")
                        .file(file)
                        .param("uuid", " phase13-model "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.model3dPath").value("phase13-model"))
                .andExpect(jsonPath("$.data.model3dUrl").value("/api/files/models-3d/phase13-model/phase13-model.glb"));

        assertTrue(Files.exists(Paths.get("upload", "models_3d", "phase13-model", "phase13-model.glb")));
    }

    @Test
    @DisplayName("API UploadModel3D: Cloudinary lá»—i thÃ¬ fallback local")
    public void testUploadModel3DCloudinaryFailureFallsBackToLocalStorage() throws Exception {
        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.uploadFileWithPublicId(
                any(),
                eq("auction_system/items/models_3d"),
                anyString(),
                eq(true)
        )).thenThrow(new RuntimeException("cloudinary down"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "model.glb",
                "application/octet-stream",
                "fake_3d_data_123".getBytes()
        );

        mockMvc.perform(multipart("/api/files/models-3d")
                        .file(file)
                        .param("uuid", "phase13-model-fallback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.model3dPath").value("phase13-model-fallback"))
                .andExpect(jsonPath("$.data.model3dUrl").value("/api/files/models-3d/phase13-model-fallback/phase13-model-fallback.glb"));

        assertTrue(Files.exists(Paths.get("upload", "models_3d", "phase13-model-fallback", "phase13-model-fallback.glb")));
    }

    @Test
    @DisplayName("API GetModel3D: File tá»“n táº¡i -> Tráº£ vá» 200")
    public void testServeModel3DSuccess() throws Exception {
        Path folder = Paths.get("upload", "models_3d", "phase13-serve-model");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("phase13-serve-model.glb"), "model_data");

        mockMvc.perform(get("/api/files/models-3d/phase13-serve-model/phase13-serve-model.glb"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"phase13-serve-model.glb\""))
                .andExpect(content().string("model_data"));
    }

    @Test
    @DisplayName("API GetModel3D: File khÃ´ng tá»“n táº¡i -> Tráº£ vá» 404")
    public void testServeModel3DNotFound() throws Exception {
        mockMvc.perform(get("/api/files/models-3d/missing-folder/missing.glb"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("API GetAvatar: File tá»“n táº¡i -> Tráº£ vá» 200")
    public void testServeAvatarSuccess() throws Exception {
        Path avatarRoot = Paths.get("upload", "avatar");
        Files.createDirectories(avatarRoot);
        Files.writeString(avatarRoot.resolve("phase13-avatar.png"), "avatar_data");

        mockMvc.perform(get("/api/files/avatar/phase13-avatar.png"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"phase13-avatar.png\""))
                .andExpect(content().string("avatar_data"));
    }

    @Test
    @DisplayName("API GetAvatar: File khÃ´ng tá»“n táº¡i -> Tráº£ vá» 404")
    public void testServeAvatarNotFound() throws Exception {
        mockMvc.perform(get("/api/files/avatar/missing-avatar.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Helper riÃªng: extension, filename vÃ  text Ä‘Æ°á»£c xá»­ lÃ½ an toÃ n")
    public void testPrivateHelperBranches() {
        assertEquals(".png", ReflectionTestUtils.invokeMethod(authGetImageController, "getSafeExtension", (String) null));
        assertEquals(".png", ReflectionTestUtils.invokeMethod(authGetImageController, "getSafeExtension", "no_extension"));
        assertEquals(".jpg", ReflectionTestUtils.invokeMethod(authGetImageController, "getSafeExtension", "PHOTO.JPG"));
        assertEquals(".png", ReflectionTestUtils.invokeMethod(authGetImageController, "getSafeExtension", "archive.tar.gz!"));
        assertEquals(".png", ReflectionTestUtils.invokeMethod(authGetImageController, "getSafeExtension", "file.verylongextension"));

        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isSafeImageExtension", (String) null)));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isSafeImageExtension", ".PNG")));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isSafeImageExtension", ".exe")));

        assertEquals("image", ReflectionTestUtils.invokeMethod(authGetImageController, "safeFileName", (String) null));
        assertEquals("image", ReflectionTestUtils.invokeMethod(authGetImageController, "safeFileName", "   "));
        assertEquals("a_b_c___png", ReflectionTestUtils.invokeMethod(authGetImageController, "safeFileName", "a\\b/c\"\r\npng"));

        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "hasText", (String) null)));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "hasText", "   ")));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "hasText", "x")));

        String storedFileName = ReflectionTestUtils.invokeMethod(authGetImageController, "buildStoredFileName", "avatar.jpeg");
        assertNotNull(storedFileName);
        assertTrue(storedFileName.endsWith(".jpeg"));
        UUID.fromString(storedFileName.substring(0, storedFileName.length() - ".jpeg".length()));
    }

    @Test
    @DisplayName("Helper riÃªng: serveFrom cháº·n file náº±m ngoÃ i thÆ° má»¥c cho phÃ©p")
    public void testServeFromRejectsOutsideAllowedRoot() {
        Path allowedRoot = tempDir.resolve("allowed").toAbsolutePath().normalize();
        Path outsideFile = tempDir.resolve("outside.png").toAbsolutePath().normalize();

        ResponseEntity<Resource> response = ReflectionTestUtils.invokeMethod(
                authGetImageController,
                "serveFrom",
                outsideFile,
                allowedRoot
        );

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
    }

    private void deleteIfExists(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (Exception ignored) {
                            // best-effort cleanup for tests
                        }
                    });
        }
    }


    @Test
    @DisplayName("Extra private helpers: path and content helper branches")
    public void testAdditionalPrivateHelperBranchesForPathsAndContent() throws Exception {
        Path createdFolder = ReflectionTestUtils.invokeMethod(authGetImageController, "createItemFolder", tempDir);
        assertNotNull(createdFolder);
        assertTrue(Files.exists(createdFolder));
        assertTrue(createdFolder.normalize().startsWith(tempDir.normalize()));

        Path folder = tempDir.resolve("extra-folder");
        Files.createDirectories(folder);
        Path destination = folder.resolve("avatar.png");
        Files.writeString(destination, "image-data");

        assertEquals("extra-folder/avatar.png", ReflectionTestUtils.invokeMethod(authGetImageController, "toClientImagePath", destination));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isInsideRootLocation", destination)));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isInsideRootLocation", (Path) null)));

        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isImageContentType", "image/jpeg")));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isImageContentType", (String) null)));
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isSafeExtension", ".abc123")));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isSafeExtension", ".")));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authGetImageController, "isSafeExtension", ".toolongextension")));
    }

    @Test
    @DisplayName("serveFrom handles null path as bad request")
    public void testServeFromRejectsNullFile() {
        ResponseEntity<Resource> response = ReflectionTestUtils.invokeMethod(
                authGetImageController,
                "serveFrom",
                null,
                tempDir.toAbsolutePath().normalize()
        );

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("uploadImage returns 500 when local copy cannot read input stream")
    public void testUploadImageInputStreamFailureReturnsServerError() throws Exception {
        org.springframework.web.multipart.MultipartFile file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        when(file.getInputStream()).thenThrow(new java.io.IOException("cannot read"));

        ResponseEntity<?> response = authGetImageController.uploadImage(file, "phase26-image-io");

        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    @DisplayName("uploadModel3D rejects null file as bad request")
    public void testUploadModel3DNullFileReturnsBadRequest() {
        ResponseEntity<?> response = authGetImageController.uploadModel3D(null, "phase26-null-model");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("uploadModel3D returns 500 when local copy cannot read input stream")
    public void testUploadModel3DInputStreamFailureReturnsServerError() throws Exception {
        org.springframework.web.multipart.MultipartFile file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new java.io.IOException("cannot read model"));

        ResponseEntity<?> response = authGetImageController.uploadModel3D(file, "phase26-model-io");

        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    @DisplayName("serveModel3D catches invalid path arguments")
    public void testServeModel3DInvalidPathReturnsServerError() {
        ResponseEntity<Resource> response = authGetImageController.serveModel3D(null, "model.glb");

        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    @DisplayName("serveFileInFolder catches invalid upload root")
    public void testServeFileInFolderInvalidRootReturnsServerError() {
        System.setProperty("auction.upload.dir", "invalid\0dir");

        ResponseEntity<Resource> response = authGetImageController.serveFileInFolder("folder", "avatar.png");

        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    @DisplayName("serveAvatar catches invalid path arguments")
    public void testServeAvatarInvalidPathReturnsServerError() {
        ResponseEntity<Resource> response = authGetImageController.serveAvatar(null);

        assertEquals(500, response.getStatusCode().value());
    }


    @Test
    @DisplayName("uploadImage rejects null file")
    public void testUploadImageNullFileReturnsBadRequest() {
        ResponseEntity<?> response = authGetImageController.uploadImage(null, "phase29-null-image");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("uploadImage accepts null content type when extension is safe")
    public void testUploadImageNullContentTypeWithSafeExtensionSucceeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                null,
                "fake_image_data_123".getBytes()
        );

        mockMvc.perform(multipart("/api/files/images")
                        .file(file)
                        .param("uuid", "phase29-null-content-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.imagePath").value("phase29-null-content-type"));
    }

    @Test
    @DisplayName("serveOldFile rejects traversal path")
    public void testServeOldFileRejectsTraversalPathDirectly() {
        ResponseEntity<Resource> response = authGetImageController.serveOldFile("../outside.png");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("serveFileInFolder rejects traversal path")
    public void testServeFileInFolderRejectsTraversalPathDirectly() {
        ResponseEntity<Resource> response = authGetImageController.serveFileInFolder("../outside", "avatar.png");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("serveModel3D rejects traversal path")
    public void testServeModel3DRejectsTraversalPathDirectly() {
        ResponseEntity<Resource> response = authGetImageController.serveModel3D("../outside", "model.glb");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @DisplayName("serveAvatar rejects traversal path")
    public void testServeAvatarRejectsTraversalPathDirectly() {
        ResponseEntity<Resource> response = authGetImageController.serveAvatar("../avatar.png");

        assertEquals(400, response.getStatusCode().value());
    }


}
