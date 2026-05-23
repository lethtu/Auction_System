package com.auction.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.auction.server.util.SessionManager;
import com.auction.server.service.CloudinaryService;
import org.springframework.boot.test.mock.mockito.MockBean;

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

    @Test
    @DisplayName("API UploadImage: Upload ảnh thành công -> Trả về 200 và imagePath")
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
    @DisplayName("API UploadImage: File không phải ảnh -> Trả về 400")
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
    @DisplayName("API UploadImage: File có Content-Type application/octet-stream nhưng đuôi .png -> Trả về 200")
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
    @DisplayName("API GetImage: File tồn tại -> Trả về 200 và nội dung file")
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
    @DisplayName("API GetImage: File không tồn tại -> Trả về 404")
    public void testServeFileNotFound() throws Exception {
        mockMvc.perform(get("/api/files/images/tung_pro_123.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("API GetImage: Lỗi I/O nội bộ -> Trả về 500")
    public void testServeFileInternalServerError() throws Exception {
        System.setProperty("auction.upload.dir", "invalid\0dir");

        mockMvc.perform(get("/api/files/images/test.jpg"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("API UploadImage: Upload ảnh thành công lên Cloudinary -> Trả về 200 và imagePath/imageUrl")
    public void testUploadImageCloudinarySuccess() throws Exception {
        org.mockito.Mockito.when(cloudinaryService.isConfigured()).thenReturn(true);
        org.mockito.Mockito.when(cloudinaryService.uploadFileWithPublicId(
                org.mockito.Mockito.any(),
                org.mockito.Mockito.eq("auction_system/items/images"),
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.eq(false)
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
    @DisplayName("API UploadModel3D: Upload file 3D thành công lên Cloudinary -> Trả về 200")
    public void testUploadModel3DCloudinarySuccess() throws Exception {
        org.mockito.Mockito.when(cloudinaryService.isConfigured()).thenReturn(true);
        org.mockito.Mockito.when(cloudinaryService.uploadFileWithPublicId(
                org.mockito.Mockito.any(),
                org.mockito.Mockito.eq("auction_system/items/models_3d"),
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.eq(true)
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
}
