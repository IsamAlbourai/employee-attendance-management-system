package com.example.attendance.controller;

import com.example.attendance.dto.WorkScheduleRequest;
import com.example.attendance.dto.WorkScheduleResponse;
import com.example.attendance.entity.WorkSchedule;
import com.example.attendance.service.WorkScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-schedules")
public class WorkScheduleController {

    private final WorkScheduleService workScheduleService;

    public WorkScheduleController(
            WorkScheduleService workScheduleService
    ) {
        this.workScheduleService = workScheduleService;
    }

    @PostMapping
    public ResponseEntity<WorkScheduleResponse> assignOrUpdateSchedule(
            @Valid @RequestBody WorkScheduleRequest request
    ) {

        WorkSchedule workSchedule =
                workScheduleService.assignOrUpdateSchedule(
                        request.getUserId(),
                        request.getRequiredMinutes()
                );

        WorkScheduleResponse response =
                new WorkScheduleResponse(
                        workSchedule.getId(),
                        workSchedule.getUser().getId(),
                        workSchedule.getUser().getName(),
                        workSchedule.getUser().getEmail(),
                        workSchedule.getRequiredMinutes()
                );

        return ResponseEntity.ok(response);
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<WorkScheduleResponse> getScheduleByUserId(
            @PathVariable Long userId
    ) {

        WorkSchedule workSchedule =
                workScheduleService.getScheduleByUserId(userId);

        WorkScheduleResponse response =
                new WorkScheduleResponse(
                        workSchedule.getId(),
                        workSchedule.getUser().getId(),
                        workSchedule.getUser().getName(),
                        workSchedule.getUser().getEmail(),
                        workSchedule.getRequiredMinutes()
                );

        return ResponseEntity.ok(response);
    }
}