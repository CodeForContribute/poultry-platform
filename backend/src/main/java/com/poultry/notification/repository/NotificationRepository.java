package com.poultry.notification.repository;

import com.poultry.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
            String recipientType, UUID recipientId);

    List<Notification> findByStatusOrderByPriorityDescCreatedAtAsc(Notification.Status status);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < n.maxRetries " +
           "AND n.nextRetryAt <= :now ORDER BY n.priority DESC, n.createdAt ASC")
    List<Notification> findRetryableNotifications(
            @Param("status") Notification.Status status, @Param("now") Instant now);

    List<Notification> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
