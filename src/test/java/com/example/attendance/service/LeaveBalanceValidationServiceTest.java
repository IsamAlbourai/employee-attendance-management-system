
package com.example.attendance.service;

import com.example.attendance.dto.LeaveBalanceResponse;
import com.example.attendance.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceValidationServiceTest {

    @Mock
    private LeaveBalanceService leaveBalanceService;

    private LeaveBalanceValidationService validationService;

    @BeforeEach
    void setUp() {

        validationService = new LeaveBalanceValidationService(
                leaveBalanceService,
                new LeaveDayCalculator()
        );
    }

    @Test
    void shouldAllowRequestWithinRemainingBalance() {

        mockBalance(2027, 25);

        assertDoesNotThrow(() ->
                validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(2027, 3, 1),
                        LocalDate.of(2027, 3, 5)
                )
        );

        verify(leaveBalanceService)
                .getLeaveBalance(2L, 2027);
    }

    @Test
    void shouldAllowRequestEqualToRemainingBalance() {

        mockBalance(2027, 5);

        assertDoesNotThrow(() ->
                validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(2027, 3, 1),
                        LocalDate.of(2027, 3, 5)
                )
        );
    }

    @Test
    void shouldRejectRequestExceedingRemainingBalance() {

        mockBalance(2027, 4);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(2027, 3, 1),
                        LocalDate.of(2027, 3, 5)
                )
        );

        assertTrue(
                exception.getMessage()
                        .contains("Insufficient annual leave balance")
        );

        assertTrue(exception.getMessage().contains("2027"));
    }

    @Test
    void shouldRejectWeekendOnlyRequest() {

        assertThrows(
                ConflictException.class,
                () -> validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(2027, 1, 9),
                        LocalDate.of(2027, 1, 10)
                )
        );

        verifyNoInteractions(leaveBalanceService);
    }

    @Test
    void shouldValidateEachYearSeparately() {

        mockBalance(2026, 2);
        mockBalance(2027, 2);

        assertDoesNotThrow(() ->
                validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(2026, 12, 30),
                        LocalDate.of(2027, 1, 4)
                )
        );

        verify(leaveBalanceService)
                .getLeaveBalance(2L, 2026);

        verify(leaveBalanceService)
                .getLeaveBalance(2L, 2027);
    }

    @Test
    void shouldRejectWhenSecondYearHasInsufficientBalance() {

        mockBalance(2026, 10);
        mockBalance(2027, 1);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(2026, 12, 30),
                        LocalDate.of(2027, 1, 4)
                )
        );

        assertTrue(exception.getMessage().contains("2027"));
    }

    @Test
    void shouldRejectInvalidDates() {

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validateRequestedLeave(
                        2L,
                        null,
                        LocalDate.of(2027, 3, 5)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(2027, 3, 5),
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(2027, 3, 5),
                        LocalDate.of(2027, 3, 1)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(1999, 12, 30),
                        LocalDate.of(2000, 1, 5)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validateRequestedLeave(
                        2L,
                        LocalDate.of(2100, 12, 30),
                        LocalDate.of(2101, 1, 5)
                )
        );

        verifyNoInteractions(leaveBalanceService);
    }

    private void mockBalance(int year, int remainingDays) {

        when(leaveBalanceService.getLeaveBalance(2L, year))
                .thenReturn(new LeaveBalanceResponse(
                        2L,
                        year,
                        30,
                        0,
                        30,
                        30 - remainingDays,
                        0,
                        remainingDays
                ));
    }
}
