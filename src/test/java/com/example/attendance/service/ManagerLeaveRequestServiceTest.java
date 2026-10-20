
package com.example.attendance.service;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerLeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private ManagerLeaveRequestService managerLeaveRequestService;

    private User manager;
    private User employee;
    private LeaveRequest leaveRequest;
    private Authentication authentication;

    @BeforeEach
    void setUp() {

        manager = new User();
        manager.setId(1L);
        manager.setName("Test Manager");
        manager.setEmail("test.employee@example.com");
        manager.setRole("MANAGER");
        manager.setActive(true);

        employee = new User();
        employee.setId(2L);
        employee.setName("Second Employee");
        employee.setEmail("second.employee@example.com");
        employee.setRole("EMPLOYEE");
        employee.setActive(true);

        leaveRequest = new LeaveRequest();
        leaveRequest.setId(1L);
        leaveRequest.setEmployee(employee);
        leaveRequest.setManager(manager);
        leaveRequest.setStartDate(LocalDate.of(2027, 1, 10));
        leaveRequest.setEndDate(LocalDate.of(2027, 1, 15));
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);

        authentication =
                new UsernamePasswordAuthenticationToken(
                        manager.getEmail(),
                        null,
                        List.of(() -> "ROLE_MANAGER")
                );
    }

    @Test
    void managerShouldSeePendingRequests() {

        when(userRepository.findByEmail(manager.getEmail()))
                .thenReturn(Optional.of(manager));

        when(leaveRequestRepository
                .findByManagerIdAndStatusOrderByCreatedAtAsc(
                        1L,
                        LeaveRequestStatus.PENDING
                ))
                .thenReturn(List.of(leaveRequest));

        List<LeaveRequest> results =
                managerLeaveRequestService.getPendingRequests(
                        authentication
                );

        assertEquals(1, results.size());
        assertEquals(leaveRequest, results.get(0));

        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void managerShouldApprovePendingRequest() {

        when(userRepository.findByEmail(manager.getEmail()))
                .thenReturn(Optional.of(manager));

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(leaveRequest));

        when(leaveRequestRepository.save(leaveRequest))
                .thenReturn(leaveRequest);

        LeaveRequest result =
                managerLeaveRequestService.approveRequest(
                        1L,
                        "Approved",
                        authentication
                );

        assertEquals(
                LeaveRequestStatus.APPROVED,
                result.getStatus()
        );

        assertEquals(
                "Approved",
                result.getManagerComment()
        );

        assertNotNull(result.getReviewedAt());

        verify(leaveRequestRepository).save(leaveRequest);

        verify(emailNotificationService).queueEmail(
                eq("LEAVE_REQUEST_APPROVED:1"),
                eq(employee.getEmail()),
                eq("Annual Leave Request APPROVED - #1"),
                argThat(body ->
                        body.contains("Second Employee")
                                && body.contains("APPROVED")
                                && body.contains("Approved")
                                && body.contains("Test Manager")
                                && body.contains("2027-01-10")
                                && body.contains("2027-01-15")
                )
        );
    }

    @Test
    void managerShouldRejectPendingRequest() {

        when(userRepository.findByEmail(manager.getEmail()))
                .thenReturn(Optional.of(manager));

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(leaveRequest));

        when(leaveRequestRepository.save(leaveRequest))
                .thenReturn(leaveRequest);

        LeaveRequest result =
                managerLeaveRequestService.rejectRequest(
                        1L,
                        "Staffing requirements",
                        authentication
                );

        assertEquals(
                LeaveRequestStatus.REJECTED,
                result.getStatus()
        );

        assertEquals(
                "Staffing requirements",
                result.getManagerComment()
        );

        assertNotNull(result.getReviewedAt());

        verify(leaveRequestRepository).save(leaveRequest);

        verify(emailNotificationService).queueEmail(
                eq("LEAVE_REQUEST_REJECTED:1"),
                eq(employee.getEmail()),
                eq("Annual Leave Request REJECTED - #1"),
                argThat(body ->
                        body.contains("Second Employee")
                                && body.contains("REJECTED")
                                && body.contains("Staffing requirements")
                                && body.contains("Test Manager")
                )
        );
    }

    @Test
    void managerShouldNotApproveAlreadyApprovedRequest() {

        leaveRequest.setStatus(LeaveRequestStatus.APPROVED);

        when(userRepository.findByEmail(manager.getEmail()))
                .thenReturn(Optional.of(manager));

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(leaveRequest));

        assertThrows(
                ConflictException.class,
                () -> managerLeaveRequestService.approveRequest(
                        1L,
                        "Approved again",
                        authentication
                )
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));

        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void managerShouldNotReviewAnotherManagersRequest() {

        User anotherManager = new User();
        anotherManager.setId(3L);
        anotherManager.setRole("MANAGER");
        anotherManager.setActive(true);

        leaveRequest.setManager(anotherManager);

        when(userRepository.findByEmail(manager.getEmail()))
                .thenReturn(Optional.of(manager));

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(leaveRequest));

        assertThrows(
                AccessDeniedException.class,
                () -> managerLeaveRequestService.approveRequest(
                        1L,
                        "Unauthorized approval",
                        authentication
                )
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));

        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void inactiveManagerShouldNotReviewRequests() {

        manager.setActive(false);

        when(userRepository.findByEmail(manager.getEmail()))
                .thenReturn(Optional.of(manager));

        assertThrows(
                AccessDeniedException.class,
                () -> managerLeaveRequestService.approveRequest(
                        1L,
                        "Approved",
                        authentication
                )
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));

        verifyNoInteractions(emailNotificationService);
    }
}
