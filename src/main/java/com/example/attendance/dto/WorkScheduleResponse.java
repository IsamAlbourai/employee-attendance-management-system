package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkScheduleResponse {

    private Long id;
    private Long userId;
    private String employeeName;
    private String employeeEmail;
    private int requiredMinutes;
}