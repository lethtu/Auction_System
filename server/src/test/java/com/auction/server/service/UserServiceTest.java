package com.auction.server.service;

import com.auction.server.model.User;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsers_returnsRepositoryResult() {
        User user = new User();
        user.setId(1);
        user.setUsername("minh");
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertSame(user, result.get(0));
    }

    @Test
    void updateProfile_validData_trimsFieldsAndSaves() {
        User user = new User();
        user.setId(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        Map<String, String> request = Map.of(
                "username", "  newuser  ",
                "fullname", "  Nguyen Van A  ",
                "email", "  new@example.com  ",
                "dob", "  2000-01-01  ",
                "place_of_birth", "  Ha Noi  "
        );

        User result = userService.updateProfile(10, request);

        assertSame(user, result);
        assertEquals("newuser", user.getUsername());
        assertEquals("Nguyen Van A", user.getFullname());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("2000-01-01", user.getDob());
        assertEquals("Ha Noi", user.getPlaceOfBirth());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_duplicateUsername_throwsAndDoesNotSave() {
        User current = new User();
        current.setId(1);
        User duplicate = new User();
        duplicate.setId(2);

        when(userRepository.findById(1)).thenReturn(Optional.of(current));
        when(userRepository.findByUsername("duplicate")).thenReturn(Optional.of(duplicate));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.updateProfile(1, Map.of(
                        "username", "duplicate",
                        "fullname", "Tester",
                        "email", "tester@example.com"
                )));

        assertEquals("Username is already taken", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_invalidEmail_throwsBeforeDuplicateChecks() {
        User user = new User();
        user.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.updateProfile(1, Map.of(
                        "username", "tester",
                        "fullname", "Tester",
                        "email", "invalid-email"
                )));

        assertEquals("Invalid email", ex.getMessage());
        verify(userRepository, never()).findByUsername("tester");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAvatarUrl_validUser_updatesAndSaves() {
        User user = new User();
        user.setId(5);
        when(userRepository.findById(5)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateAvatarUrl(5, "/api/files/avatar/a.png");

        assertSame(user, result);
        assertEquals("/api/files/avatar/a.png", user.getAvatarUrl());
        verify(userRepository).save(user);
    }

    @Test
    void updateAvatarUrl_invalidId_throwsBeforeRepositoryAccess() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateAvatarUrl(0, "/avatar.png"));

        assertEquals("Invalid userId", ex.getMessage());
        verifyNoInteractions(userRepository);
    }
}