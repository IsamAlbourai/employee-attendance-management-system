
package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssignManagerResponse {

    private Long employeeId;
    private String employeeName;
    private String employeeEmail;

    private Long managerId;
    private String managerName;
    private String managerEmail;

    private String message;
}
