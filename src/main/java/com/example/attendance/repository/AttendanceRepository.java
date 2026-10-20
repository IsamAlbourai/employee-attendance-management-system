
package com.example.attendance.repository;

import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserAndCheckOutTimeIsNull(
            User user
    );

    List<Attendance> findAllByUserOrderByCheckInTimeDesc(
            User user
    );

    @Query("""
            SELECT a
            FROM Attendance a
            JOIN FETCH a.user employee
            WHERE employee.manager.id = :managerId
              AND a.workDate BETWEEN :startDate AND :endDate
            ORDER BY a.workDate DESC, a.checkInTime DESC
            """)
    List<Attendance> findAttendanceForManager(
            @Param("managerId") Long managerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
