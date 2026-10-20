
package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceResponse;
import com.example.attendance.dto.ManagerAttendanceSummaryResponse;
import com.example.attendance.service.ManagerAttendanceReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager/attendance")
public class ManagerAttendanceReportController {

    private final ManagerAttendanceReportService managerAttendanceReportService;

    public ManagerAttendanceReportController(
            ManagerAttendanceReportService managerAttendanceReportService
    ) {
        this.managerAttendanceReportService =
                managerAttendanceReportService;
    }

    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> getTeamAttendance(
            Authentication authentication,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {

        List<AttendanceResponse> report =
                managerAttendanceReportService.getTeamAttendance(
                        authentication,
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(report);
    }

    @GetMapping("/summary")
    public ResponseEntity<ManagerAttendanceSummaryResponse> getTeamAttendanceSummary(
            Authentication authentication,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {

        ManagerAttendanceSummaryResponse summary =
                managerAttendanceReportService.getTeamAttendanceSummary(
                        authentication,
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(summary);
    }
}
