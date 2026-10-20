
package com.example.attendance.repository;

import com.example.attendance.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface LeaveBalanceRepository
        extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndLeaveYear(
            Long employeeId,
            int leaveYear
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM LeaveBalance b
            WHERE b.employee.id = :employeeId
              AND b.leaveYear = :leaveYear
            """)
    Optional<LeaveBalance> findForUpdate(
            @Param("employeeId") Long employeeId,
            @Param("leaveYear") int leaveYear
    );
}
