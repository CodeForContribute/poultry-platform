package com.poultry.notification.repository;

import com.poultry.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByCode(String code);

    Optional<NotificationTemplate> findByCodeAndStatus(String code, NotificationTemplate.Status status);
}
