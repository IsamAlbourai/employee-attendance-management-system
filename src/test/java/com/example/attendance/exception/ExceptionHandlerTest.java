
package com.example.attendance.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionHandlerTest {

    private GlobalExceptionHandler globalHandler;
    private ApiExceptionHandler apiHandler;

    @BeforeEach
    void setUp() {
        globalHandler = new GlobalExceptionHandler();
        apiHandler = new ApiExceptionHandler();
    }

    @Test
    void invalidArgumentsShouldReturn400() {

        ResponseEntity<Map<String, Object>> response =
                globalHandler.handleIllegalArgument(
                        new IllegalArgumentException(
                                "End date cannot be before start date"
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(400, response.getBody().get("status"));

        assertEquals(
                "End date cannot be before start date",
                response.getBody().get("message")
        );
    }

    @Test
    void conflictsShouldReturn409() {

        ResponseEntity<Map<String, Object>> response =
                apiHandler.handleConflict(
                        new ConflictException(
                                "Employee is already checked in"
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(409, response.getBody().get("status"));
    }

    @Test
    void missingResourcesShouldReturn404() {

        ResponseEntity<Map<String, Object>> response =
                apiHandler.handleResourceNotFound(
                        new ResourceNotFoundException(
                                "Employee not found"
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(404, response.getBody().get("status"));
    }

    @Test
    void authenticationFailuresShouldReturn401() {

        ResponseEntity<Map<String, Object>> response =
                globalHandler.handleAuthenticationException(
                        new BadCredentialsException(
                                "Incorrect password"
                        )
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(401, response.getBody().get("status"));

        assertEquals(
                "Invalid email or password",
                response.getBody().get("message")
        );
    }
}
