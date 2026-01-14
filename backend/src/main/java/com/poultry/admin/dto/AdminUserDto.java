package com.poultry.admin.dto;

import com.poultry.admin.entity.AdminUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto
{
  public static AdminUserDto fromEntity(AdminUser admin)
  {
    return AdminUserDto.builder()
                       .id(admin.getId())
                       .email(admin.getEmail())
                       .name(admin.getName())
                       .role(admin.getRole())
                       .status(admin.getStatus())
                       .lastLoginAt(admin.getLastLoginAt())
                       .createdAt(admin.getCreatedAt())
                       .mustChangePassword(admin.getMustChangePassword())
                       .build();
  }
  private UUID id;
  private String email;
  private String name;
  private AdminUser.AdminRole role;
  private AdminUser.Status status;
  private Instant lastLoginAt;
  private Instant createdAt;
  private Boolean mustChangePassword;
}
