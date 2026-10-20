
package com.example.attendance.service;

import com.example.attendance.dto.AttendanceResponse;
import com.example.attendance.dto.ManagerAttendanceSummaryResponse;
import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ManagerAttendanceReportService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    public ManagerAttendanceReportService(
            AttendanceRepository attendanceRepository,
            UserRepository userRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getTeamAttendance(
            Authentication authentication,
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<Attendance> records = getAuthorizedTeamRecords(
                authentication,
                startDate,
                endDate
        );

        return records.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ManagerAttendanceSummaryResponse getTeamAttendanceSummary(
            Authentication authentication,
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<Attendance> records = getAuthorizedTeamRecords(
                authentication,
                startDate,
                endDate
        );

        long totalRecords = records.size();
        long completedRecords = 0;
        long incompleteRecords = 0;
        long openRecords = 0;

        long totalWorkedMinutes = 0;
        long totalRequiredMinutes = 0;
        long totalMissingMinutes = 0;

        for (Attendance attendance : records) {

            if (attendance.getCheckOutTime() == null) {
                openRecords++;
                continue;
            }

            String status = attendance.getStatus();

            if ("COMPLETED".equals(status)) {
                completedRecords++;
            } else if ("INCOMPLETE".equals(status)) {
                incompleteRecords++;
            }

            long workedMinutes =
                    attendance.getWorkedMinutes() == null
                            ? 0
                            : attendance.getWorkedMinutes();

            long requiredMinutes =
                    attendance.getRequiredMinutes() == null
                            ? 0
                            : attendance.getRequiredMinutes();

            totalWorkedMinutes += workedMinutes;
            totalRequiredMinutes += requiredMinutes;

            totalMissingMinutes += Math.max(
                    0,
                    requiredMinutes - workedMinutes
            );
        }

        return new ManagerAttendanceSummaryResponse(
                startDate,
                endDate,
                totalRecords,
                completedRecords,
                incompleteRecords,
                openRecords,
                totalWorkedMinutes,
                totalRequiredMinutes,
                totalMissingMinutes
        );
    }

    private List<Attendance> getAuthorizedTeamRecords(
            Authentication authentication,
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        User manager = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Manager not found"
                        )
                );

        if (!manager.isActive()
                || !"MANAGER".equals(manager.getRole())) {

            throw new AccessDeniedException(
                    "An active manager account is required"
            );
        }

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

        return attendanceRepository.findAttendanceForManager(
                manager.getId(),
                startDate,
                endDate
        );
    }

    private AttendanceResponse toResponse(
            Attendance attendance
    ) {

        User employee = attendance.getUser();

        return new AttendanceResponse(
                attendance.getId(),
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                attendance.getWorkDate(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                attendance.getWorkedMinutes(),
                attendance.getRequiredMinutes(),
                attendance.getDifferenceMinutes(),
                attendance.getStatus()
        );
    }
}
