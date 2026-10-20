
package com.example.attendance.service;

import com.example.attendance.dto.CreateLeaveRequest;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveRequestStatus;
import com.example.attendance.entity.User;
import com.example.attendance.exception.ConflictException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.LeaveRequestRepository;
import com.example.attendance.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            UserRepository userRepository,
            EmailNotificationService emailNotificationService
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public LeaveRequest createLeaveRequest(
            CreateLeaveRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User employee = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found"
                        )
                );

        if (!employee.isActive()) {
            throw new ConflictException(
                    "Inactive employees cannot request leave"
            );
        }

        User manager = employee.getManager();

        if (manager == null) {
            throw new ConflictException(
                    "Employee does not have an assigned manager"
            );
        }

        if (!"MANAGER".equals(manager.getRole())) {
            throw new ConflictException(
                    "Assigned manager does not have the MANAGER role"
            );
        }

        if (!manager.isActive()) {
            throw new ConflictException(
                    "Assigned manager is inactive"
            );
        }

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Start date cannot be in the past"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        boolean overlappingRequest =
                leaveRequestRepository
                        .existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                employee.getId(),
                                List.of(
                                        LeaveRequestStatus.PENDING,
                                        LeaveRequestStatus.APPROVED
                                ),
                                endDate,
                                startDate
                        );

        if (overlappingRequest) {
            throw new ConflictException(
                    "Employee already has an overlapping leave request"
            );
        }

        LeaveRequest leaveRequest = new LeaveRequest();

        leaveRequest.setEmployee(employee);
        leaveRequest.setManager(manager);
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);

        LeaveRequest savedLeaveRequest =
                leaveRequestRepository.save(leaveRequest);

        queueManagerNotification(savedLeaveRequest, employee, manager);

        return savedLeaveRequest;
    }

    private void queueManagerNotification(
            LeaveRequest leaveRequest,
            User employee,
            User manager
    ) {

        String eventKey =
                "LEAVE_REQUEST_CREATED:" + leaveRequest.getId();

        String subject =
                "New Annual Leave Request - #" + leaveRequest.getId();

        String reason = leaveRequest.getReason();

        if (reason == null || reason.isBlank()) {
            reason = "No reason provided";
        }

        String body = """
                Hello,

                A new annual leave request requires your review.

                Leave Request ID: %s
                Employee: %s
                Employee Email: %s
                Start Date: %s
                End Date: %s
                Reason: %s
                Status: PENDING

                Please review this request in the
                Employee Attendance Management System.

                Regards,
                Attendance Management System
                """.formatted(
                leaveRequest.getId(),
                employee.getName(),
                employee.getEmail(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                reason
        );

        emailNotificationService.queueEmail(
                eventKey,
                manager.getEmail(),
                subject,
                body
        );
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getMyLeaveRequests(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User employee = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found"
                        )
                );

        return leaveRequestRepository
                .findByEmployeeIdOrderByCreatedAtDesc(
                        employee.getId()
                );
    }
}
