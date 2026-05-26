package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.service.CloudinaryService;
import com.auction.server.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.auction.server.util.SessionManager;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private CloudinaryService cloudinaryService;

    @MockBean
    private SessionManager sessionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @org.junit.jupiter.api.BeforeEach
    public void setUp() {
        when(sessionManager.getSession(any())).thenReturn(new SessionManager.SessionUser(1, "admin", "admin"));
    }

    @Test
    public void testGetAllUsers() throws Exception {
        User user1 = new User();
        user1.setId(1);
        user1.setUsername("u1");

        User user2 = new User();
        user2.setId(2);
        user2.setUsername("u2");

        when(userService.getAllUsers()).thenReturn(Arrays.asList(user1, user2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].username").value("u1"))
                .andExpect(jsonPath("$.data[1].username").value("u2"));
    }

    @Test
    public void testGetUserById() throws Exception {
        User user = new User();
        user.setId(1);
        user.setUsername("u1");

        when(userService.getUserById(1)).thenReturn(user);
        when(userService.getUserById(2)).thenReturn(null);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.username").value("u1"));

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    public void testUpdateProfile() throws Exception {
        User user = new User();
        user.setId(1);
        user.setUsername("newU1");

        Map<String, String> requestMap = Map.of("username", "newU1");

        when(userService.updateProfile(eq(1), anyMap())).thenReturn(user);
        when(userService.updateProfile(eq(2), anyMap())).thenThrow(new IllegalArgumentException("Failed"));

        mockMvc.perform(put("/api/users/1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMap)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.username").value("newU1"));

        mockMvc.perform(put("/api/users/2/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMap)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Failed"));
    }

    @Test
    public void testUploadAvatar_UserNotFound() throws Exception {
        when(userService.getUserById(1)).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    public void testUploadAvatar_EmptyFile() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);

        MockMultipartFile emptyFile = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/users/1/avatar").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Image file is empty."));
    }

    @Test
    public void testUploadAvatar_ExceedLimit() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);

        // 6MB file
        byte[] largeBytes = new byte[6 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile("avatar", "avatar.png", "image/png", largeBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(largeFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Image file exceeds the 5MB limit."));
    }

    @Test
    public void testUploadAvatar_InvalidExtension() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);

        MockMultipartFile badFile = new MockMultipartFile("avatar", "avatar.txt", "text/plain", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/users/1/avatar").file(badFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Please select a PNG, JPG, JPEG or WEBP image")));
    }

    @Test
    public void testUploadAvatar_InvalidContent() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);

        // Content type mismatch, and magic bytes do not match png/jpg
        MockMultipartFile badContentFile = new MockMultipartFile("avatar", "avatar.png", "text/plain", new byte[]{1, 2, 3, 4});

        mockMvc.perform(multipart("/api/users/1/avatar").file(badContentFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    public void testUploadAvatar_LocalUploadSuccess() throws Exception {
        User user = new User();
        user.setId(1);
        user.setAvatarUrl("/api/files/avatar/oldAvatar.png");
        when(userService.getUserById(1)).thenReturn(user);
        when(cloudinaryService.isConfigured()).thenReturn(false);

        // Valid png header: 89 50 4E 47
        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("avatar", "newAvatar.png", "image/png", validPngBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.avatarUrl").exists());
    }

    @Test
    public void testUploadAvatar_CloudinarySuccess() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);
        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.uploadFileWithPublicId(any(), anyString(), anyString(), anyBoolean()))
                .thenReturn("http://cloudinary.url/avatar.png");

        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("avatar", "newAvatar.png", "image/png", validPngBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.avatarUrl").value("http://cloudinary.url/avatar.png"));
    }

    @Test
    public void testSetPassword() throws Exception {
        User user = new User();
        user.setId(1);
        user.setUsername("u1");

        when(userService.setPassword(eq(1), anyString())).thenReturn(user);
        when(userService.setPassword(eq(2), anyString())).thenThrow(new IllegalArgumentException("Failed"));

        Map<String, String> request = Map.of("password", "newPass123");

        mockMvc.perform(post("/api/users/1/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(post("/api/users/2/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    public void testChangePassword() throws Exception {
        User user = new User();
        user.setId(1);
        user.setUsername("u1");

        when(userService.changePassword(eq(1), anyString(), anyString())).thenReturn(user);
        when(userService.changePassword(eq(2), anyString(), anyString())).thenThrow(new IllegalArgumentException("Failed"));

        Map<String, String> request = Map.of("oldPassword", "oldPass", "newPassword", "newPass");

        mockMvc.perform(post("/api/users/1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(post("/api/users/2/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
    @Test
    public void testUploadAvatar_OctetStreamWithPngMagicBytesSuccess() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);
        when(cloudinaryService.isConfigured()).thenReturn(false);

        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "application/octet-stream", validPngBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.avatarUrl", org.hamcrest.Matchers.startsWith("/api/files/avatar/")));

        verify(userService).updateAvatarUrl(eq(1), anyString());
    }

    @Test
    public void testUploadAvatar_CloudinaryFailureFallsBackToLocalStorage() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);
        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.uploadFileWithPublicId(any(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("cloudinary down"));

        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", validPngBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.avatarUrl", org.hamcrest.Matchers.startsWith("/api/files/avatar/")));

        verify(cloudinaryService).uploadFileWithPublicId(any(), anyString(), anyString(), anyBoolean());
        verify(userService).updateAvatarUrl(eq(1), anyString());
    }


    @Test
    public void testUploadAvatar_NoExtensionRejected() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);

        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar", "image/png", validPngBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Please select a PNG")));
    }

    @Test
    public void testUploadAvatar_NullOriginalFilenameRejected() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);

        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("avatar", null, "image/png", validPngBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    public void testUploadAvatar_OctetStreamWithJpegMagicBytesSuccess() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);
        when(cloudinaryService.isConfigured()).thenReturn(false);

        byte[] validJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.jpg", "application/octet-stream", validJpegBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.avatarUrl", org.hamcrest.Matchers.startsWith("/api/files/avatar/")));
    }

    @Test
    public void testUploadAvatar_OctetStreamWithWebpMagicBytesSuccess() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);
        when(cloudinaryService.isConfigured()).thenReturn(false);

        byte[] validWebpBytes = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.webp", "application/octet-stream", validWebpBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.avatarUrl", org.hamcrest.Matchers.startsWith("/api/files/avatar/")));
    }

    @Test
    public void testUploadAvatar_InputStreamReadFailureRejected() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);

        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(12L);
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        when(file.getContentType()).thenReturn("application/octet-stream");
        when(file.getInputStream()).thenThrow(new java.io.IOException("cannot read"));

        UserController controller = new UserController(userService, cloudinaryService);
        org.springframework.http.ResponseEntity<?> response = controller.uploadAvatar(1, file);

        org.junit.jupiter.api.Assertions.assertEquals(400, response.getStatusCode().value());
    }

    @Test
    public void testUploadAvatar_TransferToFailureReturnsServerError() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);
        when(cloudinaryService.isConfigured()).thenReturn(false);

        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(12L);
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        when(file.getContentType()).thenReturn("image/png");
        doThrow(new java.io.IOException("disk full")).when(file).transferTo(any(java.nio.file.Path.class));

        UserController controller = new UserController(userService, cloudinaryService);
        org.springframework.http.ResponseEntity<?> response = controller.uploadAvatar(1, file);

        org.junit.jupiter.api.Assertions.assertEquals(500, response.getStatusCode().value());
    }

    @Test
    public void testUploadAvatar_UpdateAvatarIllegalArgumentReturnsBadRequest() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);
        when(cloudinaryService.isConfigured()).thenReturn(false);
        when(userService.updateAvatarUrl(eq(1), anyString())).thenThrow(new IllegalArgumentException("Invalid userId"));

        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", validPngBytes);

        mockMvc.perform(multipart("/api/users/1/avatar").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid userId"));
    }

    @Test
    public void testUploadAvatar_NullCloudinaryServiceUsesLocalStorage() throws Exception {
        User user = new User();
        user.setId(1);
        when(userService.getUserById(1)).thenReturn(user);

        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", validPngBytes);

        UserController controller = new UserController(userService, null);
        org.springframework.http.ResponseEntity<?> response = controller.uploadAvatar(1, file);

        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        verify(userService).updateAvatarUrl(eq(1), anyString());
    }



    @Test
    public void testSetPassword_RuntimeExceptionReturnsServerError() throws Exception {
        when(userService.setPassword(eq(3), anyString())).thenThrow(new RuntimeException("database down"));

        mockMvc.perform(post("/api/users/3/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newPass123"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Failed to set password."));
    }

    @Test
    public void testChangePassword_RuntimeExceptionReturnsServerError() throws Exception {
        when(userService.changePassword(eq(3), anyString(), anyString())).thenThrow(new RuntimeException("database down"));

        mockMvc.perform(post("/api/users/3/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", "oldPass",
                                "newPassword", "newPass"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Failed to change password."));
    }
}
