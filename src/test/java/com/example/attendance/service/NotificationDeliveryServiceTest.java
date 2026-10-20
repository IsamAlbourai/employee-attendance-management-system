
package com.example.attendance.service;

import com.example.attendance.entity.EmailNotification;
import com.example.attendance.entity.NotificationStatus;
import com.example.attendance.repository.EmailNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    @Mock
    private EmailNotificationRepository emailNotificationRepository;

    @Mock
    private NotificationClaimService notificationClaimService;

    @Mock
    private NotificationResultService notificationResultService;

    @Mock
    private EmailService emailService;

    private NotificationDeliveryService notificationDeliveryService;

    private EmailNotification notification;

    @BeforeEach
    void setUp() {

        notificationDeliveryService =
                new NotificationDeliveryService(
                        emailNotificationRepository,
                        notificationClaimService,
                        notificationResultService,
                        emailService
                );

        notification = new EmailNotification();

        notification.setId(1L);
        notification.setEventKey("LEAVE_REQUEST_CREATED:3");
        notification.setRecipientEmail("manager@example.com");
        notification.setSubject("New Annual Leave Request");
        notification.setBody("An employee submitted annual leave.");
        notification.setStatus(NotificationStatus.PENDING);
    }

    @Test
    void shouldSendAndMarkNotificationAsSent() {

        mockReadyNotification();

        when(notificationClaimService.claimNotification(1L))
                .thenReturn(true);

        when(emailNotificationRepository.findById(1L))
                .thenReturn(Optional.of(notification));

        notificationDeliveryService.processPendingNotifications();

        verify(emailService).sendEmail(
                "manager@example.com",
                "New Annual Leave Request",
                "An employee submitted annual leave."
        );

        verify(notificationResultService).markAsSent(1L);

        verify(notificationResultService, never())
                .markAsFailed(anyLong());
    }

    @Test
    void shouldMarkNotificationAsFailedWhenSmtpFails() {

        mockReadyNotification();

        when(notificationClaimService.claimNotification(1L))
                .thenReturn(true);

        when(emailNotificationRepository.findById(1L))
                .thenReturn(Optional.of(notification));

        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailService)
                .sendEmail(
                        anyString(),
                        anyString(),
                        anyString()
                );

        notificationDeliveryService.processPendingNotifications();

        verify(notificationResultService).markAsFailed(1L);

        verify(notificationResultService, never())
                .markAsSent(anyLong());
    }

    @Test
    void shouldSkipNotificationWhenClaimFails() {

        mockReadyNotification();

        when(notificationClaimService.claimNotification(1L))
                .thenReturn(false);

        notificationDeliveryService.processPendingNotifications();

        verifyNoInteractions(emailService);

        verifyNoInteractions(notificationResultService);

        verify(emailNotificationRepository, never())
                .findById(anyLong());
    }

    private void mockReadyNotification() {

        when(emailNotificationRepository
                .findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        anyCollection(),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(notification));
    }
}
