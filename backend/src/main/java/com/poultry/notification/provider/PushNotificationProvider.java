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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationProvider {

    private final RestTemplate restTemplate;

    @Value("${external.firebase.server-key:}")
    private String firebaseServerKey;

    @Value("${external.firebase.project-id:}")
    private String firebaseProjectId;

    @Value("${notification.push.enabled:false}")
    private boolean pushEnabled;

    private static final String FCM_LEGACY_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String FCM_V1_URL = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    @CircuitBreaker(name = "firebase", fallbackMethod = "sendPushFallback")
    @Retry(name = "firebase")
    public void sendPushNotification(Notification notification) {
        if (!pushEnabled) {
            log.info("[PUSH DISABLED] Would send to {}: {} - {}",
                    notification.getRecipientAddress(),
                    notification.getSubject(),
                    notification.getBody());
            return;
        }

        String fcmToken = notification.getRecipientAddress();
        String title = notification.getSubject();
        String body = notification.getBody();
        Map<String, Object> data = notification.getDataPayload();

        sendViaFcmLegacy(fcmToken, title, body, data);
    }

    private void sendViaFcmLegacy(String fcmToken, String title, String body, Map<String, Object> data) {
        if (firebaseServerKey.isEmpty()) {
            log.warn("[FIREBASE NOT CONFIGURED] Push to {}: {} - {}", fcmToken, title, body);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=" + firebaseServerKey);

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("sound", "default");
        notification.put("badge", 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("to", fcmToken);
        payload.put("notification", notification);
        payload.put("priority", "high");

        if (data != null && !data.isEmpty()) {
            // Convert all values to strings for FCM data payload
            Map<String, String> stringData = new HashMap<>();
            data.forEach((k, v) -> stringData.put(k, String.valueOf(v)));
            payload.put("data", stringData);
        }

        // Android specific config
        Map<String, Object> android = new HashMap<>();
        android.put("priority", "high");
        android.put("notification", Map.of(
                "sound", "default",
                "click_action", "FLUTTER_NOTIFICATION_CLICK"
        ));
        payload.put("android", android);

        // iOS specific config
        Map<String, Object> apns = new HashMap<>();
        apns.put("payload", Map.of(
                "aps", Map.of(
                        "sound", "default",
                        "badge", 1
                )
        ));
        payload.put("apns", apns);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(FCM_LEGACY_URL, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer success = (Integer) response.getBody().get("success");
                Integer failure = (Integer) response.getBody().get("failure");

                if (success != null && success > 0) {
                    log.info("Push notification sent via FCM to token: {}...", fcmToken.substring(0, Math.min(20, fcmToken.length())));
                } else if (failure != null && failure > 0) {
                    log.warn("FCM push failed. Response: {}", response.getBody());
                    throw new RuntimeException("FCM push failed: " + response.getBody().get("results"));
                }
            } else {
                log.error("FCM push failed: {}", response.getBody());
                throw new RuntimeException("FCM push failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send push via FCM: {}", e.getMessage());
            throw e;
        }
    }

    public void sendPushFallback(Notification notification, Exception e) {
        log.error("Push notification sending failed after retries for {}: {}",
                notification.getRecipientAddress(), e.getMessage());
        throw new RuntimeException("Push notification service unavailable", e);
    }
}
