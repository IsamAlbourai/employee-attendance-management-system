
package com.example.attendance.service;

import com.example.attendance.dto.AttendanceCheckOutResult;
import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import com.example.attendance.entity.WorkSchedule;
import com.example.attendance.exception.ConflictException;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.WorkScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkScheduleRepository workScheduleRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private Authentication authentication;

    private AttendanceService attendanceService;

    private User user;

    @BeforeEach
    void setUp() {

        attendanceService = new AttendanceService(
                attendanceRepository,
                userRepository,
                workScheduleRepository,
                emailNotificationService
        );

        user = new User();
        user.setId(2L);
        user.setName("Second Employee");
        user.setEmail("second.employee@example.com");
        user.setPassword("hashed-password");
        user.setRole("EMPLOYEE");
        user.setActive(true);
    }

    @Test
    void checkInShouldCreateAttendanceRecord() {

        when(authentication.getName())
                .thenReturn("second.employee@example.com");

        when(userRepository.findByEmail(
                "second.employee@example.com"
        )).thenReturn(Optional.of(user));

        when(attendanceRepository
                .findByUserAndCheckOutTimeIsNull(user))
                .thenReturn(Optional.empty());

        when(attendanceRepository.save(any(Attendance.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Attendance attendance =
                attendanceService.checkIn(authentication);

        assertNotNull(attendance);
        assertEquals(user, attendance.getUser());
        assertNotNull(attendance.getWorkDate());
        assertNotNull(attendance.getCheckInTime());
        assertNull(attendance.getCheckOutTime());

        verify(attendanceRepository, times(1))
                .save(any(Attendance.class));

        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void checkInShouldRejectDuplicateCheckIn() {

        Attendance existingAttendance = new Attendance();

        existingAttendance.setUser(user);
        existingAttendance.setCheckInTime(
                LocalDateTime.now()
        );

        when(authentication.getName())
                .thenReturn("second.employee@example.com");

        when(userRepository.findByEmail(
                "second.employee@example.com"
        )).thenReturn(Optional.of(user));

        when(attendanceRepository
                .findByUserAndCheckOutTimeIsNull(user))
                .thenReturn(Optional.of(existingAttendance));

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> attendanceService.checkIn(authentication)
                );

        assertEquals(
                "Employee is already checked in",
                exception.getMessage()
        );

        verify(attendanceRepository, never())
                .save(any(Attendance.class));

        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void checkOutShouldCalculateWorkedTimeAndStatus() {

        Attendance attendance = new Attendance();

        attendance.setUser(user);
        attendance.setCheckInTime(
                LocalDateTime.now().minusMinutes(500)
        );

        WorkSchedule workSchedule = new WorkSchedule();
        workSchedule.setRequiredMinutes(480);

        mockCheckoutDependencies(attendance, workSchedule);

        AttendanceCheckOutResult result =
                attendanceService.checkOut(authentication);

        assertNotNull(result);

        assertTrue(result.getWorkedMinutes() >= 500);
        assertEquals(480, result.getRequiredMinutes());
        assertTrue(result.getDifferenceMinutes() >= 20);
        assertEquals("COMPLETED", result.getStatus());

        Attendance savedAttendance = result.getAttendance();

        assertNotNull(savedAttendance.getCheckOutTime());
        assertEquals("COMPLETED", savedAttendance.getStatus());
        assertEquals(480, savedAttendance.getRequiredMinutes());

        verify(attendanceRepository).save(attendance);

        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void incompleteCheckoutShouldNotifyAssignedManager() {

        User manager = new User();
        manager.setId(1L);
        manager.setName("Test Manager");
        manager.setEmail("test.employee@example.com");
        manager.setRole("MANAGER");
        manager.setActive(true);

        user.setManager(manager);

        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setWorkDate(LocalDate.now());
        attendance.setCheckInTime(
                LocalDateTime.now().minusMinutes(60)
        );

        WorkSchedule workSchedule = new WorkSchedule();
        workSchedule.setRequiredMinutes(480);

        mockCheckoutDependencies(attendance, workSchedule);

        AttendanceCheckOutResult result =
                attendanceService.checkOut(authentication);

        assertEquals("INCOMPLETE", result.getStatus());
        assertEquals(480, result.getRequiredMinutes());
        assertTrue(result.getWorkedMinutes() < 480);
        assertTrue(result.getDifferenceMinutes() < 0);

        verify(emailNotificationService, times(1))
                .queueEmail(
                        eq("ATTENDANCE_INCOMPLETE:10"),
                        eq(manager.getEmail()),
                        argThat(subject ->
                                subject.contains("Incomplete Attendance")
                                        && subject.contains(user.getName())
                        ),
                        argThat(body ->
                                body.contains(user.getEmail())
                                        && body.contains("INCOMPLETE")
                                        && body.contains("Required Minutes: 480")
                                        && body.contains("Attendance ID: 10")
                        )
                );
    }

    @Test
    void incompleteCheckoutWithoutManagerShouldNotQueueEmail() {

        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setWorkDate(LocalDate.now());
        attendance.setCheckInTime(
                LocalDateTime.now().minusMinutes(60)
        );

        WorkSchedule workSchedule = new WorkSchedule();
        workSchedule.setRequiredMinutes(480);

        mockCheckoutDependencies(attendance, workSchedule);

        AttendanceCheckOutResult result =
                attendanceService.checkOut(authentication);

        assertEquals("INCOMPLETE", result.getStatus());

        verify(attendanceRepository).save(attendance);

        verifyNoInteractions(emailNotificationService);
    }

    private void mockCheckoutDependencies(
            Attendance attendance,
            WorkSchedule workSchedule
    ) {

        when(authentication.getName())
                .thenReturn(user.getEmail());

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(attendanceRepository
                .findByUserAndCheckOutTimeIsNull(user))
                .thenReturn(Optional.of(attendance));

        when(workScheduleRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(workSchedule));

        when(attendanceRepository.save(any(Attendance.class)))
                .thenAnswer(invocation -> {
                    Attendance saved = invocation.getArgument(0);
                    saved.setId(10L);
                    return saved;
                });
    }
}
