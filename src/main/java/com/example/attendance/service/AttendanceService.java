package com.example.attendance.service;

import com.example.attendance.dto.AttendanceCheckOutResult;
import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkSchedule;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkScheduleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final WorkScheduleRepository workScheduleRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            UserRepository userRepository,
            WorkScheduleRepository workScheduleRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.workScheduleRepository = workScheduleRepository;
    }

    public Attendance checkIn(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        attendanceRepository
                .findByUserAndCheckOutTimeIsNull(user)
                .ifPresent(attendance -> {
                    throw new IllegalArgumentException(
                            "Employee is already checked in"
                    );
                });

        Attendance attendance = new Attendance();

        attendance.setUser(user);
        attendance.setWorkDate(LocalDate.now());
        attendance.setCheckInTime(LocalDateTime.now());

        return attendanceRepository.save(attendance);
    }

    public AttendanceCheckOutResult checkOut(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        Attendance attendance =
                attendanceRepository
                        .findByUserAndCheckOutTimeIsNull(user)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee is not currently checked in"
                                )
                        );

        attendance.setCheckOutTime(LocalDateTime.now());

        long workedMinutes =
                Duration.between(
                        attendance.getCheckInTime(),
                        attendance.getCheckOutTime()
                ).toMinutes();

        WorkSchedule workSchedule =
                workScheduleRepository.findByUserId(user.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Work schedule not found for this user"
                                )
                        );

        int requiredMinutes =
                workSchedule.getRequiredMinutes();

        long differenceMinutes =
                workedMinutes - requiredMinutes;

        String status =
                workedMinutes >= requiredMinutes
                        ? "COMPLETED"
                        : "INCOMPLETE";

        Attendance savedAttendance =
                attendanceRepository.save(attendance);

        return new AttendanceCheckOutResult(
                savedAttendance,
                workedMinutes,
                requiredMinutes,
                differenceMinutes,
                status
        );
    }
}