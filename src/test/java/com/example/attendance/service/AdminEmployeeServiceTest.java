
package com.example.attendance.service;

import com.example.attendance.entity.User;
import com.example.attendance.exception.ConflictException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminEmployeeServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminEmployeeService adminEmployeeService;

    private User employee;
    private User manager;

    @BeforeEach
    void setUp() {

        employee = new User();
        employee.setId(2L);
        employee.setName("Second Employee");
        employee.setRole("EMPLOYEE");
        employee.setActive(true);

        manager = new User();
        manager.setId(1L);
        manager.setName("Test Employee");
        manager.setRole("MANAGER");
        manager.setActive(true);
    }

    @Test
    void assignManagerShouldSucceed() {

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(employee));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(manager));

        when(userRepository.save(employee))
                .thenReturn(employee);

        User result = adminEmployeeService.assignManager(
                2L,
                1L
        );

        assertEquals(manager, result.getManager());

        verify(userRepository).save(employee);
    }

    @Test
    void assignManagerShouldRejectMissingEmployee() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> adminEmployeeService.assignManager(
                        99L,
                        1L
                )
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void assignManagerShouldRejectInvalidManagerRole() {

        manager.setRole("EMPLOYEE");

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(employee));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(manager));

        assertThrows(
                ConflictException.class,
                () -> adminEmployeeService.assignManager(
                        2L,
                        1L
                )
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void assignManagerShouldRejectInactiveManager() {

        manager.setActive(false);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(employee));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(manager));

        assertThrows(
                ConflictException.class,
                () -> adminEmployeeService.assignManager(
                        2L,
                        1L
                )
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void assignManagerShouldRejectSelfAssignment() {

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(employee));

        assertThrows(
                ConflictException.class,
                () -> adminEmployeeService.assignManager(
                        2L,
                        2L
                )
        );

        verify(userRepository, never()).save(any(User.class));
    }
}
