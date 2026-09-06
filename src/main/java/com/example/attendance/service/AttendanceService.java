package com.example.attendance.service;

import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            UserRepository userRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
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
    public Attendance checkOut(Authentication authentication) {

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

        return attendanceRepository.save(attendance);
    }
}