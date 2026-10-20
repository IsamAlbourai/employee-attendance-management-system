
package com.example.attendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_notifications",
        indexes = {
                @Index(
                        name = "idx_email_notification_status",
                        columnList = "status, next_attempt_at"
                ),
                @Index(
                        name = "idx_email_notification_created",
                        columnList = "created_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_email_notification_event",
                        columnNames = "event_key"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class EmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_key",
            nullable = false,
            length = 200,
            unique = true
    )
    private String eventKey;

    @Column(
            name = "recipient_email",
            nullable = false,
            length = 255
    )
    private String recipientEmail;

    @Column(
            name = "subject",
            nullable = false,
            length = 255
    )
    private String subject;

    @Column(
            name = "body",
            nullable = false,
            length = 4000
    )
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private NotificationStatus status =
            NotificationStatus.PENDING;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private int attemptCount = 0;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(
            name = "last_error",
            length = 1000
    )
    private String lastError;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt;
        }
    }
}
