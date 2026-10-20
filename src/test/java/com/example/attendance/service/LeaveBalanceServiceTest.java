
package com.example.attendance.service;

import com.example.attendance.dto.LeaveBalanceResponse;
import com.example.attendance.entity.LeaveBalance;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveRequestStatus;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.LeaveBalanceRepository;
import com.example.attendance.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    private LeaveBalanceService leaveBalanceService;

    private LeaveBalance balance;

    @BeforeEach
    void setUp() {

        LeaveDayCalculator calculator = new LeaveDayCalculator();

        leaveBalanceService = new LeaveBalanceService(
                leaveBalanceRepository,
                leaveRequestRepository,
                calculator
        );

        balance = new LeaveBalance();
        balance.setId(1L);
        balance.setLeaveYear(2027);
        balance.setAnnualAllowance(30);
        balance.setCarriedOverDays(0);
    }

    @Test
    void shouldCalculateApprovedAndPendingLeave() {

        LeaveRequest approved = createRequest(
                LocalDate.of(2027, 3, 1),
                LocalDate.of(2027, 3, 5),
                LeaveRequestStatus.APPROVED
        );

        LeaveRequest pending = createRequest(
                LocalDate.of(2027, 4, 5),
                LocalDate.of(2027, 4, 9),
                LeaveRequestStatus.PENDING
        );

        mockBalanceAndRequests(
                2027,
                List.of(approved, pending)
        );

        LeaveBalanceResponse result =
                leaveBalanceService.getLeaveBalance(2L, 2027);

        assertEquals(30, result.annualAllowance());
        assertEquals(30, result.totalEntitlement());

        assertEquals(5, result.approvedDays());
        assertEquals(5, result.pendingDays());
        assertEquals(20, result.remainingDays());
    }

    @Test
    void shouldIncludeCarriedOverDays() {

        balance.setCarriedOverDays(3);

        mockBalanceAndRequests(2027, List.of());

        LeaveBalanceResponse result =
                leaveBalanceService.getLeaveBalance(2L, 2027);

        assertEquals(30, result.annualAllowance());
        assertEquals(3, result.carriedOverDays());
        assertEquals(33, result.totalEntitlement());
        assertEquals(33, result.remainingDays());
    }

    @Test
    void shouldCountOnlyDaysWithinSelectedYear() {

        LeaveRequest crossYearRequest = createRequest(
                LocalDate.of(2026, 12, 30),
                LocalDate.of(2027, 1, 4),
                LeaveRequestStatus.APPROVED
        );

        mockBalanceAndRequests(
                2027,
                List.of(crossYearRequest)
        );

        LeaveBalanceResponse result =
                leaveBalanceService.getLeaveBalance(2L, 2027);

        assertEquals(2, result.approvedDays());
        assertEquals(0, result.pendingDays());
        assertEquals(28, result.remainingDays());
    }

    @Test
    void shouldReturnFullAllowanceWhenNoLeaveExists() {

        mockBalanceAndRequests(2027, List.of());

        LeaveBalanceResponse result =
                leaveBalanceService.getLeaveBalance(2L, 2027);

        assertEquals(2L, result.employeeId());
        assertEquals(2027, result.leaveYear());

        assertEquals(0, result.approvedDays());
        assertEquals(0, result.pendingDays());
        assertEquals(30, result.remainingDays());
    }

    @Test
    void shouldReturnZeroForWeekendOnlyRequest() {

        LeaveRequest weekendRequest = createRequest(
                LocalDate.of(2027, 1, 9),
                LocalDate.of(2027, 1, 10),
                LeaveRequestStatus.PENDING
        );

        mockBalanceAndRequests(
                2027,
                List.of(weekendRequest)
        );

        LeaveBalanceResponse result =
                leaveBalanceService.getLeaveBalance(2L, 2027);

        assertEquals(0, result.pendingDays());
        assertEquals(30, result.remainingDays());
    }

    @Test
    void shouldRejectInvalidEmployeeIdAndYear() {

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveBalanceService.getLeaveBalance(
                        null, 2027
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveBalanceService.getLeaveBalance(
                        -1L, 2027
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveBalanceService.getLeaveBalance(
                        2L, 1999
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveBalanceService.getLeaveBalance(
                        2L, 2101
                )
        );

        verifyNoInteractions(leaveBalanceRepository);
        verifyNoInteractions(leaveRequestRepository);
    }

    @Test
    void shouldRejectMissingLeaveBalance() {

        when(leaveBalanceRepository
                .findByEmployeeIdAndLeaveYear(2L, 2027))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> leaveBalanceService.getLeaveBalance(
                        2L, 2027
                )
        );

        verifyNoInteractions(leaveRequestRepository);
    }

    private LeaveRequest createRequest(
            LocalDate startDate,
            LocalDate endDate,
            LeaveRequestStatus status
    ) {

        LeaveRequest request = new LeaveRequest();

        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setStatus(status);

        return request;
    }

    private void mockBalanceAndRequests(
            int year,
            List<LeaveRequest> requests
    ) {

        when(leaveBalanceRepository
                .findByEmployeeIdAndLeaveYear(2L, year))
                .thenReturn(Optional.of(balance));

        when(leaveRequestRepository
                .findLeaveRequestsOverlappingYear(
                        eq(2L),
                        eq(List.of(
                                LeaveRequestStatus.APPROVED,
                                LeaveRequestStatus.PENDING
                        )),
                        eq(LocalDate.of(year, 1, 1)),
                        eq(LocalDate.of(year, 12, 31))
                ))
                .thenReturn(requests);
    }
}
