
package com.example.attendance.service;

import com.example.attendance.dto.AttendanceResponse;
import com.example.attendance.dto.ManagerAttendanceSummaryResponse;
import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerAttendanceReportServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private UserRepository userRepository;

    private ManagerAttendanceReportService reportService;

    private User manager;
    private User employee;
    private Authentication authentication;

    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {

        reportService = new ManagerAttendanceReportService(
                attendanceRepository,
                userRepository
        );

        manager = new User();
        manager.setId(1L);
        manager.setName("Test Manager");
        manager.setEmail("test.employee@example.com");
        manager.setRole("MANAGER");
        manager.setActive(true);

        employee = new User();
        employee.setId(2L);
        employee.setName("Second Employee");
        employee.setEmail("second.employee@example.com");
        employee.setRole("EMPLOYEE");
        employee.setActive(true);
        employee.setManager(manager);

        authentication =
                new UsernamePasswordAuthenticationToken(
                        manager.getEmail(),
                        null,
                        List.of(() -> "ROLE_MANAGER")
                );

        startDate = LocalDate.of(2026, 10, 1);
        endDate = LocalDate.of(2026, 10, 31);
    }

    @Test
    void managerShouldRetrieveTeamAttendance() {

        Attendance attendance = new Attendance();

        attendance.setId(6L);
        attendance.setUser(employee);
        attendance.setWorkDate(LocalDate.of(2026, 10, 20));
        attendance.setCheckInTime(
                LocalDateTime.of(2026, 10, 20, 8, 0)
        );
        attendance.setCheckOutTime(
                LocalDateTime.of(2026, 10, 20, 14, 0)
        );
        attendance.setWorkedMinutes(360L);
        attendance.setRequiredMinutes(480);
        attendance.setDifferenceMinutes(-120L);
        attendance.setStatus("INCOMPLETE");

        mockManager();

        when(attendanceRepository.findAttendanceForManager(
                1L,
                startDate,
                endDate
        )).thenReturn(List.of(attendance));

        List<AttendanceResponse> result =
                reportService.getTeamAttendance(
                        authentication,
                        startDate,
                        endDate
                );

        assertEquals(1, result.size());

        verify(attendanceRepository).findAttendanceForManager(
                1L,
                startDate,
                endDate
        );
    }

    @Test
    void managerShouldReceiveEmptyReportWhenNoRecordsExist() {

        mockManager();

        when(attendanceRepository.findAttendanceForManager(
                1L,
                startDate,
                endDate
        )).thenReturn(List.of());

        List<AttendanceResponse> result =
                reportService.getTeamAttendance(
                        authentication,
                        startDate,
                        endDate
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void inactiveManagerShouldBeDeniedAccess() {

        manager.setActive(false);
        mockManager();

        assertThrows(
                AccessDeniedException.class,
                () -> reportService.getTeamAttendance(
                        authentication,
                        startDate,
                        endDate
                )
        );

        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void employeeRoleShouldBeDeniedAccess() {

        manager.setRole("EMPLOYEE");
        mockManager();

        assertThrows(
                AccessDeniedException.class,
                () -> reportService.getTeamAttendance(
                        authentication,
                        startDate,
                        endDate
                )
        );

        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void invalidDateRangeShouldBeRejected() {

        mockManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getTeamAttendance(
                        authentication,
                        endDate,
                        startDate
                )
        );

        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void missingAuthenticationShouldBeRejected() {

        assertThrows(
                AccessDeniedException.class,
                () -> reportService.getTeamAttendance(
                        null,
                        startDate,
                        endDate
                )
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void unknownManagerShouldBeRejected() {

        when(userRepository.findByEmail(manager.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> reportService.getTeamAttendance(
                        authentication,
                        startDate,
                        endDate
                )
        );

        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void summaryShouldCalculateAttendanceTotals() {

        Attendance completed = new Attendance();
        completed.setId(1L);
        completed.setUser(employee);
        completed.setCheckOutTime(
                LocalDateTime.of(2026, 10, 20, 16, 0)
        );
        completed.setWorkedMinutes(500L);
        completed.setRequiredMinutes(480);
        completed.setStatus("COMPLETED");

        Attendance incomplete = new Attendance();
        incomplete.setId(2L);
        incomplete.setUser(employee);
        incomplete.setCheckOutTime(
                LocalDateTime.of(2026, 10, 21, 14, 0)
        );
        incomplete.setWorkedMinutes(360L);
        incomplete.setRequiredMinutes(480);
        incomplete.setStatus("INCOMPLETE");

        Attendance open = new Attendance();
        open.setId(3L);
        open.setUser(employee);
        open.setCheckInTime(
                LocalDateTime.of(2026, 10, 22, 8, 0)
        );
        open.setStatus(null);

        mockManager();

        when(attendanceRepository.findAttendanceForManager(
                1L,
                startDate,
                endDate
        )).thenReturn(
                List.of(completed, incomplete, open)
        );

        ManagerAttendanceSummaryResponse summary =
                reportService.getTeamAttendanceSummary(
                        authentication,
                        startDate,
                        endDate
                );

        assertEquals(startDate, summary.startDate());
        assertEquals(endDate, summary.endDate());

        assertEquals(3, summary.totalRecords());
        assertEquals(1, summary.completedRecords());
        assertEquals(1, summary.incompleteRecords());
        assertEquals(1, summary.openRecords());

        assertEquals(860, summary.totalWorkedMinutes());
        assertEquals(960, summary.totalRequiredMinutes());
        assertEquals(120, summary.totalMissingMinutes());

        verify(attendanceRepository).findAttendanceForManager(
                1L,
                startDate,
                endDate
        );
    }

    @Test
    void summaryShouldReturnZerosWhenNoAttendanceExists() {

        mockManager();

        when(attendanceRepository.findAttendanceForManager(
                1L,
                startDate,
                endDate
        )).thenReturn(List.of());

        ManagerAttendanceSummaryResponse summary =
                reportService.getTeamAttendanceSummary(
                        authentication,
                        startDate,
                        endDate
                );

        assertEquals(0, summary.totalRecords());
        assertEquals(0, summary.completedRecords());
        assertEquals(0, summary.incompleteRecords());
        assertEquals(0, summary.openRecords());

        assertEquals(0, summary.totalWorkedMinutes());
        assertEquals(0, summary.totalRequiredMinutes());
        assertEquals(0, summary.totalMissingMinutes());
    }

    @Test
    void summaryShouldRejectInactiveManager() {

        manager.setActive(false);
        mockManager();

        assertThrows(
                AccessDeniedException.class,
                () -> reportService.getTeamAttendanceSummary(
                        authentication,
                        startDate,
                        endDate
                )
        );

        verifyNoInteractions(attendanceRepository);
    }
    private void mockManager() {

        when(userRepository.findByEmail(manager.getEmail()))
                .thenReturn(Optional.of(manager));
    }
}
