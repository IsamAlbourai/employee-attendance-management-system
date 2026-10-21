
package com.example.attendance.service;

import com.example.attendance.dto.AttendanceCheckOutResult;
import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkSchedule;
import com.example.attendance.exception.ConflictException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkScheduleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final EmailNotificationService emailNotificationService;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            UserRepository userRepository,
            WorkScheduleRepository workScheduleRepository,
            EmailNotificationService emailNotificationService
    ) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public Attendance checkIn(Authentication authentication) {

        User user = getLockedUser(authentication);

        attendanceRepository
                .findByUserAndCheckOutTimeIsNull(user)
                .ifPresent(attendance -> {
                    throw new ConflictException(
                            "Employee is already checked in"
                    );
                });

        Attendance attendance = new Attendance();

        attendance.setUser(user);
        attendance.setWorkDate(LocalDate.now());
        attendance.setCheckInTime(LocalDateTime.now());

        return attendanceRepository.saveAndFlush(attendance);
    }

    @Transactional
    public AttendanceCheckOutResult checkOut(
            Authentication authentication
    ) {

        User user = getLockedUser(authentication);

        Attendance attendance =
                attendanceRepository
                        .findByUserAndCheckOutTimeIsNull(user)
                        .orElseThrow(() ->
                                new ConflictException(
                                        "Employee is not currently checked in"
                                )
                        );

        WorkSchedule workSchedule =
                workScheduleRepository
                        .findByUserId(user.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Work schedule not found for this user"
                                )
                        );

        attendance.setCheckOutTime(LocalDateTime.now());

        long workedMinutes =
                Duration.between(
                        attendance.getCheckInTime(),
                        attendance.getCheckOutTime()
                ).toMinutes();

        int requiredMinutes =
                workSchedule.getRequiredMinutes();

        long differenceMinutes =
                workedMinutes - requiredMinutes;

        String status =
                workedMinutes >= requiredMinutes
                        ? "COMPLETED"
                        : "INCOMPLETE";

        attendance.setWorkedMinutes(workedMinutes);
        attendance.setRequiredMinutes(requiredMinutes);
        attendance.setDifferenceMinutes(differenceMinutes);
        attendance.setStatus(status);

        Attendance savedAttendance =
                attendanceRepository.save(attendance);

        if ("INCOMPLETE".equals(status)) {
            queueIncompleteAttendanceNotification(
                    savedAttendance,
                    user,
                    requiredMinutes,
                    workedMinutes,
                    differenceMinutes
            );
        }

        return new AttendanceCheckOutResult(
                savedAttendance,
                workedMinutes,
                requiredMinutes,
                differenceMinutes,
                status
        );
    }

    private User getLockedUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException(
                    "Authentication is required"
            );
        }

        User account = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        User user = userRepository
                .findByIdForUpdate(account.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        if (!user.isActive()) {
            throw new ConflictException(
                    "Inactive users cannot record attendance"
            );
        }

        return user;
    }

    private void queueIncompleteAttendanceNotification(
            Attendance attendance,
            User employee,
            int requiredMinutes,
            long workedMinutes,
            long differenceMinutes
    ) {

        User manager = employee.getManager();

        if (manager == null
                || !manager.isActive()
                || !"MANAGER".equals(manager.getRole())) {
            return;
        }

        String eventKey =
                "ATTENDANCE_INCOMPLETE:" + attendance.getId();

        String subject =
                "Incomplete Attendance - "
                        + employee.getName()
                        + " - "
                        + attendance.getWorkDate();

        String body = """
                Hello %s,

                An employee has checked out before completing
                their required working hours.

                Employee: %s
                Employee Email: %s
                Attendance ID: %s
                Work Date: %s

                Required Minutes: %s
                Worked Minutes: %s
                Missing Minutes: %s

                Attendance Status: INCOMPLETE

                Please review this attendance record in the
                Employee Attendance Management System.

                Regards,
                Attendance Management System
                """.formatted(
                manager.getName(),
                employee.getName(),
                employee.getEmail(),
                attendance.getId(),
                attendance.getWorkDate(),
                requiredMinutes,
                workedMinutes,
                Math.abs(differenceMinutes)
        );

        emailNotificationService.queueEmail(
                eventKey,
                manager.getEmail(),
                subject,
                body
        );
    }

    @Transactional(readOnly = true)
    public List<Attendance> getMyAttendanceHistory(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        return attendanceRepository
                .findAllByUserOrderByCheckInTimeDesc(user);
    }
}
