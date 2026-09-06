package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceCheckOutResult;
import com.example.attendance.dto.AttendanceResponse;
import com.example.attendance.entity.Attendance;
import com.example.attendance.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(
            AttendanceService attendanceService
    ) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(
            Authentication authentication
    ) {

        Attendance attendance =
                attendanceService.checkIn(authentication);

        AttendanceResponse response =
                new AttendanceResponse(
                        attendance.getId(),
                        attendance.getUser().getId(),
                        attendance.getUser().getName(),
                        attendance.getUser().getEmail(),
                        attendance.getWorkDate(),
                        attendance.getCheckInTime(),
                        attendance.getCheckOutTime(),
                        null,
                        null,
                        null,
                        null
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(
            Authentication authentication
    ) {

        AttendanceCheckOutResult result =
                attendanceService.checkOut(authentication);

        Attendance attendance =
                result.getAttendance();

        AttendanceResponse response =
                new AttendanceResponse(
                        attendance.getId(),
                        attendance.getUser().getId(),
                        attendance.getUser().getName(),
                        attendance.getUser().getEmail(),
                        attendance.getWorkDate(),
                        attendance.getCheckInTime(),
                        attendance.getCheckOutTime(),
                        result.getWorkedMinutes(),
                        result.getRequiredMinutes(),
                        result.getDifferenceMinutes(),
                        result.getStatus()
                );

        return ResponseEntity.ok(response);
    }
    @GetMapping("/history")
    public ResponseEntity<List<AttendanceResponse>> getMyAttendanceHistory(
            Authentication authentication
    ) {

        List<Attendance> attendanceHistory =
                attendanceService.getMyAttendanceHistory(authentication);

        List<AttendanceResponse> response =
                attendanceHistory.stream()
                        .map(attendance ->
                                new AttendanceResponse(
                                        attendance.getId(),
                                        attendance.getUser().getId(),
                                        attendance.getUser().getName(),
                                        attendance.getUser().getEmail(),
                                        attendance.getWorkDate(),
                                        attendance.getCheckInTime(),
                                        attendance.getCheckOutTime(),
                                        attendance.getWorkedMinutes(),
                                        attendance.getRequiredMinutes(),
                                        attendance.getDifferenceMinutes(),
                                        attendance.getStatus()
                                )
                        )
                        .toList();

        return ResponseEntity.ok(response);
    }
}