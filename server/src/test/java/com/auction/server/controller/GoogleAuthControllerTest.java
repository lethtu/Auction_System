package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.util.SessionManager;
import com.auction.server.view.EmailServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.TestPropertySource;

@WebMvcTest(GoogleAuthController.class)
@TestPropertySource(properties = {"google.client-id=mock", "google.client-secret=mock"})
public class GoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HandleLoginSignup loginSignup;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private EmailServer emailServer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetGoogleConfig() throws Exception {
        mockMvc.perform(get("/api/auth/google/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.mock").value(true));
    }

    @Test
    public void testGoogleLogin_MockUser_ExistingUser() throws Exception {
        User user = new User();
        user.setId(5);
        user.setUsername("googleuser");
        user.setEmail("google@gmail.com");
        user.setBanned(false);

        when(loginSignup.findByUsernameOrEmail("google@gmail.com")).thenReturn(Optional.of(user));
        when(sessionManager.createSession(user)).thenReturn("mock-token");

        Map<String, String> payload = new HashMap<>();
        payload.put("email", "google@gmail.com");
        payload.put("name", "Google User");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.sessionToken").value("mock-token"));
    }

    @Test
    public void testGoogleLogin_MockUser_NewUser() throws Exception {
        when(loginSignup.findByUsernameOrEmail("newgoogle@gmail.com")).thenReturn(Optional.empty());
        when(loginSignup.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });
        when(sessionManager.createSession(any(User.class))).thenReturn("new-token");

        Map<String, String> payload = new HashMap<>();
        payload.put("email", "newgoogle@gmail.com");
        payload.put("name", "New Google User");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.sessionToken").value("new-token"));

        verify(emailServer, times(1)).SendEmail(eq("newgoogle@gmail.com"), anyString(), anyString());
    }

    @Test
    public void testGoogleLogin_MockUser_BannedUser() throws Exception {
        User user = new User();
        user.setId(5);
        user.setUsername("googleuser");
        user.setEmail("banned@gmail.com");
        user.setBanned(true);

        when(loginSignup.findByUsernameOrEmail("banned@gmail.com")).thenReturn(Optional.of(user));

        Map<String, String> payload = new HashMap<>();
        payload.put("email", "banned@gmail.com");
        payload.put("name", "Banned Google User");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Account has been banned"));
    }
}
