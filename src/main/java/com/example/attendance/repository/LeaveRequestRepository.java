
package com.example.attendance.repository;

import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(
            Long employeeId
    );

    List<LeaveRequest> findByManagerIdAndStatusOrderByCreatedAtAsc(
            Long managerId,
            LeaveRequestStatus status
    );

    boolean existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            List<LeaveRequestStatus> statuses,
            LocalDate endDate,
            LocalDate startDate
    );
}
