
package com.example.attendance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {

        emailService = new EmailService(
                mailSender,
                "no-reply@attendance.local"
        );
    }

    @Test
    void sendEmailShouldCreateCorrectMessage() {

        emailService.sendEmail(
                "manager@example.com",
                "Annual Leave Request",
                "An employee submitted a leave request."
        );

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(
                        SimpleMailMessage.class
                );

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals(
                "no-reply@attendance.local",
                message.getFrom()
        );

        assertArrayEquals(
                new String[]{"manager@example.com"},
                message.getTo()
        );

        assertEquals(
                "Annual Leave Request",
                message.getSubject()
        );

        assertEquals(
                "An employee submitted a leave request.",
                message.getText()
        );
    }

    @Test
    void sendEmailShouldRejectMissingRecipient() {

        assertThrows(
                IllegalArgumentException.class,
                () -> emailService.sendEmail(
                        "",
                        "Test Subject",
                        "Test Body"
                )
        );

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendEmailShouldRejectEmptySubject() {

        assertThrows(
                IllegalArgumentException.class,
                () -> emailService.sendEmail(
                        "manager@example.com",
                        "",
                        "Test Body"
                )
        );

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendEmailShouldRejectEmptyBody() {

        assertThrows(
                IllegalArgumentException.class,
                () -> emailService.sendEmail(
                        "manager@example.com",
                        "Test Subject",
                        ""
                )
        );

        verifyNoInteractions(mailSender);
    }
}
