package com.auction.server.service;

import com.auction.server.model.User;
import com.auction.server.repository.UserRepository;
import com.auction.server.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    public void testGetAllUsers() {
        List<User> list = Arrays.asList(new User(), new User());
        when(userRepository.findAll()).thenReturn(list);
        List<User> result = userService.getAllUsers();
        assertEquals(2, result.size());
    }

    @Test
    public void testGetUserById() {
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        assertEquals(user, userService.getUserById(1));
        assertNull(userService.getUserById(2));
    }

    @Test
    public void testUpdateProfile_InvalidId() {
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(null, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(0, new HashMap<>()));
    }

    @Test
    public void testUpdateProfile_UserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(1, new HashMap<>()));
    }

    @Test
    public void testUpdateProfile_MissingRequiredFields() {
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Map<String, String> request = new HashMap<>();
        // missing username
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(1, request));

        request.put("username", "user1");
        // missing fullname
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(1, request));

        request.put("fullname", "Full Name");
        // missing email
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(1, request));

        request.put("email", "invalid-email");
        // invalid email format
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(1, request));
    }

    @Test
    public void testUpdateProfile_UsernameOrEmailTaken() {
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Map<String, String> request = new HashMap<>();
        request.put("username", "takenUser");
        request.put("fullname", "Full Name");
        request.put("email", "test@test.com");

        User existingUser = new User();
        existingUser.setId(2);
        existingUser.setUsername("takenUser");

        when(userRepository.findByUsername("takenUser")).thenReturn(Optional.of(existingUser));
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(1, request));

        when(userRepository.findByUsername("takenUser")).thenReturn(Optional.empty());
        User existingUser2 = new User();
        existingUser2.setId(2);
        existingUser2.setEmail("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(existingUser2));
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(1, request));
    }

    @Test
    public void testUpdateProfile_Success() {
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        Map<String, String> request = new HashMap<>();
        request.put("username", "newUser");
        request.put("fullname", "New Name");
        request.put("email", "new@test.com");
        request.put("dob", "2000-01-01");
        request.put("placeOfBirth", "Hanoi");

        User result = userService.updateProfile(1, request);
        assertEquals("newUser", result.getUsername());
        assertEquals("New Name", result.getFullname());
        assertEquals("new@test.com", result.getEmail());
        assertEquals("2000-01-01", result.getDob());
        assertEquals("Hanoi", result.getPlaceOfBirth());
    }

    @Test
    public void testUpdateAvatarUrl() {
        assertThrows(IllegalArgumentException.class, () -> userService.updateAvatarUrl(null, "url"));
        assertThrows(IllegalArgumentException.class, () -> userService.updateAvatarUrl(-1, "url"));

        when(userRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.updateAvatarUrl(1, "url"));

        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User result = userService.updateAvatarUrl(1, "new_avatar");
        assertEquals("new_avatar", result.getAvatarUrl());
    }

    @Test
    public void testSetPassword() {
        assertThrows(IllegalArgumentException.class, () -> userService.setPassword(null, "pass"));
        assertThrows(IllegalArgumentException.class, () -> userService.setPassword(0, "pass"));

        when(userRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.setPassword(1, "pass"));

        User user = new User();
        user.setId(1);
        user.setPasswordSet(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        // Already set
        assertThrows(IllegalArgumentException.class, () -> userService.setPassword(1, "pass"));

        user.setPasswordSet(false);
        // Short password
        assertThrows(IllegalArgumentException.class, () -> userService.setPassword(1, "123"));

        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        User result = userService.setPassword(1, "newPassword123");
        assertTrue(result.getPasswordSet());
        assertNotNull(result.getPassword());
    }

    @Test
    public void testChangePassword() {
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(null, "old", "new"));
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(0, "old", "new"));

        when(userRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1, "old", "new"));

        User user = new User();
        user.setId(1);
        user.setPasswordSet(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        // Password not set yet
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1, "old", "new"));

        user.setPasswordSet(true);
        user.setPassword(PasswordUtil.hashPassword("correctOld"));
        // Empty old password
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1, "", "newPassword"));
        // Incorrect old password
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1, "wrongOld", "newPassword"));
        // Short new password
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1, "correctOld", "123"));

        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        User result = userService.changePassword(1, "correctOld", "newPassword123");
        assertTrue(PasswordUtil.checkPassword("newPassword123", result.getPassword()));
    }
}
