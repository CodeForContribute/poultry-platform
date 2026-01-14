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
public class SmsProvider {

    private final RestTemplate restTemplate;

    @Value("${external.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${external.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${external.twilio.phone-number:}")
    private String twilioPhoneNumber;

    @Value("${external.msg91.auth-key:}")
    private String msg91AuthKey;

    @Value("${external.msg91.sender-id:PLTRY}")
    private String msg91SenderId;

    @Value("${external.msg91.template-id:}")
    private String msg91TemplateId;

    @Value("${notification.sms.provider:twilio}")
    private String smsProvider;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @CircuitBreaker(name = "msg91", fallbackMethod = "sendSmsFallback")
    @Retry(name = "msg91")
    public void sendSms(Notification notification) {
        if (!smsEnabled) {
            log.info("[SMS DISABLED] Would send to {}: {}", notification.getRecipientAddress(), notification.getBody());
            return;
        }

        String phoneNumber = notification.getRecipientAddress();
        String message = notification.getBody();

        if ("twilio".equalsIgnoreCase(smsProvider)) {
            sendViaTwilio(phoneNumber, message);
        } else if ("msg91".equalsIgnoreCase(smsProvider)) {
            sendViaMsg91(phoneNumber, message);
        } else {
            log.warn("Unknown SMS provider: {}, defaulting to Twilio", smsProvider);
            sendViaTwilio(phoneNumber, message);
        }
    }

    private void sendViaTwilio(String phoneNumber, String message) {
        if (twilioAccountSid.isEmpty() || twilioAuthToken.isEmpty()) {
            log.warn("[TWILIO NOT CONFIGURED] SMS to {}: {}", phoneNumber, message);
            return;
        }

        String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", twilioAccountSid);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(twilioAccountSid, twilioAuthToken);

        String body = String.format("To=%s&From=%s&Body=%s",
                encodeUrlParam(phoneNumber),
                encodeUrlParam(twilioPhoneNumber),
                encodeUrlParam(message));

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent via Twilio to {}, SID: {}", phoneNumber, response.getBody().get("sid"));
            } else {
                log.error("Twilio SMS failed: {}", response.getBody());
                throw new RuntimeException("Twilio SMS failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send SMS via Twilio to {}: {}", phoneNumber, e.getMessage());
            throw e;
        }
    }

    private void sendViaMsg91(String phoneNumber, String message) {
        if (msg91AuthKey.isEmpty()) {
            log.warn("[MSG91 NOT CONFIGURED] SMS to {}: {}", phoneNumber, message);
            return;
        }

        String url = "https://control.msg91.com/api/v5/flow/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authkey", msg91AuthKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("template_id", msg91TemplateId);
        payload.put("sender", msg91SenderId);
        payload.put("short_url", "0");
        payload.put("recipients", List.of(Map.of(
                "mobiles", phoneNumber.replaceAll("[^0-9]", ""),
                "message", message
        )));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent via MSG91 to {}, response: {}", phoneNumber, response.getBody());
            } else {
                log.error("MSG91 SMS failed: {}", response.getBody());
                throw new RuntimeException("MSG91 SMS failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send SMS via MSG91 to {}: {}", phoneNumber, e.getMessage());
            throw e;
        }
    }

    public void sendSmsFallback(Notification notification, Exception e) {
        log.error("SMS sending failed after retries for {}: {}", notification.getRecipientAddress(), e.getMessage());
        throw new RuntimeException("SMS service unavailable", e);
    }

    private String encodeUrlParam(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
