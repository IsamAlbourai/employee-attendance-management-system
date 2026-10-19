
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

    public ManagerLeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            UserRepository userRepository
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
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
        leaveRequest.setReviewedAt(
                LocalDateTime.now()
        );

        return leaveRequestRepository.save(leaveRequest);
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
