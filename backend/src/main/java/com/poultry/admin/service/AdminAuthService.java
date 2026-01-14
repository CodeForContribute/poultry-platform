package com.poultry.admin.service;

import com.poultry.admin.dto.AdminLoginRequest;
import com.poultry.admin.dto.AdminUserDto;
import com.poultry.admin.dto.CreateAdminRequest;
import com.poultry.admin.entity.AdminUser;
import com.poultry.admin.repository.AdminUserRepository;
import com.poultry.auth.dto.TokenResponse;
import com.poultry.auth.security.JwtService;
import com.poultry.auth.service.AuditService;
import com.poultry.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService
{

  @Transactional
  public TokenResponse login(AdminLoginRequest request, String ipAddress, String userAgent)
  {
    log.info("Admin login attempt for email: {}", request.getEmail());

    AdminUser admin = adminUserRepository.findByEmail(request.getEmail())
                                         .orElseThrow(() -> new BusinessException("Invalid credentials", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED));

    if (!admin.isActive())
    {
      if (admin.isLocked())
      {
        throw new BusinessException("Account is locked. Try again later.", "ACCOUNT_LOCKED", HttpStatus.FORBIDDEN);
      }
      throw new BusinessException("Account is not active", "ACCOUNT_INACTIVE", HttpStatus.FORBIDDEN);
    }

    if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash()))
    {
      admin.incrementFailedAttempts();
      adminUserRepository.save(admin);

      auditService.logLoginFailure("ADMIN", admin.getEmail(), "Invalid password");
      throw new BusinessException("Invalid credentials", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
    }

    // Successful login
    admin.resetFailedAttempts();
    admin.setLastLoginAt(Instant.now());
    adminUserRepository.save(admin);

    auditService.logLoginSuccess("ADMIN", admin.getId(), admin.getEmail());

    String accessToken = jwtService.generateAccessToken(admin.getId(), "ADMIN", admin.getRole().name(), Map.of());
    String refreshToken = jwtService.generateRefreshToken(admin.getId(), "ADMIN");

    return TokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenType("Bearer")
                        .expiresIn(jwtService.getAccessTokenExpirySeconds())
                        .userId(admin.getId())
                        .userType("ADMIN")
                        .name(admin.getName())
                        .email(admin.getEmail())
                        .role(admin.getRole().name())
                        .mustChangePassword(admin.getMustChangePassword())
                        .build();
  }

  @Transactional
  public AdminUserDto createAdmin(CreateAdminRequest request, UUID createdBy)
  {
    log.info("Creating new admin user: {} by admin: {}", request.getEmail(), createdBy);

    if (adminUserRepository.existsByEmail(request.getEmail()))
    {
      throw new BusinessException("Email already exists", "EMAIL_EXISTS", HttpStatus.CONFLICT);
    }

    AdminUser admin = AdminUser.builder()
                               .email(request.getEmail())
                               .name(request.getName())
                               .passwordHash(passwordEncoder.encode(request.getPassword()))
                               .role(request.getRole())
                               .passwordChangedAt(Instant.now())
                               .mustChangePassword(true)
                               .status(AdminUser.Status.ACTIVE)
                               .build();

    admin = adminUserRepository.save(admin);
    log.info("Created admin user: {} with role: {}", admin.getId(), admin.getRole());

    return AdminUserDto.fromEntity(admin);
  }

  @Transactional
  public void changePassword(UUID adminId, String currentPassword, String newPassword)
  {
    AdminUser admin = adminUserRepository.findById(adminId)
                                         .orElseThrow(() -> new BusinessException("Admin not found", "ADMIN_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash()))
    {
      throw new BusinessException("Current password is incorrect", "INVALID_PASSWORD", HttpStatus.BAD_REQUEST);
    }

    admin.setPasswordHash(passwordEncoder.encode(newPassword));
    admin.setPasswordChangedAt(Instant.now());
    admin.setMustChangePassword(false);
    adminUserRepository.save(admin);

    log.info("Password changed for admin: {}", adminId);
  }

  @Transactional
  public void updateAdminStatus(UUID adminId, AdminUser.Status status, UUID updatedBy)
  {
    AdminUser admin = adminUserRepository.findById(adminId)
                                         .orElseThrow(() -> new BusinessException("Admin not found", "ADMIN_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (admin.getRole() == AdminUser.AdminRole.SUPER_ADMIN && status != AdminUser.Status.ACTIVE)
    {
      throw new BusinessException("Cannot deactivate super admin", "CANNOT_DEACTIVATE_SUPER_ADMIN", HttpStatus.FORBIDDEN);
    }

    admin.setStatus(status);
    adminUserRepository.save(admin);

    log.info("Admin {} status changed to {} by {}", adminId, status, updatedBy);
  }

  public AdminUserDto getAdminById(UUID adminId)
  {
    AdminUser admin = adminUserRepository.findById(adminId)
                                         .orElseThrow(() -> new BusinessException("Admin not found", "ADMIN_NOT_FOUND", HttpStatus.NOT_FOUND));
    return AdminUserDto.fromEntity(admin);
  }
  private final AdminUserRepository adminUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuditService auditService;
}
