
package com.example.attendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "leave_balances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_leave_balance_employee_year",
                        columnNames = {"employee_id", "leave_year"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_leave_balance_year",
                        columnList = "leave_year"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "employee_id",
            nullable = false
    )
    private User employee;

    @Column(
            name = "leave_year",
            nullable = false
    )
    private int leaveYear;

    @Column(
            name = "annual_allowance",
            nullable = false
    )
    private int annualAllowance = 30;

    @Column(
            name = "carried_over_days",
            nullable = false
    )
    private int carriedOverDays = 0;

    @Version
    private Long version;
}
