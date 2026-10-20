
package com.example.attendance.service;

import com.example.attendance.dto.LeaveBalanceResponse;
import com.example.attendance.entity.LeaveBalance;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveRequestStatus;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.LeaveBalanceRepository;
import com.example.attendance.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveDayCalculator leaveDayCalculator;

    public LeaveBalanceService(
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveRequestRepository leaveRequestRepository,
            LeaveDayCalculator leaveDayCalculator
    ) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveDayCalculator = leaveDayCalculator;
    }

    @Transactional(readOnly = true)
    public LeaveBalanceResponse getLeaveBalance(
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

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveYear(employeeId, year)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Leave balance not found for employee and year"
                        )
                );

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<LeaveRequest> requests =
                leaveRequestRepository.findLeaveRequestsOverlappingYear(
                        employeeId,
                        List.of(
                                LeaveRequestStatus.APPROVED,
                                LeaveRequestStatus.PENDING
                        ),
                        yearStart,
                        yearEnd
                );

        int approvedDays = 0;
        int pendingDays = 0;

        for (LeaveRequest request : requests) {

            LocalDate effectiveStart =
                    request.getStartDate().isBefore(yearStart)
                            ? yearStart
                            : request.getStartDate();

            LocalDate effectiveEnd =
                    request.getEndDate().isAfter(yearEnd)
                            ? yearEnd
                            : request.getEndDate();

            int workingDays = leaveDayCalculator.countWorkingDays(
                    effectiveStart,
                    effectiveEnd
            );

            if (request.getStatus() == LeaveRequestStatus.APPROVED) {
                approvedDays += workingDays;
            } else if (request.getStatus() == LeaveRequestStatus.PENDING) {
                pendingDays += workingDays;
            }
        }

        int totalEntitlement =
                balance.getAnnualAllowance()
                        + balance.getCarriedOverDays();

        int remainingDays =
                totalEntitlement - approvedDays - pendingDays;

        return new LeaveBalanceResponse(
                employeeId,
                year,
                balance.getAnnualAllowance(),
                balance.getCarriedOverDays(),
                totalEntitlement,
                approvedDays,
                pendingDays,
                remainingDays
        );
    }
}
