package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.util.SessionManager;
import com.auction.server.view.EmailServer;
import com.auction.server.view.RqForgotPass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthForgotpass.class)
class AuthForgotpassTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthForgotpass authForgotpassController;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private RqForgotPass forgotPass;

    @MockBean
    private EmailServer emailServer;

    @MockBean
    private HandleLoginSignup saveRepo;

    private User mockUser;

    @BeforeEach
    void setup() {
        mockUser = new User();
        mockUser.setFullname("Test User");
        mockUser.setPassword("old_password");
    }

    @Test
    @DisplayName("ForgotPass: existing email sends verification code")
    void testForgotPass_EmailExists() throws Exception {
        Mockito.when(forgotPass.forgot_pass("test@gmail.com")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/api/forgot_pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "test@gmail.com" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Verification code sent"));

        Mockito.verify(emailServer).SendEmail(eq("test@gmail.com"), anyString(), anyString());
    }

    @Test
    void forgotPass_existingOtp_reusesStoredCode() throws Exception {
        Mockito.when(forgotPass.forgot_pass("test@gmail.com")).thenReturn(Optional.of(mockUser));
        Map<String, String> fakeOtpStorage = new ConcurrentHashMap<>();
        fakeOtpStorage.put("test@gmail.com", "ABC123");
        ReflectionTestUtils.setField(authForgotpassController, "otpStorage", fakeOtpStorage);

        mockMvc.perform(post("/api/forgot_pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "test@gmail.com" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emailServer).SendEmail(eq("test@gmail.com"), anyString(), bodyCaptor.capture());
        assertEquals("ABC123", fakeOtpStorage.get("test@gmail.com"));
        assertTrue(bodyCaptor.getValue().contains("ABC123"));
    }

    @Test
    @DisplayName("ForgotPass: missing email returns logic 404")
    void testForgotPass_EmailNotFound() throws Exception {
        Mockito.when(forgotPass.forgot_pass("wrong@gmail.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/forgot_pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "wrong@gmail.com" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No account is associated with this email"));
    }

    @Test
    @DisplayName("CheckCode: valid OTP saves new password")
    void testCheckCode_Success() throws Exception {
        Mockito.when(forgotPass.forgot_pass("test@gmail.com")).thenReturn(Optional.of(mockUser));
        Map<String, String> fakeOtpStorage = new ConcurrentHashMap<>();
        fakeOtpStorage.put("test@gmail.com", "123456");
        ReflectionTestUtils.setField(authForgotpassController, "otpStorage", fakeOtpStorage);

        mockMvc.perform(post("/api/check_code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@gmail.com",
                                  "code": "123456",
                                  "password": "new_password_123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertTrue(com.auction.server.util.PasswordUtil.checkPassword("new_password_123", mockUser.getPassword()));
        assertFalse(fakeOtpStorage.containsKey("test@gmail.com"));
        Mockito.verify(saveRepo).save(mockUser);
    }

    @Test
    void checkCode_validOtpButUserMissing_returnsLogic404() throws Exception {
        Mockito.when(forgotPass.forgot_pass("test@gmail.com")).thenReturn(Optional.empty());
        Map<String, String> fakeOtpStorage = new ConcurrentHashMap<>();
        fakeOtpStorage.put("test@gmail.com", "123456");
        ReflectionTestUtils.setField(authForgotpassController, "otpStorage", fakeOtpStorage);

        mockMvc.perform(post("/api/check_code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@gmail.com",
                                  "code": "123456",
                                  "password": "new_password_123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No account is associated with this email"));
    }

    @Test
    @DisplayName("CheckCode: wrong OTP returns logic 400")
    void testCheckCode_WrongCode() throws Exception {
        Mockito.when(forgotPass.forgot_pass("test@gmail.com")).thenReturn(Optional.of(mockUser));
        Map<String, String> fakeOtpStorage = new ConcurrentHashMap<>();
        fakeOtpStorage.put("test@gmail.com", "123456");
        ReflectionTestUtils.setField(authForgotpassController, "otpStorage", fakeOtpStorage);

        mockMvc.perform(post("/api/check_code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@gmail.com",
                                  "code": "999999",
                                  "password": "new_password_123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid or expired code, please try again"));
    }

    @Test
    @DisplayName("CheckCode: missing OTP returns logic 404")
    void testCheckCode_ExpiredCode() throws Exception {
        mockMvc.perform(post("/api/check_code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@gmail.com",
                                  "code": "123456",
                                  "password": "new_password_123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No account is associated with this email"));
    }
}