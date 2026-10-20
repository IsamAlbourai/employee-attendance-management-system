
package com.example.attendance.service;

import com.example.attendance.entity.LeaveBalance;
import com.example.attendance.entity.User;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.LeaveBalanceRepository;
import com.example.attendance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveBalanceInitializationService {

    private static final int DEFAULT_ANNUAL_ALLOWANCE = 30;

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;

    public LeaveBalanceInitializationService(
            LeaveBalanceRepository leaveBalanceRepository,
            UserRepository userRepository
    ) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LeaveBalance initializeEmployeeYear(
            Long employeeId,
            int year
    ) {

        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException(
                    "Valid employee ID is required"
            );
        }

        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException(
                    "Leave year must be between 2000 and 2100"
            );
        }

        // Lock the existing employee row before checking
        // whether the annual leave balance exists.
        User employee = userRepository
                .findByIdForUpdate(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found"
                        )
                );

        if (!"EMPLOYEE".equals(employee.getRole())) {
            throw new IllegalArgumentException(
                    "Leave balances can only be initialized for employees"
            );
        }

        return leaveBalanceRepository
                .findByEmployeeIdAndLeaveYear(employeeId, year)
                .orElseGet(() -> {

                    LeaveBalance balance = new LeaveBalance();

                    balance.setEmployee(employee);
                    balance.setLeaveYear(year);
                    balance.setAnnualAllowance(
                            DEFAULT_ANNUAL_ALLOWANCE
                    );
                    balance.setCarriedOverDays(0);

                    return leaveBalanceRepository.saveAndFlush(balance);
                });
    }
}
