
package com.example.attendance.dto;

import com.example.attendance.entity.LeaveRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LeaveRequestResponse {

    private Long id;

    private Long employeeId;
    private String employeeName;
    private String employeeEmail;

    private Long managerId;
    private String managerName;
    private String managerEmail;

    private LocalDate startDate;
    private LocalDate endDate;

    private String reason;

    private LeaveRequestStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    private String managerComment;
}
