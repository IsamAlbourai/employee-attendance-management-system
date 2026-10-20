
package com.example.attendance.service;

import com.example.attendance.entity.EmailNotification;
import com.example.attendance.entity.NotificationStatus;
import com.example.attendance.repository.EmailNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private EmailNotificationRepository emailNotificationRepository;

    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {

        emailNotificationService =
                new EmailNotificationService(
                        emailNotificationRepository
                );
    }

    @Test
    void queueEmailShouldSavePendingNotification() {

        String eventKey = "LEAVE_REQUEST_CREATED:3";

        when(emailNotificationRepository.existsByEventKey(eventKey))
                .thenReturn(false);

        emailNotificationService.queueEmail(
                eventKey,
                "manager@example.com",
                "New Annual Leave Request",
                "An employee submitted annual leave."
        );

        ArgumentCaptor<EmailNotification> captor =
                ArgumentCaptor.forClass(EmailNotification.class);

        verify(emailNotificationRepository).save(
                captor.capture()
        );

        EmailNotification saved = captor.getValue();

        assertEquals(eventKey, saved.getEventKey());

        assertEquals(
                "manager@example.com",
                saved.getRecipientEmail()
        );

        assertEquals(
                "New Annual Leave Request",
                saved.getSubject()
        );

        assertEquals(
                "An employee submitted annual leave.",
                saved.getBody()
        );

        assertEquals(
                NotificationStatus.PENDING,
                saved.getStatus()
        );

        assertEquals(0, saved.getAttemptCount());
    }

    @Test
    void queueEmailShouldIgnoreExistingEventKey() {

        String eventKey = "LEAVE_REQUEST_CREATED:3";

        when(emailNotificationRepository.existsByEventKey(eventKey))
                .thenReturn(true);

        emailNotificationService.queueEmail(
                eventKey,
                "manager@example.com",
                "New Annual Leave Request",
                "An employee submitted annual leave."
        );

        verify(emailNotificationRepository, never())
                .save(any(EmailNotification.class));
    }

    @Test
    void queueEmailShouldRejectMissingEventKey() {

        assertThrows(
                IllegalArgumentException.class,
                () -> emailNotificationService.queueEmail(
                        "",
                        "manager@example.com",
                        "Test Subject",
                        "Test Body"
                )
        );

        verifyNoInteractions(emailNotificationRepository);
    }

    @Test
    void queueEmailShouldRejectMissingRecipient() {

        assertThrows(
                IllegalArgumentException.class,
                () -> emailNotificationService.queueEmail(
                        "TEST_EVENT:1",
                        "",
                        "Test Subject",
                        "Test Body"
                )
        );

        verifyNoInteractions(emailNotificationRepository);
    }

    @Test
    void queueEmailShouldRejectEmptySubject() {

        assertThrows(
                IllegalArgumentException.class,
                () -> emailNotificationService.queueEmail(
                        "TEST_EVENT:1",
                        "manager@example.com",
                        "",
                        "Test Body"
                )
        );

        verifyNoInteractions(emailNotificationRepository);
    }

    @Test
    void queueEmailShouldRejectEmptyBody() {

        assertThrows(
                IllegalArgumentException.class,
                () -> emailNotificationService.queueEmail(
                        "TEST_EVENT:1",
                        "manager@example.com",
                        "Test Subject",
                        ""
                )
        );

        verifyNoInteractions(emailNotificationRepository);
    }
}
