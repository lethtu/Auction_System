package com.auction.server.util;

import com.auction.server.model.User;
import com.auction.server.repository.HandleLoginSignup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordMigrationRunnerTest {

    @Mock
    private HandleLoginSignup userRepository;

    @InjectMocks
    private PasswordMigrationRunner runner;

    @Test
    public void testRun_EmptyUsers() throws Exception {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        runner.run();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testRun_NoPlaintextPasswords() throws Exception {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("$2a$12$hashedPasswordValue");

        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));
        runner.run();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testRun_WithPlaintextPasswords() throws Exception {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("plaintextPass");

        User user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("$2b$12$alreadyHashed");

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        runner.run();

        // Should save user1 with hashed password, but not user2
        verify(userRepository, times(1)).save(user1);
        verify(userRepository, never()).save(user2);
    }
}
