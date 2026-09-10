package com.example.attendance.service;

import com.example.attendance.dto.LoginRequest;
import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    private AuthService authService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService
        );

        userDetails =
                new org.springframework.security.core.userdetails.User(
                        "second.employee@example.com",
                        "hashed-password",
                        Collections.emptyList()
                );
    }

    @Test
    void loginShouldReturnJwtToken() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "second.employee@example.com"
        );

        request.setPassword(
                "Password123"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("test-jwt-token");

        String token =
                authService.login(request);

        assertNotNull(token);

        assertEquals(
                "test-jwt-token",
                token
        );

        verify(authenticationManager, times(1))
                .authenticate(
                        any(
                                UsernamePasswordAuthenticationToken.class
                        )
                );

        verify(jwtService, times(1))
                .generateToken(userDetails);
    }
}