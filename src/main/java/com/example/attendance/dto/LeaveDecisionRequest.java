
package com.example.attendance.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveDecisionRequest {

    @Size(
            max = 1000,
            message = "Manager comment cannot exceed 1000 characters"
    )
    private String comment;
}
