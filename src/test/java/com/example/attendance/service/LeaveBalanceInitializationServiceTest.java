
package com.example.attendance.service;

import com.example.attendance.entity.LeaveBalance;
import com.example.attendance.entity.User;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.LeaveBalanceRepository;
import com.example.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceInitializationServiceTest {

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private UserRepository userRepository;

    private LeaveBalanceInitializationService service;

    private User employee;

    @BeforeEach
    void setUp() {

        service = new LeaveBalanceInitializationService(
                leaveBalanceRepository,
                userRepository
        );

        employee = new User();
        employee.setId(2L);
        employee.setRole("EMPLOYEE");
        employee.setActive(true);
    }

    @Test
    void shouldInitializeNewEmployeeBalance() {

        when(userRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(employee));

        when(leaveBalanceRepository
                .findByEmployeeIdAndLeaveYear(2L, 2027))
                .thenReturn(Optional.empty());

        when(leaveBalanceRepository
                .saveAndFlush(any(LeaveBalance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeaveBalance result =
                service.initializeEmployeeYear(2L, 2027);

        assertEquals(2L, result.getEmployee().getId());
        assertEquals(2027, result.getLeaveYear());
        assertEquals(30, result.getAnnualAllowance());
        assertEquals(0, result.getCarriedOverDays());

        InOrder inOrder = inOrder(
                userRepository,
                leaveBalanceRepository
        );

        inOrder.verify(userRepository)
                .findByIdForUpdate(2L);

        inOrder.verify(leaveBalanceRepository)
                .findByEmployeeIdAndLeaveYear(2L, 2027);

        inOrder.verify(leaveBalanceRepository)
                .saveAndFlush(any(LeaveBalance.class));
    }

    @Test
    void shouldReturnExistingBalanceWithoutChanges() {

        LeaveBalance existing = new LeaveBalance();
        existing.setId(10L);
        existing.setEmployee(employee);
        existing.setLeaveYear(2027);
        existing.setAnnualAllowance(25);
        existing.setCarriedOverDays(4);

        when(userRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(employee));

        when(leaveBalanceRepository
                .findByEmployeeIdAndLeaveYear(2L, 2027))
                .thenReturn(Optional.of(existing));

        LeaveBalance result =
                service.initializeEmployeeYear(2L, 2027);

        assertSame(existing, result);
        assertEquals(25, result.getAnnualAllowance());
        assertEquals(4, result.getCarriedOverDays());

        verify(leaveBalanceRepository, never())
                .saveAndFlush(any(LeaveBalance.class));
    }

    @Test
    void shouldInitializeDifferentYearSeparately() {

        when(userRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(employee));

        when(leaveBalanceRepository
                .findByEmployeeIdAndLeaveYear(2L, 2028))
                .thenReturn(Optional.empty());

        when(leaveBalanceRepository
                .saveAndFlush(any(LeaveBalance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeaveBalance result =
                service.initializeEmployeeYear(2L, 2028);

        assertEquals(2028, result.getLeaveYear());
        assertEquals(30, result.getAnnualAllowance());
    }

    @Test
    void shouldRejectInvalidEmployeeIdAndYear() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeEmployeeYear(null, 2027)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeEmployeeYear(0L, 2027)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeEmployeeYear(2L, 1999)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeEmployeeYear(2L, 2101)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(leaveBalanceRepository);
    }

    @Test
    void shouldRejectUnknownEmployee() {

        when(userRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.initializeEmployeeYear(99L, 2027)
        );

        verifyNoInteractions(leaveBalanceRepository);
    }

    @Test
    void shouldRejectManagerAndAdminAccounts() {

        employee.setRole("MANAGER");

        when(userRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(employee));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeEmployeeYear(2L, 2027)
        );

        employee.setRole("ADMIN");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeEmployeeYear(2L, 2027)
        );

        verifyNoInteractions(leaveBalanceRepository);
    }
}
