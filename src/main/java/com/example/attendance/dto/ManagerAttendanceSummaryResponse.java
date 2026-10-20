
package com.example.attendance.dto;

import java.time.LocalDate;

public record ManagerAttendanceSummaryResponse(

        LocalDate startDate,
        LocalDate endDate,

        long totalRecords,
        long completedRecords,
        long incompleteRecords,
        long openRecords,

        long totalWorkedMinutes,
        long totalRequiredMinutes,
        long totalMissingMinutes

) {
}
