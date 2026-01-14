package com.poultry.notification.service;

import com.poultry.auth.entity.Buyer;
import com.poultry.auth.repository.BuyerRepository;
import com.poultry.common.exception.BusinessException;
import com.poultry.notification.dto.SendNotificationRequest;
import com.poultry.notification.entity.Notification;
import com.poultry.notification.entity.NotificationTemplate;
import com.poultry.notification.provider.EmailProvider;
import com.poultry.notification.provider.PushNotificationProvider;
import com.poultry.notification.provider.SmsProvider;
import com.poultry.notification.provider.WhatsAppProvider;
import com.poultry.notification.repository.NotificationRepository;
import com.poultry.notification.repository.NotificationTemplateRepository;
import com.poultry.order.entity.Order;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final SmsProvider smsProvider;
    private final EmailProvider emailProvider;
    private final PushNotificationProvider pushNotificationProvider;
    private final WhatsAppProvider whatsAppProvider;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    @Transactional
    public Notification sendNotification(SendNotificationRequest request) {
        NotificationTemplate template = templateRepository
                .findByCodeAndStatus(request.getTemplateCode(), NotificationTemplate.Status.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("NotificationTemplate", request.getTemplateCode()));

        String renderedBody = renderTemplate(template.getBodyTemplate(), request.getVariables());
        String renderedSubject = template.getSubject() != null
                ? renderTemplate(template.getSubject(), request.getVariables())
                : null;

        Notification notification = Notification.builder()
                .templateId(template.getId())
                .templateCode(template.getCode())
                .recipientType(request.getRecipientType())
                .recipientId(request.getRecipientId())
                .recipientAddress(request.getRecipientAddress())
                .channel(request.getChannel() != null ? request.getChannel()
                        : Notification.Channel.valueOf(template.getChannel().name()))
                .priority(request.getPriority() != null ? request.getPriority() : Notification.Priority.MEDIUM)
                .subject(renderedSubject)
                .body(renderedBody)
                .dataPayload(request.getDataPayload())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .status(Notification.Status.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        log.info("Notification created: id={}, template={}, recipient={}:{}",
                notification.getId(), request.getTemplateCode(),
                request.getRecipientType(), request.getRecipientId());

        // Trigger async sending
        sendAsync(notification.getId());

        return notification;
    }

    @Async("notificationExecutor")
    public void sendAsync(UUID notificationId) {
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> BusinessException.notFound("Notification", notificationId));

            sendToProvider(notification);

            notification.setStatus(Notification.Status.SENT);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);

            log.info("Notification sent: id={}, channel={}", notificationId, notification.getChannel());

        } catch (Exception e) {
            log.error("Failed to send notification: id={}", notificationId, e);
            handleSendFailure(notificationId, e);
        }
    }

    private void sendToProvider(Notification notification) {
        switch (notification.getChannel()) {
            case SMS -> sendSms(notification);
            case FCM -> sendPushNotification(notification);
            case EMAIL -> sendEmail(notification);
            case WHATSAPP -> sendWhatsApp(notification);
        }
    }

    private void sendSms(Notification notification) {
        smsProvider.sendSms(notification);
    }

    private void sendPushNotification(Notification notification) {
        pushNotificationProvider.sendPushNotification(notification);
    }

    private void sendEmail(Notification notification) {
        emailProvider.sendEmail(notification);
    }

    private void sendWhatsApp(Notification notification) {
        whatsAppProvider.sendWhatsApp(notification);
    }

    @Transactional
    public void handleSendFailure(UUID notificationId, Exception e) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) return;

        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setErrorMessage(e.getMessage());

        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            notification.setStatus(Notification.Status.FAILED);
            log.warn("Notification permanently failed after {} retries: id={}",
                    notification.getMaxRetries(), notificationId);
        } else {
            // Exponential backoff: 1min, 4min, 9min
            long delaySeconds = (long) Math.pow(notification.getRetryCount(), 2) * 60;
            notification.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            log.info("Notification retry scheduled: id={}, attempt={}, nextRetry={}",
                    notificationId, notification.getRetryCount(), notification.getNextRetryAt());
        }

        notificationRepository.save(notification);
    }

    private String renderTemplate(String template, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variables.getOrDefault(variableName, "{{" + variableName + "}}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Send payment success notification to buyer
     */
    @Transactional
    public void notifyPaymentSuccess(Order order, UUID paymentId, BigDecimal amount) {
        Buyer buyer = buyerRepository.findById(order.getBuyerId())
                .orElse(null);

        if (buyer == null) {
            log.warn("Buyer not found for payment notification: buyerId={}", order.getBuyerId());
            return;
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("order_number", order.getOrderNumber());
        variables.put("amount", amount.toPlainString());

        // Send SMS notification to buyer
        SendNotificationRequest smsRequest = SendNotificationRequest.builder()
                .templateCode("PAYMENT_SUCCESS_SMS_EN")
                .recipientType("BUYER")
                .recipientId(buyer.getId())
                .channel(Notification.Channel.SMS)
                .priority(Notification.Priority.HIGH)
                .variables(variables)
                .referenceType("PAYMENT")
                .referenceId(paymentId)
                .build();

        try {
            sendNotification(smsRequest);
        } catch (Exception e) {
            log.error("Failed to send payment success SMS to buyer: {}", buyer.getId(), e);
        }

        // Send push notification if FCM token available
        if (buyer.getFcmToken() != null) {
            SendNotificationRequest pushRequest = SendNotificationRequest.builder()
                    .templateCode("ORDER_PLACED_PUSH")
                    .recipientType("BUYER")
                    .recipientId(buyer.getId())
                    .recipientAddress(buyer.getFcmToken())
                    .channel(Notification.Channel.FCM)
                    .priority(Notification.Priority.HIGH)
                    .variables(variables)
                    .dataPayload(Map.of(
                            "type", "PAYMENT_SUCCESS",
                            "orderId", order.getId().toString(),
                            "paymentId", paymentId.toString()
                    ))
                    .referenceType("PAYMENT")
                    .referenceId(paymentId)
                    .build();

            try {
                sendNotification(pushRequest);
            } catch (Exception e) {
                log.error("Failed to send payment success push to buyer: {}", buyer.getId(), e);
            }
        }
    }

  /**
   * Send order notification (called from Kafka consumer)
   */
  @Async("notificationExecutor")
  public void sendOrderNotification(UUID recipientId, String recipientType, String notificationType,
                                    String orderNumber, BigDecimal amount)
  {
    log.info("Sending order notification: type={}, recipient={}:{}", notificationType, recipientType, recipientId);

    Map<String, String> variables = new HashMap<>();
    variables.put("order_number", orderNumber);
    variables.put("amount", amount != null ? amount.toPlainString() : "0");

    try
    {
      SendNotificationRequest request = SendNotificationRequest.builder()
                                                               .templateCode(notificationType + "_" + recipientType)
                                                               .recipientType(recipientType)
                                                               .recipientId(recipientId)
                                                               .channel(Notification.Channel.SMS)
                                                               .priority(Notification.Priority.HIGH)
                                                               .variables(variables)
                                                               .referenceType("ORDER")
                                                               .build();

      sendNotification(request);
    }
    catch (Exception e)
    {
      log.error("Failed to send order notification: {}", e.getMessage());
    }
  }

  /**
   * Send payment notification (called from Kafka consumer)
   */
  @Async("notificationExecutor")
  public void sendPaymentNotification(UUID buyerId, String notificationType, BigDecimal amount, String reference)
  {
    log.info("Sending payment notification: type={}, buyerId={}", notificationType, buyerId);

    Map<String, String> variables = new HashMap<>();
    variables.put("amount", amount != null ? amount.toPlainString() : "0");
    variables.put("reference", reference != null ? reference : "");

    try
    {
      SendNotificationRequest request = SendNotificationRequest.builder()
                                                               .templateCode(notificationType)
                                                               .recipientType("BUYER")
                                                               .recipientId(buyerId)
                                                               .channel(Notification.Channel.SMS)
                                                               .priority(Notification.Priority.HIGH)
                                                               .variables(variables)
                                                               .referenceType("PAYMENT")
                                                               .build();

      sendNotification(request);
    }
    catch (Exception e)
    {
      log.error("Failed to send payment notification: {}", e.getMessage());
    }
  }

  /**
   * Send delivery notification (called from Kafka consumer)
   */
  @Async("notificationExecutor")
  public void sendDeliveryNotification(UUID orderId, String notificationType, String status)
  {
    log.info("Sending delivery notification: type={}, orderId={}", notificationType, orderId);

    Map<String, String> variables = new HashMap<>();
    variables.put("status", status != null ? status : "");

    try
    {
      SendNotificationRequest request = SendNotificationRequest.builder()
                                                               .templateCode(notificationType)
                                                               .recipientType("BUYER")
                                                               .recipientId(orderId) // This would need to be resolved to buyer ID
                                                               .channel(Notification.Channel.SMS)
                                                               .priority(Notification.Priority.MEDIUM)
                                                               .variables(variables)
                                                               .referenceType("DELIVERY")
                                                               .referenceId(orderId)
                                                               .build();

      sendNotification(request);
    }
    catch (Exception e)
    {
      log.error("Failed to send delivery notification: {}", e.getMessage());
    }
  }

  /**
   * Send settlement notification (called from Kafka consumer)
   */
  @Async("notificationExecutor")
  public void sendSettlementNotification(UUID sellerId, String notificationType, BigDecimal amount)
  {
    log.info("Sending settlement notification: type={}, sellerId={}", notificationType, sellerId);

    Map<String, String> variables = new HashMap<>();
    variables.put("amount", amount != null ? amount.toPlainString() : "0");

    try
    {
      SendNotificationRequest request = SendNotificationRequest.builder()
                                                               .templateCode(notificationType)
                                                               .recipientType("SELLER")
                                                               .recipientId(sellerId)
                                                               .channel(Notification.Channel.SMS)
                                                               .priority(Notification.Priority.HIGH)
                                                               .variables(variables)
                                                               .referenceType("SETTLEMENT")
                                                               .build();

      sendNotification(request);
    }
    catch (Exception e)
    {
      log.error("Failed to send settlement notification: {}", e.getMessage());
    }
  }

  /**
     * Notify seller about new paid order
     */
    @Transactional
    public void notifySellerOrderPaid(Order order, UUID paymentId, BigDecimal amount) {
        Seller seller = sellerRepository.findById(order.getSellerId())
                .orElse(null);

        if (seller == null) {
            log.warn("Seller not found for payment notification: sellerId={}", order.getSellerId());
            return;
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("order_number", order.getOrderNumber());
        variables.put("amount", amount.toPlainString());

        // Send email notification to seller
        if (seller.getEmail() != null) {
            SendNotificationRequest emailRequest = SendNotificationRequest.builder()
                    .templateCode("NEW_ORDER_SELLER_SMS")
                    .recipientType("SELLER")
                    .recipientId(seller.getId())
                    .recipientAddress(seller.getEmail())
                    .channel(Notification.Channel.EMAIL)
                    .priority(Notification.Priority.HIGH)
                    .variables(variables)
                    .referenceType("PAYMENT")
                    .referenceId(paymentId)
                    .build();

            try {
                sendNotification(emailRequest);
            } catch (Exception e) {
                log.error("Failed to send payment notification email to seller: {}", seller.getId(), e);
            }
        }
    }
}
