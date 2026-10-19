
package com.example.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignManagerRequest {

    @NotNull(message = "Manager ID is required")
    @Positive(message = "Manager ID must be positive")
    private Long managerId;
}
