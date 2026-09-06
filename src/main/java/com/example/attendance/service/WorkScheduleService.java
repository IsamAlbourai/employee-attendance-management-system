package com.example.attendance.service;

import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkSchedule;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkScheduleRepository;
import org.springframework.stereotype.Service;

@Service
public class WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final UserRepository userRepository;

    public WorkScheduleService(
            WorkScheduleRepository workScheduleRepository,
            UserRepository userRepository
    ) {
        this.workScheduleRepository = workScheduleRepository;
        this.userRepository = userRepository;
    }

    public WorkSchedule assignOrUpdateSchedule(
            Long userId,
            int requiredMinutes
    ) {

        if (requiredMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Required work time must be greater than 0 minutes"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        WorkSchedule workSchedule =
                workScheduleRepository.findByUserId(userId)
                        .orElseGet(WorkSchedule::new);

        workSchedule.setUser(user);
        workSchedule.setRequiredMinutes(requiredMinutes);

        return workScheduleRepository.save(workSchedule);
    }
    public WorkSchedule getScheduleByUserId(Long userId) {

        return workScheduleRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Work schedule not found for this user"
                        )
                );
    }
}