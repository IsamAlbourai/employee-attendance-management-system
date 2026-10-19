
package com.example.attendance.service;

import com.example.attendance.entity.User;
import com.example.attendance.exception.ConflictException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminEmployeeService {

    private final UserRepository userRepository;

    public AdminEmployeeService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User assignManager(
            Long employeeId,
            Long managerId
    ) {

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found"
                        )
                );

        User manager = userRepository.findById(managerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Manager not found"
                        )
                );

        if (employee.getId().equals(manager.getId())) {
            throw new ConflictException(
                    "An employee cannot be their own manager"
            );
        }

        if (!"EMPLOYEE".equals(employee.getRole())) {
            throw new ConflictException(
                    "The selected user is not an employee"
            );
        }

        if (!"MANAGER".equals(manager.getRole())) {
            throw new ConflictException(
                    "The selected manager does not have the MANAGER role"
            );
        }

        if (!manager.isActive()) {
            throw new ConflictException(
                    "Cannot assign an inactive manager"
            );
        }

        employee.setManager(manager);

        return userRepository.save(employee);
    }
}
