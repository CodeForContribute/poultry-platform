package com.poultry.auth.service;

import com.poultry.auth.entity.AuditLog;
import com.poultry.auth.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void logLoginSuccess(String userType, UUID userId, String email) {
        createAuditLog(userType, userId, AuditLog.AuditAction.LOGIN_SUCCESS, "SUCCESS",
                Map.of("email", maskEmail(email)));
    }

    @Async
    public void logLoginFailure(String userType, String email, String reason) {
        createAuditLog(userType, null, AuditLog.AuditAction.LOGIN_FAILED, "FAILURE",
                Map.of("email", maskEmail(email), "reason", reason));
    }

    @Async
    public void logLogout(String userType, UUID userId) {
        createAuditLog(userType, userId, AuditLog.AuditAction.LOGOUT, "SUCCESS", null);
    }

    @Async
    public void logPasswordChange(String userType, UUID userId, boolean forced) {
        createAuditLog(userType, userId, AuditLog.AuditAction.PASSWORD_CHANGE, "SUCCESS",
                Map.of("forced", forced));
    }

    @Async
    public void logOtpRequest(String phone, String purpose) {
        createAuditLog("BUYER", null, AuditLog.AuditAction.OTP_REQUEST, "SUCCESS",
                Map.of("phone", maskPhone(phone), "purpose", purpose));
    }

    @Async
    public void logOtpVerify(UUID buyerId, String phone, boolean success, String reason) {
        createAuditLog("BUYER", buyerId, AuditLog.AuditAction.OTP_VERIFY,
                success ? "SUCCESS" : "FAILURE",
                Map.of("phone", maskPhone(phone), "reason", reason != null ? reason : ""));
    }

    private void createAuditLog(String userType, UUID userId, AuditLog.AuditAction action,
                                String outcome, Map<String, Object> details) {
        try {
            HttpServletRequest request = getCurrentRequest();

            AuditLog auditLog = AuditLog.builder()
                    .userType(userType)
                    .userId(userId)
                    .action(action)
                    .outcome(outcome)
                    .ipAddress(getClientIp(request))
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .deviceInfo(extractDeviceInfo(request))
                    .details(details)
                    .correlationId(getCorrelationId(request))
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private String getCorrelationId(HttpServletRequest request) {
        if (request == null) return UUID.randomUUID().toString();
        Object correlationId = request.getAttribute("correlationId");
        return correlationId != null ? correlationId.toString() : UUID.randomUUID().toString();
    }

    private Map<String, Object> extractDeviceInfo(HttpServletRequest request) {
        if (request == null) return null;

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("userAgent", request.getHeader("User-Agent"));
        deviceInfo.put("acceptLanguage", request.getHeader("Accept-Language"));
        return deviceInfo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****@****.com";
        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 2) return "**" + domain;
        return localPart.substring(0, 2) + "****" + domain;
    }
}
