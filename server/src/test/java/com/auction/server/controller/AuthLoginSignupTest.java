package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqLoginSignup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.auction.server.util.SessionManager;

@WebMvcTest(AuthLoginSignup.class)
public class AuthLoginSignupTest {

    @Autowired
    private MockMvc mockMvc; // Công cụ giả lập bắn Request (như Postman)

    @MockBean
    private SessionManager sessionManager;

    // Tạo diễn viên đóng thế cho Database và Dịch vụ Email
    @MockBean
    private RqLoginSignup rq;

    @MockBean
    private EmailServer emailServer;

    private User mockUser;

    @BeforeEach
    public void setup() {
        // Chuẩn bị sẵn một User chuẩn bị cho các bài test
        mockUser = new User();
        // Bạn có thể cần gọi mockUser.setId(1) tùy thuộc vào model User của bạn
        mockUser.setUsername("testuser");
        mockUser.setPassword("matkhau123");
        mockUser.setFullname("Người Dùng Thử Nghiệm");
        mockUser.setEmail("test@gmail.com");
    }

    // =====================================================================
    // TEST API 1: /api/login (ĐĂNG NHẬP)
    // =====================================================================

    @Test
    @DisplayName("API Login: Đúng tài khoản -> Trả về 200 và User")
    public void testLogin_Success() throws Exception {
        // MỚM LỜI GIẢ: Khi DB nhận được đúng username/password này, trả về mockUser
        Mockito.when(rq.login("testuser", "matkhau123")).thenReturn(Optional.of(mockUser));

        String requestJson = """
            {
                "username": "testuser",
                "password": "matkhau123"
            }
            """;

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk()) // API HTTP Status = 200
                .andExpect(jsonPath("$.status").value(200)) // Trạng thái logic của bạn = 200
                .andExpect(jsonPath("$.message").value("Login successful"));
        // .andExpect(jsonPath("$.data").exists()); // Có thể bật lên nếu muốn check data trả về
    }

    @Test
    @DisplayName("API Login: Sai tài khoản/mật khẩu -> Trả về 401")
    public void testLogin_Fail() throws Exception {
        // MỚM LỜI GIẢ: Incorrect info -> Optional rỗng
        Mockito.when(rq.login("wronguser", "wrongpass")).thenReturn(Optional.empty());

        String requestJson = """
            {
                "username": "wronguser",
                "password": "wrongpass"
            }
            """;

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Incorrect username or password"));
    }

    // =====================================================================
    // TEST API 2: /api/signup (ĐĂNG KÝ)
    // =====================================================================

    @Test
    @DisplayName("API Signup: Thông tin hợp lệ -> Đăng ký và Gửi Email")
    public void testSignup_Success() throws Exception {
        // THEO LOGIC CỦA BẠN: rq.signup() trả về FALSE là đăng ký thành công!
        Mockito.when(rq.signup(any(User.class))).thenReturn(false);

        String requestJson = """
            {
                "username": "newuser",
                "password": "newpassword123",
                "fullname": "Tân Binh",
                "email": "newuser@gmail.com"
            }
            """;

        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Registration successful"));

        // KIỂM TRA CHÉO: Phải chắc chắn thằng Server đã gọi hàm SendEmail!
        Mockito.verify(emailServer).SendEmail(
                eq("newuser@gmail.com"),
                eq("Account Registration Successful"),
                anyString()
        );
    }

    @Test
    @DisplayName("API Signup: Bị trùng Username/Email -> Trả về 400, KHÔNG gửi Email")
    public void testSignup_Fail_Duplicate() throws Exception {
        // THEO LOGIC CỦA BẠN: rq.signup() trả về TRUE là bị trùng lặp/lỗi!
        Mockito.when(rq.signup(any(User.class))).thenReturn(true);

        String requestJson = """
            {
                "username": "existuser",
                "password": "password123",
                "fullname": "Cựu Binh",
                "email": "exist@gmail.com"
            }
            """;

        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email or Username already exists"));

        // KIỂM TRA CHÉO: Bị lỗi thì cấm được gửi email đi!
        Mockito.verify(emailServer, Mockito.never()).SendEmail(anyString(), anyString(), anyString());
    }
}