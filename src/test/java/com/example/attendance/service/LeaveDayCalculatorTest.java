
package com.example.attendance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LeaveDayCalculatorTest {

    private LeaveDayCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new LeaveDayCalculator();
    }

    @Test
    void shouldCountMondayThroughFriday() {

        int days = calculator.countWorkingDays(
                LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 10, 23)
        );

        assertEquals(5, days);
    }

    @Test
    void shouldExcludeWeekendFromFullWeek() {

        int days = calculator.countWorkingDays(
                LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 10, 25)
        );

        assertEquals(5, days);
    }

    @Test
    void shouldReturnZeroForWeekendOnly() {

        int days = calculator.countWorkingDays(
                LocalDate.of(2026, 10, 24),
                LocalDate.of(2026, 10, 25)
        );

        assertEquals(0, days);
    }

    @Test
    void shouldCountFridayThroughMonday() {

        int days = calculator.countWorkingDays(
                LocalDate.of(2026, 10, 23),
                LocalDate.of(2026, 10, 26)
        );

        assertEquals(2, days);
    }

    @Test
    void shouldCountWorkingDaysAcrossMonths() {

        int days = calculator.countWorkingDays(
                LocalDate.of(2026, 10, 29),
                LocalDate.of(2026, 11, 3)
        );

        assertEquals(4, days);
    }

    @Test
    void shouldCountWorkingDaysAcrossYears() {

        int days = calculator.countWorkingDays(
                LocalDate.of(2026, 12, 30),
                LocalDate.of(2027, 1, 4)
        );

        assertEquals(4, days);
    }

    @Test
    void shouldRejectInvalidDateRanges() {

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.countWorkingDays(
                        LocalDate.of(2026, 10, 25),
                        LocalDate.of(2026, 10, 19)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.countWorkingDays(
                        null,
                        LocalDate.of(2026, 10, 19)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.countWorkingDays(
                        LocalDate.of(2026, 10, 19),
                        null
                )
        );
    }
}
