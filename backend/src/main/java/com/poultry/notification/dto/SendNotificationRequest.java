package com.poultry.notification.dto;

import com.poultry.notification.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class SendNotificationRequest {
    private String templateCode;
    private String recipientType;
    private UUID recipientId;
    private String recipientAddress;
    private Notification.Channel channel;
    private Notification.Priority priority;
    private Map<String, String> variables;
    private Map<String, Object> dataPayload;
    private String referenceType;
    private UUID referenceId;
}
