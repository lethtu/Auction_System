package com.auction.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthGetImage.class)
public class AuthGetImageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthGetImage authGetImageController;

    // TẠO THƯ MỤC ẢO: JUnit 5 sẽ tự tạo thư mục này khi chạy và tự xóa khi test xong
    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() {
        // Tráo cái biến rootLocation cứng ngắc thành thư mục ảo
        ReflectionTestUtils.setField(authGetImageController, "rootLocation", tempDir);
    }

    // TEST 1: TÌM THẤY FILE TRẢ VỀ 200 OK

    @Test
    @DisplayName("API GetImage: File tồn tại -> Trả về 200 và nội dung file")
    public void testServeFile_Success() throws Exception {
        // CHUẨN BỊ LÚC BẮT ĐẦU: Lén tạo một file tên là "avatar.png" nhét vào thư mục ảo
        String fileName = "avatar.png";
        Path fakeFile = tempDir.resolve(fileName);
        Files.writeString(fakeFile, "fake_image_data_123"); // Dữ liệu giả

        // TEST API
        mockMvc.perform(get("/api/files/images/" + fileName))
                .andExpect(status().isOk()) // Kỳ vọng trả về HTTP 200
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"avatar.png\""))
                .andExpect(content().string("fake_image_data_123")); // Kiểm tra xem code có đọc đúng ruột file không
    }

    // TEST 2: KHÔNG TÌM THẤY FILE TRẢ VỀ 404 NOT FOUND

    @Test
    @DisplayName("API GetImage: File không tồn tại -> Trả về 404")
    public void testServeFile_NotFound() throws Exception {
        // Cố tình xin một cái tên file không hề có trong thư mục ảo
        String badFileName = "tung_pro_123.jpg";

        mockMvc.perform(get("/api/files/images/" + badFileName))
                .andExpect(status().isNotFound()); // Kỳ vọng Controller bắt được lỗi và ném ra 404
    }

    // TEST 3: LỖI BÊN TRONG SERVER (HTTP 500)

    @Test
    @DisplayName("API GetImage: Lỗi I/O nội bộ -> Trả về 500")
    public void testServeFile_InternalServerError() throws Exception {
        // Cố tình nhét null vào rootLocation để code ném Exception
        ReflectionTestUtils.setField(authGetImageController, "rootLocation", null);

        mockMvc.perform(get("/api/files/images/test.jpg"))
                .andExpect(status().isInternalServerError()); // Kỳ vọng vào khối catch (Exception e)
    }
}