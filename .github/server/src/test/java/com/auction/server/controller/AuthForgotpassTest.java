package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqForgotPass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthForgotpass.class)
public class AuthForgotpassTest {

    @Autowired
    private MockMvc mockMvc; // Công cụ giả lập Request (như Postman)

    @Autowired
    private AuthForgotpass authForgotpassController; // Controller đang được test

    // TẠO CÁC DIỄN VIÊN ĐÓNG THẾ CHO SERVER
    @MockBean
    private RqForgotPass forgotPass;

    @MockBean
    private EmailServer emailServer;

    @MockBean
    private HandleLoginSignup saveRepo; // Tương ứng với biến "Save" trong code của bạn

    private User mockUser;

    @BeforeEach
    public void setup() {
        // Tạo một User giả trước mỗi bài test
        mockUser = new User();
        mockUser.setFullname("Người Dùng Test");
        mockUser.setPassword("old_password");
    }

    // TEST API 1: /api/forgot_pass (LẤY MÃ OTP)

    @Test
    @DisplayName("API ForgotPass: Email hợp lệ -> Tạo OTP và Gửi Email")
    public void testForgotPass_EmailExists() throws Exception {
        // Trả về User có thật
        Mockito.when(forgotPass.forgot_pass("test@gmail.com")).thenReturn(Optional.of(mockUser));

        String requestJson = """
            { "email": "test@gmail.com" }
            """;

        // Bắn API
        mockMvc.perform(post("/api/forgot_pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.status").value(200)) // Logic status = 200
                .andExpect(jsonPath("$.message").value("Verification code sent"));

        // KIỂM TRA CHÉO: Xác nhận xem Controller có thực sự ra lệnh cho EmailServer gửi mail đi không?
        Mockito.verify(emailServer).SendEmail(eq("test@gmail.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("API ForgotPass: Email không tồn tại -> Báo lỗi 404")
    public void testForgotPass_EmailNotFound() throws Exception {
        // Không tìm thấy user
        Mockito.when(forgotPass.forgot_pass("wrong@gmail.com")).thenReturn(Optional.empty());

        String requestJson = """
            { "email": "wrong@gmail.com" }
            """;

        mockMvc.perform(post("/api/forgot_pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No account is associated with this email"));
    }

    // TEST API 2: /api/check_code (ĐỔI MẬT KHẨU)

    @Test
    @DisplayName("API CheckCode: Mã OTP ĐÚNG -> Lưu mật khẩu mới thành công")
    public void testCheckCode_Success() throws Exception {
        Mockito.when(forgotPass.forgot_pass("test@gmail.com")).thenReturn(Optional.of(mockUser));

        // REFLECTION:
        // Nhét mã OTP 123456 vào bộ nhớ Server
        Map<String, String> fakeOtpStorage = new ConcurrentHashMap<>();
        fakeOtpStorage.put("test@gmail.com", "123456");
        ReflectionTestUtils.setField(authForgotpassController, "otpStorage", fakeOtpStorage);

        String requestJson = """
            {
                "email": "test@gmail.com",
                "code": "123456",
                "password": "new_password_123"
            }
            """;

        mockMvc.perform(post("/api/check_code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        // KIỂM TRA CHÉO: Kiểm tra xem User đã được cập nhật pass mới và lưu vào DB chưa?
        assertEquals("new_password_123", mockUser.getPassword());
        Mockito.verify(saveRepo).save(mockUser); // Xác nhận hàm Save.save() đã được gọi
    }

    @Test
    @DisplayName("API CheckCode: Mã OTP SAI -> Báo lỗi 100")
    public void testCheckCode_WrongCode() throws Exception {
        Mockito.when(forgotPass.forgot_pass("test@gmail.com")).thenReturn(Optional.of(mockUser));

        // Nhét mã đúng là 123456 vào Server
        Map<String, String> fakeOtpStorage = new ConcurrentHashMap<>();
        fakeOtpStorage.put("test@gmail.com", "123456");
        ReflectionTestUtils.setField(authForgotpassController, "otpStorage", fakeOtpStorage);

        // Nhưng Client lại gửi lên mã 999999
        String requestJson = """
            {
                "email": "test@gmail.com",
                "code": "999999",
                "password": "new_password_123"
            }
            """;

        mockMvc.perform(post("/api/check_code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid or expired code, please try again"));
    }

    @Test
    @DisplayName("API CheckCode: Không có OTP trong bộ nhớ (Hết hạn) -> Báo lỗi 100")
    public void testCheckCode_ExpiredCode() throws Exception {
        // Cố tình không khởi tạo OTP trong bộ nhớ (otpStorage rỗng)
        String requestJson = """
            {
                "email": "test@gmail.com",
                "code": "123456",
                "password": "new_password_123"
            }
            """;

        mockMvc.perform(post("/api/check_code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No account is associated with this email"));
    }
}