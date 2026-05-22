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
import org.springframework.boot.test.mock.mockito.MockBean;

@WebMvcTest(AuthGetImage.class)
public class AuthGetImageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthGetImage authGetImageController;

    @MockBean
    private SessionManager sessionManager;

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
}
