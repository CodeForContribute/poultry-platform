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

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailProvider {

    private final RestTemplate restTemplate;

    @Value("${external.sendgrid.api-key:}")
    private String sendgridApiKey;

    @Value("${external.sendgrid.from-email:noreply@poultry-platform.com}")
    private String sendgridFromEmail;

    @Value("${external.sendgrid.from-name:Poultry Platform}")
    private String sendgridFromName;

    @Value("${external.aws.ses.access-key:}")
    private String awsAccessKey;

    @Value("${external.aws.ses.secret-key:}")
    private String awsSecretKey;

    @Value("${external.aws.ses.region:ap-south-1}")
    private String awsRegion;

    @Value("${external.aws.ses.from-email:}")
    private String sesFromEmail;

    @Value("${notification.email.provider:sendgrid}")
    private String emailProvider;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @CircuitBreaker(name = "sendgrid", fallbackMethod = "sendEmailFallback")
    @Retry(name = "sendgrid")
    public void sendEmail(Notification notification) {
        if (!emailEnabled) {
            log.info("[EMAIL DISABLED] Would send to {}: {} - {}",
                    notification.getRecipientAddress(),
                    notification.getSubject(),
                    notification.getBody());
            return;
        }

        String toEmail = notification.getRecipientAddress();
        String subject = notification.getSubject();
        String body = notification.getBody();

        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            sendViaSendGrid(toEmail, subject, body);
        } else if ("ses".equalsIgnoreCase(emailProvider)) {
            sendViaAwsSes(toEmail, subject, body);
        } else {
            log.warn("Unknown email provider: {}, defaulting to SendGrid", emailProvider);
            sendViaSendGrid(toEmail, subject, body);
        }
    }

    private void sendViaSendGrid(String toEmail, String subject, String body) {
        if (sendgridApiKey.isEmpty()) {
            log.warn("[SENDGRID NOT CONFIGURED] Email to {}: {} - {}", toEmail, subject, body);
            return;
        }

        String url = "https://api.sendgrid.com/v3/mail/send";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sendgridApiKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("personalizations", List.of(Map.of(
                "to", List.of(Map.of("email", toEmail))
        )));
        payload.put("from", Map.of(
                "email", sendgridFromEmail,
                "name", sendgridFromName
        ));
        payload.put("subject", subject);
        payload.put("content", List.of(Map.of(
                "type", "text/html",
                "value", wrapInHtmlTemplate(body)
        )));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent via SendGrid to {}", toEmail);
            } else {
                log.error("SendGrid email failed: {}", response.getBody());
                throw new RuntimeException("SendGrid email failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email via SendGrid to {}: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    private void sendViaAwsSes(String toEmail, String subject, String body) {
        if (awsAccessKey.isEmpty() || awsSecretKey.isEmpty()) {
            log.warn("[AWS SES NOT CONFIGURED] Email to {}: {} - {}", toEmail, subject, body);
            return;
        }

        // AWS SES using simple email sending via REST API
        // In production, consider using AWS SDK for better reliability
        String url = String.format("https://email.%s.amazonaws.com/", awsRegion);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // Note: In production, implement proper AWS Signature Version 4 signing

        String requestBody = String.format(
                "Action=SendEmail&Source=%s&Destination.ToAddresses.member.1=%s&Message.Subject.Data=%s&Message.Body.Html.Data=%s",
                encodeUrlParam(sesFromEmail.isEmpty() ? sendgridFromEmail : sesFromEmail),
                encodeUrlParam(toEmail),
                encodeUrlParam(subject),
                encodeUrlParam(wrapInHtmlTemplate(body))
        );

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent via AWS SES to {}", toEmail);
            } else {
                log.error("AWS SES email failed: {}", response.getBody());
                throw new RuntimeException("AWS SES email failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email via AWS SES to {}: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    public void sendEmailFallback(Notification notification, Exception e) {
        log.error("Email sending failed after retries for {}: {}", notification.getRecipientAddress(), e.getMessage());
        throw new RuntimeException("Email service unavailable", e);
    }

    private String wrapInHtmlTemplate(String body) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f9f9f9; }
                    .footer { padding: 10px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Poultry Platform</h1>
                    </div>
                    <div class="content">
                        %s
                    </div>
                    <div class="footer">
                        <p>This is an automated message from Poultry Platform.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(body);
    }

    private String encodeUrlParam(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
