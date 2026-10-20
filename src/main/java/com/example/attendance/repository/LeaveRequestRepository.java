
package com.example.attendance.repository;

import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT l
            FROM LeaveRequest l
            WHERE l.employee.id = :employeeId
              AND l.status IN :statuses
              AND l.startDate <= :yearEnd
              AND l.endDate >= :yearStart
            ORDER BY l.startDate ASC
            """)
    List<LeaveRequest> findLeaveRequestsOverlappingYear(
            @Param("employeeId") Long employeeId,
            @Param("statuses") List<LeaveRequestStatus> statuses,
            @Param("yearStart") LocalDate yearStart,
            @Param("yearEnd") LocalDate yearEnd
    );
}
