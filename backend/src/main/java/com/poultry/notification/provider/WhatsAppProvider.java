package com.poultry.notification.provider;

import com.poultry.notification.entity.Notification;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppProvider {

    private final RestTemplate restTemplate;

    @Value("${external.whatsapp.phone-number-id:}")
    private String whatsappPhoneNumberId;

    @Value("${external.whatsapp.access-token:}")
    private String whatsappAccessToken;

    @Value("${external.whatsapp.api-version:v18.0}")
    private String whatsappApiVersion;

    @Value("${notification.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    private static final String WHATSAPP_API_URL = "https://graph.facebook.com/%s/%s/messages";

    @CircuitBreaker(name = "whatsapp", fallbackMethod = "sendWhatsAppFallback")
    @Retry(name = "whatsapp")
    public void sendWhatsApp(Notification notification) {
        if (!whatsappEnabled) {
            log.info("[WHATSAPP DISABLED] Would send to {}: {}",
                    notification.getRecipientAddress(),
                    notification.getBody());
            return;
        }

        String phoneNumber = notification.getRecipientAddress();
        String message = notification.getBody();

        sendViaWhatsAppBusinessApi(phoneNumber, message);
    }

    private void sendViaWhatsAppBusinessApi(String phoneNumber, String message) {
        if (whatsappAccessToken.isEmpty() || whatsappPhoneNumberId.isEmpty()) {
            log.warn("[WHATSAPP NOT CONFIGURED] WhatsApp to {}: {}", phoneNumber, message);
            return;
        }

        String url = String.format(WHATSAPP_API_URL, whatsappApiVersion, whatsappPhoneNumberId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(whatsappAccessToken);

        // Format phone number (remove any non-numeric characters, add country code if missing)
        String formattedPhone = formatPhoneNumber(phoneNumber);

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", formattedPhone);
        payload.put("type", "text");
        payload.put("text", Map.of("body", message));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");

                if (messages != null && !messages.isEmpty()) {
                    String messageId = (String) messages.get(0).get("id");
                    log.info("WhatsApp message sent to {}, message_id: {}", phoneNumber, messageId);
                } else {
                    log.warn("WhatsApp message sent but no message_id returned: {}", body);
                }
            } else {
                log.error("WhatsApp API failed: {}", response.getBody());
                throw new RuntimeException("WhatsApp API failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", phoneNumber, e.getMessage());
            throw e;
        }
    }

    public void sendWhatsAppWithTemplate(String phoneNumber, String templateName, String languageCode,
                                          List<Map<String, Object>> components) {
        if (!whatsappEnabled || whatsappAccessToken.isEmpty() || whatsappPhoneNumberId.isEmpty()) {
            log.warn("[WHATSAPP NOT CONFIGURED] Template {} to {}", templateName, phoneNumber);
            return;
        }

        String url = String.format(WHATSAPP_API_URL, whatsappApiVersion, whatsappPhoneNumberId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(whatsappAccessToken);

        String formattedPhone = formatPhoneNumber(phoneNumber);

        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", languageCode));
        if (components != null && !components.isEmpty()) {
            template.put("components", components);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", formattedPhone);
        payload.put("type", "template");
        payload.put("template", template);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp template message sent to {}", phoneNumber);
            } else {
                log.error("WhatsApp template API failed: {}", response.getBody());
                throw new RuntimeException("WhatsApp template API failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send WhatsApp template to {}: {}", phoneNumber, e.getMessage());
            throw e;
        }
    }

    public void sendWhatsAppFallback(Notification notification, Exception e) {
        log.error("WhatsApp sending failed after retries for {}: {}",
                notification.getRecipientAddress(), e.getMessage());
        throw new RuntimeException("WhatsApp service unavailable", e);
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove all non-numeric characters
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");

        // Add India country code if not present (assuming India as default)
        if (cleaned.length() == 10) {
            cleaned = "91" + cleaned;
        } else if (cleaned.startsWith("0")) {
            cleaned = "91" + cleaned.substring(1);
        }

        return cleaned;
    }
}
