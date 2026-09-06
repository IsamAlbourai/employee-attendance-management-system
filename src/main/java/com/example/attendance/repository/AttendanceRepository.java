package com.example.attendance.repository;

import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserAndWorkDate(
            User user,
            LocalDate workDate
    );

    Optional<Attendance> findByUserAndCheckOutTimeIsNull(
            User user
    );
}