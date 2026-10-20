
package com.example.attendance.controller;

import com.example.attendance.dto.LeaveBalanceResponse;
import com.example.attendance.entity.User;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.LeaveBalanceInitializationService;
import com.example.attendance.service.LeaveBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave-balances")
public class LeaveBalanceController {

    private final UserRepository userRepository;
    private final LeaveBalanceInitializationService initializationService;
    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(
            UserRepository userRepository,
            LeaveBalanceInitializationService initializationService,
            LeaveBalanceService leaveBalanceService
    ) {
        this.userRepository = userRepository;
        this.initializationService = initializationService;
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/my")
    public ResponseEntity<LeaveBalanceResponse> getMyLeaveBalance(
            @RequestParam int year,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        User employee = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found"
                        )
                );

        if (!employee.isActive()
                || !"EMPLOYEE".equals(employee.getRole())) {

            throw new AccessDeniedException(
                    "An active employee account is required"
            );
        }

        Long employeeId = employee.getId();

        // Creates the employee-year balance only if missing.
        // This service performs the operation in a transaction.
        initializationService.initializeEmployeeYear(
                employeeId,
                year
        );

        // Read the current balance, including approved
        // and pending leave requests.
        LeaveBalanceResponse response =
                leaveBalanceService.getLeaveBalance(
                        employeeId,
                        year
                );

        return ResponseEntity.ok(response);
    }
}
