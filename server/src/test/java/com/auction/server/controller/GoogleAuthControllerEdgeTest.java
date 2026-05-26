package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.util.SessionManager;
import com.auction.server.view.EmailServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleAuthController.class)
@TestPropertySource(properties = {"google.client-id=mock", "google.client-secret=mock"})
class GoogleAuthControllerEdgeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoogleAuthController googleAuthController;

    @MockBean
    private HandleLoginSignup loginSignup;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private EmailServer emailServer;

    @BeforeEach
    void forceMockGoogleConfig() {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "mock");
        ReflectionTestUtils.setField(googleAuthController, "clientSecret", "mock");
    }

    @Test
    void googleLogin_mockModeDefaultsEmailAndNameWhenPayloadBlank() throws Exception {
        when(loginSignup.findByUsernameOrEmail(anyString())).thenReturn(Optional.empty());
        when(loginSignup.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(21);
            return saved;
        });
        when(sessionManager.createSession(any(User.class))).thenReturn("default-token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<String, String>())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.email").value("mockuser@gmail.com"))
                .andExpect(jsonPath("$.data.fullname").value("Mock Google User"))
                .andExpect(jsonPath("$.data.sessionToken").value("default-token"));

        verify(emailServer, times(1)).SendEmail(eq("mockuser@gmail.com"), anyString(), anyString());
    }

    @Test
    void googleLogin_newUserUsesNextUsernameWhenBaseUsernameExists() throws Exception {
        User existing = new User();
        existing.setId(9);
        existing.setUsername("newconflict");
        existing.setEmail("other@gmail.com");

        when(loginSignup.findByUsernameOrEmail(anyString())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            if ("newconflict".equals(value)) {
                return Optional.of(existing);
            }
            return Optional.empty();
        });
        when(loginSignup.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(22);
            return saved;
        });
        when(sessionManager.createSession(any(User.class))).thenReturn("conflict-token");

        Map<String, String> payload = new HashMap<>();
        payload.put("email", "newconflict@gmail.com");
        payload.put("name", "Conflict User");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(22))
                .andExpect(jsonPath("$.data.username").value("newconflict1"))
                .andExpect(jsonPath("$.data.email").value("newconflict@gmail.com"))
                .andExpect(jsonPath("$.data.sessionToken").value("conflict-token"));
    }
}
