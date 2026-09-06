package com.example.attendance.dto;

import com.example.attendance.entity.Attendance;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttendanceCheckOutResult {

    private Attendance attendance;
    private long workedMinutes;
    private int requiredMinutes;
    private long differenceMinutes;
    private String status;
}