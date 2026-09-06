package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AttendanceResponse {

    private Long id;
    private Long userId;
    private String employeeName;
    private String employeeEmail;
    private LocalDate workDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    private Long workedMinutes;
    private Integer requiredMinutes;
    private Long differenceMinutes;
    private String status;
}