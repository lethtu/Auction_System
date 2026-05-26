package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import com.auction.server.util.SessionManager;
import com.auction.server.view.EmailServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleAuthController.class)
@TestPropertySource(properties = {"google.client-id=mock", "google.client-secret=mock"})
public class GoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GoogleAuthController googleAuthController;

    @MockBean
    private HandleLoginSignup loginSignup;

    @MockBean
    private SessionManager sessionManager;

    @MockBean
    private EmailServer emailServer;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void resetGoogleConfig() {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "mock");
        ReflectionTestUtils.setField(googleAuthController, "clientSecret", "mock");
    }

    @Test
    public void testGetGoogleConfig() throws Exception {
        mockMvc.perform(get("/api/auth/google/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.mock").value(true));
    }

    @Test
    public void testGetGoogleConfig_BlankClientId_TreatsAsMock() throws Exception {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "   ");

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
    public void testGoogleLogin_MockUser_DefaultEmailAndName() throws Exception {
        when(loginSignup.findByUsernameOrEmail("mockuser@gmail.com")).thenReturn(Optional.empty());
        when(loginSignup.findByUsernameOrEmail("mockuser")).thenReturn(Optional.empty());
        when(loginSignup.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(11);
            return saved;
        });
        when(sessionManager.createSession(any(User.class))).thenReturn("default-token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.sessionToken").value("default-token"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(loginSignup).save(userCaptor.capture());
        assertEquals("mockuser", userCaptor.getValue().getUsername());
        assertEquals("Mock Google User", userCaptor.getValue().getFullname());
    }

    @Test
    public void testGoogleLogin_MockUser_UsernameConflict_AddsSuffix() throws Exception {
        User existingUsername = new User();
        existingUsername.setUsername("john");

        when(loginSignup.findByUsernameOrEmail("john@gmail.com")).thenReturn(Optional.empty());
        when(loginSignup.findByUsernameOrEmail("john")).thenReturn(Optional.of(existingUsername));
        when(loginSignup.findByUsernameOrEmail("john1")).thenReturn(Optional.empty());
        when(loginSignup.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(12);
            return saved;
        });
        when(sessionManager.createSession(any(User.class))).thenReturn("suffix-token");

        Map<String, String> payload = new HashMap<>();
        payload.put("email", "john@gmail.com");
        payload.put("name", "John Google");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionToken").value("suffix-token"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(loginSignup).save(userCaptor.capture());
        assertEquals("john1", userCaptor.getValue().getUsername());
    }

    @Test
    public void testGoogleLogin_MockUser_EmailNotificationFailure_StillLoginSuccessful() throws Exception {
        when(loginSignup.findByUsernameOrEmail("notifyfail@gmail.com")).thenReturn(Optional.empty());
        when(loginSignup.findByUsernameOrEmail("notifyfail")).thenReturn(Optional.empty());
        when(loginSignup.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(13);
            return saved;
        });
        when(sessionManager.createSession(any(User.class))).thenReturn("notify-token");
        doThrow(new RuntimeException("smtp failed")).when(emailServer)
                .SendEmail(eq("notifyfail@gmail.com"), anyString(), anyString());

        Map<String, String> payload = new HashMap<>();
        payload.put("email", "notifyfail@gmail.com");
        payload.put("name", "Notify Fail");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.sessionToken").value("notify-token"));
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

    @Test
    public void testGoogleLogin_RealConfig_MissingCodeOrRedirectUri() throws Exception {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "real-client-id");
        ReflectionTestUtils.setField(googleAuthController, "clientSecret", "real-client-secret");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing authorization code or redirect URI"));
    }


    @Test
    public void testGetGoogleConfig_RealClientId_NotMock() throws Exception {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "real-client-id");

        mockMvc.perform(get("/api/auth/google/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.mock").value(false));
    }

    @Test
    public void testGoogleLogin_RealConfig_TokenExchangeFails() throws Exception {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "real-client-id");
        ReflectionTestUtils.setField(googleAuthController, "clientSecret", "real-client-secret");
        installGoogleHttpResponses(googleHttpResponse(401, "{}"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "bad-code",
                                "redirectUri", "http://localhost/callback"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Failed to exchange Google authorization code"));
    }

    @Test
    public void testGoogleLogin_RealConfig_TokenResponseMissingAccessToken() throws Exception {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "real-client-id");
        ReflectionTestUtils.setField(googleAuthController, "clientSecret", "real-client-secret");
        installGoogleHttpResponses(googleHttpResponse(200, "{}"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "code-without-token",
                                "redirectUri", "http://localhost/callback"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Failed to retrieve access token from Google"));
    }

    @Test
    public void testGoogleLogin_RealConfig_ProfileFetchFails() throws Exception {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "real-client-id");
        ReflectionTestUtils.setField(googleAuthController, "clientSecret", "real-client-secret");
        installGoogleHttpResponses(
                googleHttpResponse(200, "{\"access_token\":\"token-123\"}"),
                googleHttpResponse(503, "{}")
        );

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "good-code",
                                "redirectUri", "http://localhost/callback"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Failed to retrieve user profile from Google"));
    }

    @Test
    public void testGoogleLogin_RealConfig_ProfileWithoutEmailRejected() throws Exception {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "real-client-id");
        ReflectionTestUtils.setField(googleAuthController, "clientSecret", "real-client-secret");
        installGoogleHttpResponses(
                googleHttpResponse(200, "{\"access_token\":\"token-123\"}"),
                googleHttpResponse(200, "{\"name\":\"No Email\"}")
        );

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "good-code",
                                "redirectUri", "http://localhost/callback"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email not provided by Google"));
    }

    @Test
    public void testGoogleLogin_RealConfig_ExistingUserSuccess() throws Exception {
        ReflectionTestUtils.setField(googleAuthController, "clientId", "real-client-id");
        ReflectionTestUtils.setField(googleAuthController, "clientSecret", "real-client-secret");
        installGoogleHttpResponses(
                googleHttpResponse(200, "{\"access_token\":\"token-123\"}"),
                googleHttpResponse(200, "{\"email\":\"real@gmail.com\",\"name\":\"Real User\"}")
        );

        User user = new User();
        user.setId(21);
        user.setUsername("realuser");
        user.setEmail("real@gmail.com");
        user.setBanned(false);

        when(loginSignup.findByUsernameOrEmail("real@gmail.com")).thenReturn(Optional.of(user));
        when(sessionManager.createSession(user)).thenReturn("real-token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "good-code",
                                "redirectUri", "http://localhost/callback"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.sessionToken").value("real-token"));
    }

    @SafeVarargs
    private final void installGoogleHttpResponses(java.net.http.HttpResponse<String>... responses) throws Exception {
        java.util.Queue<java.net.http.HttpResponse<String>> queue = new java.util.ArrayDeque<>(java.util.Arrays.asList(responses));
        java.net.http.HttpClient httpClient = org.mockito.Mockito.mock(java.net.http.HttpClient.class);
        when(httpClient.send(
                any(java.net.http.HttpRequest.class),
                org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any()
        )).thenAnswer(invocation -> queue.remove());
        ReflectionTestUtils.setField(googleAuthController, "httpClient", httpClient);
    }

    @SuppressWarnings("unchecked")
    private java.net.http.HttpResponse<String> googleHttpResponse(int status, String body) {
        java.net.http.HttpResponse<String> response =
                (java.net.http.HttpResponse<String>) org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
