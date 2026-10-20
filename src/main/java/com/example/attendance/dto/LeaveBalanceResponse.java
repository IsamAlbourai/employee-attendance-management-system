
package com.example.attendance.dto;

public record LeaveBalanceResponse(

        Long employeeId,
        int leaveYear,

        int annualAllowance,
        int carriedOverDays,
        int totalEntitlement,

        int approvedDays,
        int pendingDays,

        int remainingDays

) {
}
