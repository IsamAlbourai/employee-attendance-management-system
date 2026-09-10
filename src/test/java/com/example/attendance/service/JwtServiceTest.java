package com.example.attendance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {

        jwtService =
                new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secretKey",
                "VGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS10aGF0LWlzLWxvbmc="
        );

        ReflectionTestUtils.setField(
                jwtService,
                "jwtExpiration",
                86400000L
        );

        userDetails =
                new User(
                        "second.employee@example.com",
                        "hashed-password",
                        Collections.emptyList()
                );
    }

    @Test
    void generateTokenShouldCreateToken() {

        String token =
                jwtService.generateToken(
                        userDetails
                );

        assertNotNull(token);

        assertFalse(
                token.isBlank()
        );
    }

    @Test
    void extractUsernameShouldReturnEmail() {

        String token =
                jwtService.generateToken(
                        userDetails
                );

        String username =
                jwtService.extractUsername(
                        token
                );

        assertEquals(
                "second.employee@example.com",
                username
        );
    }

    @Test
    void validTokenShouldBeAccepted() {

        String token =
                jwtService.generateToken(
                        userDetails
                );

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        userDetails
                );

        assertTrue(valid);
    }

    @Test
    void tokenShouldBeRejectedForDifferentUser() {

        String token =
                jwtService.generateToken(
                        userDetails
                );

        UserDetails differentUser =
                new User(
                        "admin@example.com",
                        "hashed-password",
                        Collections.emptyList()
                );

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        differentUser
                );

        assertFalse(valid);
    }

    @Test
    void tokenShouldBeRejectedForDisabledUser() {

        UserDetails activeUser =
                User.withUsername(
                                "second.employee@example.com"
                        )
                        .password(
                                "hashed-password"
                        )
                        .authorities(
                                Collections.emptyList()
                        )
                        .build();

        String token =
                jwtService.generateToken(
                        activeUser
                );

        UserDetails disabledUser =
                User.withUsername(
                                "second.employee@example.com"
                        )
                        .password(
                                "hashed-password"
                        )
                        .authorities(
                                Collections.emptyList()
                        )
                        .disabled(true)
                        .build();

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        disabledUser
                );

        assertFalse(valid);
    }
}