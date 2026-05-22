package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.model.User;
import com.auction.server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService);
    }

    @Test
    void getAllUsers_returnsSuccessResponse() {
        User user = new User();
        user.setId(1);
        user.setUsername("bidder01");
        when(userService.getAllUsers()).thenReturn(List.of(user));

        ResponseEntity<ApiResponse<List<User>>> response = controller.getAllUsers();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getStatus());
        assertEquals("Success", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void getUserById_missingUser_returnsNotFound() {
        when(userService.getUserById(99)).thenReturn(null);

        ResponseEntity<ApiResponse<User>> response = controller.getUserById(99);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    void updateProfile_success_returnsUpdatedUser() {
        User updated = new User();
        updated.setId(7);
        updated.setUsername("newname");
        Map<String, String> request = Map.of("username", "newname");
        when(userService.updateProfile(7, request)).thenReturn(updated);

        ResponseEntity<ApiResponse<User>> response = controller.updateProfile(7, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Account information updated successfully", response.getBody().getMessage());
        assertSame(updated, response.getBody().getData());
    }

    @Test
    void updateProfile_invalidRequest_returnsBadRequest() {
        Map<String, String> request = Map.of("email", "bad");
        when(userService.updateProfile(7, request)).thenThrow(new IllegalArgumentException("Invalid email"));

        ResponseEntity<ApiResponse<User>> response = controller.updateProfile(7, request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Invalid email", response.getBody().getMessage());
    }

    @Test
    void uploadAvatar_missingUser_returnsNotFoundBeforeFileValidation() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar", "avatar.png", "image/png", new byte[] {1, 2, 3});
        when(userService.getUserById(404)).thenReturn(null);

        ResponseEntity<ApiResponse<Map<String, String>>> response = controller.uploadAvatar(404, file);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
        verify(userService, never()).updateAvatarUrl(any(), any());
    }

    @Test
    void uploadAvatar_invalidExtension_returnsBadRequest() {
        User user = new User();
        user.setId(3);
        MockMultipartFile file = new MockMultipartFile(
                "avatar", "avatar.txt", "image/png", new byte[] {1, 2, 3});
        when(userService.getUserById(3)).thenReturn(user);

        ResponseEntity<ApiResponse<Map<String, String>>> response = controller.uploadAvatar(3, file);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Invalid file. Please select a PNG, JPG, JPEG or WEBP image under 5MB.", response.getBody().getMessage());
        verify(userService, never()).updateAvatarUrl(any(), any());
    }
}