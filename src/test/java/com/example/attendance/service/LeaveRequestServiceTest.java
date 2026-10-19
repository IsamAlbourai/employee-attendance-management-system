
package com.example.attendance.service;

import com.example.attendance.dto.CreateLeaveRequest;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveRequestStatus;
import com.example.attendance.entity.User;
import com.example.attendance.exception.ConflictException;
import com.example.attendance.repository.LeaveRequestRepository;
import com.example.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private User employee;
    private User manager;
    private CreateLeaveRequest request;
    private Authentication authentication;

    @BeforeEach
    void setUp() {

        manager = new User();
        manager.setId(1L);
        manager.setName("Test Employee");
        manager.setEmail("test.employee@example.com");
        manager.setRole("MANAGER");
        manager.setActive(true);

        employee = new User();
        employee.setId(2L);
        employee.setName("Second Employee");
        employee.setEmail("second.employee@example.com");
        employee.setRole("EMPLOYEE");
        employee.setActive(true);
        employee.setManager(manager);

        request = new CreateLeaveRequest();
        request.setStartDate(LocalDate.now().plusDays(10));
        request.setEndDate(LocalDate.now().plusDays(15));
        request.setReason("Annual vacation");

        authentication =
                new UsernamePasswordAuthenticationToken(
                        employee.getEmail(),
                        null,
                        List.of(() -> "ROLE_EMPLOYEE")
                );
    }

    @Test
    void employeeShouldCreateLeaveRequest() {

        when(userRepository.findByEmail(employee.getEmail()))
                .thenReturn(Optional.of(employee));

        when(leaveRequestRepository
                .existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(2L),
                        anyList(),
                        eq(request.getEndDate()),
                        eq(request.getStartDate())
                ))
                .thenReturn(false);

        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequest result =
                leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                );

        assertEquals(employee, result.getEmployee());
        assertEquals(manager, result.getManager());

        assertEquals(
                LeaveRequestStatus.PENDING,
                result.getStatus()
        );

        assertEquals(
                request.getStartDate(),
                result.getStartDate()
        );

        assertEquals(
                request.getEndDate(),
                result.getEndDate()
        );

        assertEquals(
                "Annual vacation",
                result.getReason()
        );

        verify(leaveRequestRepository).save(
                any(LeaveRequest.class)
        );
    }

    @Test
    void employeeShouldNotCreateOverlappingLeaveRequest() {

        when(userRepository.findByEmail(employee.getEmail()))
                .thenReturn(Optional.of(employee));

        when(leaveRequestRepository
                .existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(2L),
                        anyList(),
                        eq(request.getEndDate()),
                        eq(request.getStartDate())
                ))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                )
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));
    }

    @Test
    void employeeShouldNotRequestLeaveWithoutManager() {

        employee.setManager(null);

        when(userRepository.findByEmail(employee.getEmail()))
                .thenReturn(Optional.of(employee));

        assertThrows(
                ConflictException.class,
                () -> leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                )
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));
    }

    @Test
    void employeeShouldNotRequestLeaveWithInactiveManager() {

        manager.setActive(false);

        when(userRepository.findByEmail(employee.getEmail()))
                .thenReturn(Optional.of(employee));

        assertThrows(
                ConflictException.class,
                () -> leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                )
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));
    }

    @Test
    void employeeShouldNotRequestLeaveWithInvalidDates() {

        request.setStartDate(LocalDate.now().plusDays(15));
        request.setEndDate(LocalDate.now().plusDays(10));

        when(userRepository.findByEmail(employee.getEmail()))
                .thenReturn(Optional.of(employee));

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                )
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));
    }

    @Test
    void employeeShouldSeeOwnLeaveHistory() {

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(1L);
        leaveRequest.setEmployee(employee);
        leaveRequest.setManager(manager);
        leaveRequest.setStatus(LeaveRequestStatus.APPROVED);

        when(userRepository.findByEmail(employee.getEmail()))
                .thenReturn(Optional.of(employee));

        when(leaveRequestRepository
                .findByEmployeeIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(leaveRequest));

        List<LeaveRequest> result =
                leaveRequestService.getMyLeaveRequests(
                        authentication
                );

        assertEquals(1, result.size());
        assertEquals(leaveRequest, result.get(0));

        verify(leaveRequestRepository)
                .findByEmployeeIdOrderByCreatedAtDesc(2L);
    }
}
