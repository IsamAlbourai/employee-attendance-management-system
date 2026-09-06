package com.example.attendance.repository;

import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkScheduleRepository
        extends JpaRepository<WorkSchedule, Long> {

    Optional<WorkSchedule> findByUser(User user);

    Optional<WorkSchedule> findByUserId(Long userId);
}