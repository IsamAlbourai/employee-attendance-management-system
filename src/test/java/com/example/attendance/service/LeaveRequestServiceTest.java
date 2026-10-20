
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private LeaveBalanceInitializationService initializationService;

    @Mock
    private LeaveBalanceValidationService validationService;

    private LeaveRequestService leaveRequestService;

    private User employee;
    private User manager;
    private CreateLeaveRequest request;
    private Authentication authentication;

    @BeforeEach
    void setUp() {

        leaveRequestService = new LeaveRequestService(
                leaveRequestRepository,
                userRepository,
                emailNotificationService,
                initializationService,
                validationService,
                new LeaveDayCalculator()
        );

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

        // Always select a future Monday to keep tests
        // independent of the day they are executed.
        LocalDate monday = LocalDate.now()
                .plusWeeks(2)
                .with(TemporalAdjusters.nextOrSame(
                        DayOfWeek.MONDAY
                ));

        request = new CreateLeaveRequest();
        request.setStartDate(monday);
        request.setEndDate(monday.plusDays(4));
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

        mockAuthenticatedEmployee();

        when(leaveRequestRepository
                .existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(2L),
                        anyList(),
                        eq(request.getEndDate()),
                        eq(request.getStartDate())
                ))
                .thenReturn(false);

        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> {
                    LeaveRequest saved = invocation.getArgument(0);
                    saved.setId(3L);
                    return saved;
                });

        LeaveRequest result =
                leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                );

        assertEquals(3L, result.getId());
        assertSame(employee, result.getEmployee());
        assertSame(manager, result.getManager());

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

        int year = request.getStartDate().getYear();

        verify(initializationService)
                .initializeEmployeeYear(2L, year);

        verify(validationService)
                .validateRequestedLeave(
                        2L,
                        request.getStartDate(),
                        request.getEndDate()
                );

        InOrder inOrder = inOrder(
                userRepository,
                leaveRequestRepository,
                validationService
        );

        inOrder.verify(userRepository)
                .findByIdForUpdate(2L);

        inOrder.verify(leaveRequestRepository)
                .existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(2L),
                        anyList(),
                        eq(request.getEndDate()),
                        eq(request.getStartDate())
                );

        inOrder.verify(validationService)
                .validateRequestedLeave(
                        2L,
                        request.getStartDate(),
                        request.getEndDate()
                );

        inOrder.verify(leaveRequestRepository)
                .save(any(LeaveRequest.class));

        verify(emailNotificationService)
                .queueEmail(
                        eq("LEAVE_REQUEST_CREATED:3"),
                        eq(manager.getEmail()),
                        eq("New Annual Leave Request - #3"),
                        argThat(body ->
                                body.contains("Second Employee")
                                        && body.contains(employee.getEmail())
                                        && body.contains("Annual vacation")
                                        && body.contains(
                                        request.getStartDate().toString()
                                )
                                        && body.contains(
                                        request.getEndDate().toString()
                                )
                                        && body.contains("PENDING")
                        )
                );
    }

    @Test
    void employeeShouldNotCreateOverlappingLeaveRequest() {

        mockAuthenticatedEmployee();

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

        verifyNoInteractions(initializationService);
        verifyNoInteractions(validationService);
        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void employeeShouldNotRequestLeaveWithoutManager() {

        employee.setManager(null);

        mockAuthenticatedEmployee();

        assertThrows(
                ConflictException.class,
                () -> leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                )
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));

        verifyNoInteractions(validationService);
        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void employeeShouldNotRequestLeaveWithInactiveManager() {

        manager.setActive(false);

        mockAuthenticatedEmployee();

        assertThrows(
                ConflictException.class,
                () -> leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                )
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));

        verifyNoInteractions(validationService);
        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void employeeShouldNotRequestLeaveWithInvalidDates() {

        request.setStartDate(request.getEndDate().plusDays(1));

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                )
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(leaveRequestRepository);
        verifyNoInteractions(emailNotificationService);
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
        assertSame(leaveRequest, result.get(0));

        verify(leaveRequestRepository)
                .findByEmployeeIdOrderByCreatedAtDesc(2L);

        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void employeeShouldNotRequestMoreLeaveThanAvailable() {

        mockAuthenticatedEmployee();

        doThrow(new ConflictException(
                "Insufficient annual leave balance"
        )).when(validationService)
                .validateRequestedLeave(
                        eq(2L),
                        eq(request.getStartDate()),
                        eq(request.getEndDate())
                );

        assertThrows(
                ConflictException.class,
                () -> leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                )
        );

        verify(initializationService)
                .initializeEmployeeYear(
                        2L,
                        request.getStartDate().getYear()
                );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));

        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void employeeShouldNotRequestWeekendOnlyLeave() {

        LocalDate saturday = LocalDate.now()
                .plusWeeks(2)
                .with(TemporalAdjusters.nextOrSame(
                        DayOfWeek.SATURDAY
                ));

        request.setStartDate(saturday);
        request.setEndDate(saturday.plusDays(1));

        assertThrows(
                ConflictException.class,
                () -> leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                )
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(initializationService);
        verifyNoInteractions(validationService);

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));

        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void employeeShouldInitializeBalancesForBothYears() {

        int startYear = LocalDate.now().getYear() + 1;

        request.setStartDate(
                LocalDate.of(startYear, 12, 30)
        );

        request.setEndDate(
                LocalDate.of(startYear + 1, 1, 4)
        );

        mockAuthenticatedEmployee();

        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> {
                    LeaveRequest saved = invocation.getArgument(0);
                    saved.setId(10L);
                    return saved;
                });

        LeaveRequest result =
                leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                );

        assertEquals(10L, result.getId());

        verify(initializationService)
                .initializeEmployeeYear(2L, startYear);

        verify(initializationService)
                .initializeEmployeeYear(2L, startYear + 1);

        verify(validationService)
                .validateRequestedLeave(
                        2L,
                        request.getStartDate(),
                        request.getEndDate()
                );

        verify(leaveRequestRepository)
                .save(any(LeaveRequest.class));

        verify(emailNotificationService)
                .queueEmail(
                        eq("LEAVE_REQUEST_CREATED:10"),
                        eq(manager.getEmail()),
                        anyString(),
                        anyString()
                );
    }

    private void mockAuthenticatedEmployee() {

        when(userRepository.findByEmail(employee.getEmail()))
                .thenReturn(Optional.of(employee));

        when(userRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(employee));
    }
}
