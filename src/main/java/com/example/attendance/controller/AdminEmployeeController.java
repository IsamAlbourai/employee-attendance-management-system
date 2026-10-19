
package com.example.attendance.controller;

import com.example.attendance.dto.AssignManagerRequest;
import com.example.attendance.dto.AssignManagerResponse;
import com.example.attendance.entity.User;
import com.example.attendance.service.AdminEmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/employees")
public class AdminEmployeeController {

    private final AdminEmployeeService adminEmployeeService;

    public AdminEmployeeController(
            AdminEmployeeService adminEmployeeService
    ) {
        this.adminEmployeeService = adminEmployeeService;
    }

    @PutMapping("/{employeeId}/manager")
    public ResponseEntity<AssignManagerResponse> assignManager(
            @PathVariable Long employeeId,
            @Valid @RequestBody AssignManagerRequest request
    ) {

        User employee = adminEmployeeService.assignManager(
                employeeId,
                request.getManagerId()
        );

        User manager = employee.getManager();

        AssignManagerResponse response = new AssignManagerResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                manager.getId(),
                manager.getName(),
                manager.getEmail(),
                "Manager assigned successfully"
        );

        return ResponseEntity.ok(response);
    }
}
