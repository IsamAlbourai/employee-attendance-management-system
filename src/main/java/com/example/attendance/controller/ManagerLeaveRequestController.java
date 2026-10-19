
package com.example.attendance.controller;

import com.example.attendance.dto.LeaveDecisionRequest;
import com.example.attendance.dto.LeaveRequestResponse;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.User;
import com.example.attendance.service.ManagerLeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/leave-requests")
public class ManagerLeaveRequestController {

    private final ManagerLeaveRequestService managerLeaveRequestService;

    public ManagerLeaveRequestController(
            ManagerLeaveRequestService managerLeaveRequestService
    ) {
        this.managerLeaveRequestService = managerLeaveRequestService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequestResponse>> getPendingRequests(
            Authentication authentication
    ) {

        List<LeaveRequestResponse> responses =
                managerLeaveRequestService
                        .getPendingRequests(authentication)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse> approveRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveDecisionRequest request,
            Authentication authentication
    ) {

        LeaveRequest leaveRequest =
                managerLeaveRequestService.approveRequest(
                        id,
                        request.getComment(),
                        authentication
                );

        return ResponseEntity.ok(toResponse(leaveRequest));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveDecisionRequest request,
            Authentication authentication
    ) {

        LeaveRequest leaveRequest =
                managerLeaveRequestService.rejectRequest(
                        id,
                        request.getComment(),
                        authentication
                );

        return ResponseEntity.ok(toResponse(leaveRequest));
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
