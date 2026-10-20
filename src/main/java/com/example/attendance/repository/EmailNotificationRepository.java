
package com.example.attendance.repository;

import com.example.attendance.entity.EmailNotification;
import com.example.attendance.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EmailNotificationRepository
        extends JpaRepository<EmailNotification, Long> {

    boolean existsByEventKey(String eventKey);

    List<EmailNotification>
    findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<NotificationStatus> statuses,
            LocalDateTime currentTime
    );

    @Modifying
    @Query("""
            UPDATE EmailNotification n
            SET n.status = :processingStatus,
                n.attemptCount = n.attemptCount + 1,
                n.lastAttemptAt = :currentTime,
                n.version = n.version + 1
            WHERE n.id = :notificationId
              AND n.status IN :eligibleStatuses
              AND n.nextAttemptAt <= :currentTime
            """)
    int claimNotification(
            @Param("notificationId") Long notificationId,
            @Param("processingStatus")
            NotificationStatus processingStatus,
            @Param("eligibleStatuses")
            Collection<NotificationStatus> eligibleStatuses,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Modifying
    @Query("""
            UPDATE EmailNotification n
            SET n.status = :failedStatus,
                n.nextAttemptAt = :currentTime,
                n.lastError = :errorMessage,
                n.version = n.version + 1
            WHERE n.status = :processingStatus
              AND n.lastAttemptAt < :cutoffTime
              AND n.attemptCount < :maxAttempts
            """)
    int recoverRetryableNotifications(
            @Param("processingStatus")
            NotificationStatus processingStatus,
            @Param("failedStatus")
            NotificationStatus failedStatus,
            @Param("cutoffTime") LocalDateTime cutoffTime,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("maxAttempts") int maxAttempts,
            @Param("errorMessage") String errorMessage
    );

    @Modifying
    @Query("""
            UPDATE EmailNotification n
            SET n.status = :failedStatus,
                n.nextAttemptAt = null,
                n.lastError = :errorMessage,
                n.version = n.version + 1
            WHERE n.status = :processingStatus
              AND n.lastAttemptAt < :cutoffTime
              AND n.attemptCount >= :maxAttempts
            """)
    int recoverExhaustedNotifications(
            @Param("processingStatus")
            NotificationStatus processingStatus,
            @Param("failedStatus")
            NotificationStatus failedStatus,
            @Param("cutoffTime") LocalDateTime cutoffTime,
            @Param("maxAttempts") int maxAttempts,
            @Param("errorMessage") String errorMessage
    );
}
