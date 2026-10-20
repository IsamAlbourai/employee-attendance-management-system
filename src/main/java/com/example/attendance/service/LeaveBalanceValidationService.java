
package com.example.attendance.service;

import com.example.attendance.dto.LeaveBalanceResponse;
import com.example.attendance.exception.ConflictException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class LeaveBalanceValidationService {

    private final LeaveBalanceService leaveBalanceService;
    private final LeaveDayCalculator leaveDayCalculator;

    public LeaveBalanceValidationService(
            LeaveBalanceService leaveBalanceService,
            LeaveDayCalculator leaveDayCalculator
    ) {
        this.leaveBalanceService = leaveBalanceService;
        this.leaveDayCalculator = leaveDayCalculator;
    }

    public void validateRequestedLeave(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        if (startDate.getYear() < 2000
                || endDate.getYear() > 2100) {
            throw new IllegalArgumentException(
                    "Leave dates must be between 2000 and 2100"
            );
        }

        int totalRequestedDays =
                leaveDayCalculator.countWorkingDays(
                        startDate,
                        endDate
                );

        if (totalRequestedDays == 0) {
            throw new ConflictException(
                    "Leave request must include at least one working day"
            );
        }

        for (int year = startDate.getYear();
             year <= endDate.getYear();
             year++) {

            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd = LocalDate.of(year, 12, 31);

            LocalDate effectiveStart =
                    startDate.isBefore(yearStart)
                            ? yearStart
                            : startDate;

            LocalDate effectiveEnd =
                    endDate.isAfter(yearEnd)
                            ? yearEnd
                            : endDate;

            int requestedDays =
                    leaveDayCalculator.countWorkingDays(
                            effectiveStart,
                            effectiveEnd
                    );

            if (requestedDays == 0) {
                continue;
            }

            LeaveBalanceResponse balance =
                    leaveBalanceService.getLeaveBalance(
                            employeeId,
                            year
                    );

            if (requestedDays > balance.remainingDays()) {
                throw new ConflictException(
                        "Insufficient annual leave balance for "
                                + year
                                + ". Requested: "
                                + requestedDays
                                + " working days. Available: "
                                + balance.remainingDays()
                                + " working days."
                );
            }
        }
    }
}
