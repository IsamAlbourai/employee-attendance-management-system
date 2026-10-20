
package com.example.attendance.service;

import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveRequestStatus;
import com.example.attendance.entity.User;
import com.example.attendance.exception.ConflictException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.LeaveRequestRepository;
import com.example.attendance.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ManagerLeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    public ManagerLeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            UserRepository userRepository,
            EmailNotificationService emailNotificationService
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getPendingRequests(
            Authentication authentication
    ) {

        User manager = getAuthenticatedManager(authentication);

        return leaveRequestRepository
                .findByManagerIdAndStatusOrderByCreatedAtAsc(
                        manager.getId(),
                        LeaveRequestStatus.PENDING
                );
    }

    @Transactional
    public LeaveRequest approveRequest(
            Long requestId,
            String comment,
            Authentication authentication
    ) {

        return reviewRequest(
                requestId,
                LeaveRequestStatus.APPROVED,
                comment,
                authentication
        );
    }

    @Transactional
    public LeaveRequest rejectRequest(
            Long requestId,
            String comment,
            Authentication authentication
    ) {

        return reviewRequest(
                requestId,
                LeaveRequestStatus.REJECTED,
                comment,
                authentication
        );
    }

    private LeaveRequest reviewRequest(
            Long requestId,
            LeaveRequestStatus decision,
            String comment,
            Authentication authentication
    ) {

        User manager = getAuthenticatedManager(authentication);

        LeaveRequest leaveRequest = leaveRequestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Leave request not found"
                        )
                );

        if (!leaveRequest.getManager()
                .getId()
                .equals(manager.getId())) {

            throw new AccessDeniedException(
                    "You are not assigned to this leave request"
            );
        }

        if (leaveRequest.getStatus()
                != LeaveRequestStatus.PENDING) {

            throw new ConflictException(
                    "Only pending leave requests can be reviewed"
            );
        }

        leaveRequest.setStatus(decision);
        leaveRequest.setManagerComment(comment);
        leaveRequest.setReviewedAt(LocalDateTime.now());

        LeaveRequest savedLeaveRequest =
                leaveRequestRepository.save(leaveRequest);

        queueEmployeeNotification(
                savedLeaveRequest,
                manager,
                decision
        );

        return savedLeaveRequest;
    }

    private void queueEmployeeNotification(
            LeaveRequest leaveRequest,
            User manager,
            LeaveRequestStatus decision
    ) {

        String eventKey =
                "LEAVE_REQUEST_" + decision + ":" + leaveRequest.getId();

        String subject =
                "Annual Leave Request " + decision
                        + " - #" + leaveRequest.getId();

        String managerComment = leaveRequest.getManagerComment();

        if (managerComment == null || managerComment.isBlank()) {
            managerComment = "No comment provided";
        }

        String body = """
                Hello %s,

                Your annual leave request has been reviewed.

                Leave Request ID: %s
                Start Date: %s
                End Date: %s
                Decision: %s
                Reviewed By: %s
                Manager Comment: %s

                You can view your leave request in the
                Employee Attendance Management System.

                Regards,
                Attendance Management System
                """.formatted(
                leaveRequest.getEmployee().getName(),
                leaveRequest.getId(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                decision,
                manager.getName(),
                managerComment
        );

        emailNotificationService.queueEmail(
                eventKey,
                leaveRequest.getEmployee().getEmail(),
                subject,
                body
        );
    }

    private User getAuthenticatedManager(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        User manager = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Manager not found"
                        )
                );

        if (!"MANAGER".equals(manager.getRole())
                || !manager.isActive()) {

            throw new AccessDeniedException(
                    "An active manager account is required"
            );
        }

        return manager;
    }
}
