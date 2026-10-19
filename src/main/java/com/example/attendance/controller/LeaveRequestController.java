
package com.example.attendance.controller;

import com.example.attendance.dto.CreateLeaveRequest;
import com.example.attendance.dto.LeaveRequestResponse;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.User;
import com.example.attendance.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(
            LeaveRequestService leaveRequestService
    ) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(
            @Valid @RequestBody CreateLeaveRequest request,
            Authentication authentication
    ) {

        LeaveRequest leaveRequest =
                leaveRequestService.createLeaveRequest(
                        request,
                        authentication
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(leaveRequest));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests(
            Authentication authentication
    ) {

        List<LeaveRequestResponse> responses =
                leaveRequestService
                        .getMyLeaveRequests(authentication)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    private LeaveRequestResponse toResponse(
            LeaveRequest leaveRequest
    ) {

        User employee = leaveRequest.getEmployee();
        User manager = leaveRequest.getManager();

        return new LeaveRequestResponse(
                leaveRequest.getId(),
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                manager.getId(),
                manager.getName(),
                manager.getEmail(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getReason(),
                leaveRequest.getStatus(),
                leaveRequest.getCreatedAt(),
                leaveRequest.getReviewedAt(),
                leaveRequest.getManagerComment()
        );
    }
}
