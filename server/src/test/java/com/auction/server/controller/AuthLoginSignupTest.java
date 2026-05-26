package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.util.SessionManager;
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

@WebMvcTest(AuthLoginSignup.class)
public class AuthLoginSignupTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private RqLoginSignup rq;

    @MockBean
    private EmailServer emailServer;

    private User mockUser;

    @BeforeEach
    public void setup() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");
        mockUser.setPassword("matkhau123");
        mockUser.setFullname("Test User");
        mockUser.setEmail("test@gmail.com");
    }

    @Test
    @DisplayName("API Login: valid credentials return 200")
    public void testLogin_Success() throws Exception {
        Mockito.when(rq.login("testuser", "matkhau123")).thenReturn(Optional.of(mockUser));
        Mockito.when(sessionManager.createSession(mockUser)).thenReturn("token-123");

        String requestJson = """
            {
                "username": "testuser",
                "password": "matkhau123"
            }
            """;

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"));

        Mockito.verify(sessionManager).createSession(mockUser);
    }

    @Test
    @DisplayName("API Login: invalid credentials return 401")
    public void testLogin_Fail() throws Exception {
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

    @Test
    @DisplayName("API Login: banned account return 401 and no session")
    public void testLogin_BannedAccount() throws Exception {
        mockUser.setBanned(true);
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Account has been banned"));

        Mockito.verify(sessionManager, Mockito.never()).createSession(any(User.class));
    }

    @Test
    @DisplayName("API Signup: valid info registers and sends email")
    public void testSignup_Success() throws Exception {
        User successfulSavedUser = new User();
        successfulSavedUser.setId(1);
        successfulSavedUser.setUsername("newuser");
        successfulSavedUser.setPassword("newpassword123");
        successfulSavedUser.setFullname("New User");
        successfulSavedUser.setEmail("newuser@gmail.com");

        Mockito.when(rq.signup(any(User.class))).thenReturn(Optional.of(successfulSavedUser));

        String requestJson = """
            {
                "username": "newuser",
                "password": "newpassword123",
                "fullname": "New User",
                "email": "newuser@gmail.com"
            }
            """;

        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Registration successful"));

        Mockito.verify(emailServer).SendEmail(
                eq("newuser@gmail.com"),
                eq("Account Registration Successful"),
                anyString()
        );
    }

    @Test
    @DisplayName("API Signup: duplicate username or email returns 400 and no email")
    public void testSignup_Fail_Duplicate() throws Exception {
        Mockito.when(rq.signup(any(User.class))).thenReturn(Optional.empty());

        String requestJson = """
            {
                "username": "existuser",
                "password": "password123",
                "fullname": "Old User",
                "email": "exist@gmail.com"
            }
            """;

        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email or Username already exists"));

        Mockito.verify(emailServer, Mockito.never()).SendEmail(anyString(), anyString(), anyString());
    }
}
