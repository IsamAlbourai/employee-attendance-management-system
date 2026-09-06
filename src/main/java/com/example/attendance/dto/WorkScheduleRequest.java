package com.example.attendance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkScheduleRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @Min(value = 1, message = "Required minutes must be at least 1")
    private int requiredMinutes;
}