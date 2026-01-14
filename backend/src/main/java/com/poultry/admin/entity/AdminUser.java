package com.poultry.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser
{

  public enum AdminRole
  {
    SUPER_ADMIN,
    OPERATIONS,
    FINANCE,
    SUPPORT
  }

  public enum Status
  {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION
  }

  public boolean isLocked()
  {
    return lockedUntil != null && Instant.now().isBefore(lockedUntil);
  }

  public boolean isActive()
  {
    return status == Status.ACTIVE && !isLocked();
  }

  public void incrementFailedAttempts()
  {
    this.failedLoginAttempts++;
    if (this.failedLoginAttempts >= 5)
    {
      this.lockedUntil = Instant.now().plusSeconds(1800); // 30 minutes
    }
  }

  public void resetFailedAttempts()
  {
    this.failedLoginAttempts = 0;
    this.lockedUntil = null;
  }

  public boolean hasPermission(String permission)
  {
    return switch (role)
    {
      case SUPER_ADMIN -> true;
      case OPERATIONS -> permission.startsWith("seller") || permission.startsWith("order") ||
          permission.startsWith("delivery") || permission.startsWith("verification");
      case FINANCE -> permission.startsWith("settlement") || permission.startsWith("payment") ||
          permission.startsWith("ledger") || permission.startsWith("report");
      case SUPPORT -> permission.startsWith("buyer") || permission.startsWith("order.view") ||
          permission.startsWith("seller.view");
    };
  }
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(nullable = false, unique = true)
  private String email;
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;
  @Column(nullable = false)
  private String name;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AdminRole role;
  @Column(name = "password_changed_at", nullable = false)
  private Instant passwordChangedAt;
  @Column(name = "must_change_password", nullable = false)
  @Builder.Default
  private Boolean mustChangePassword = false;
  @Column(name = "failed_login_attempts", nullable = false)
  @Builder.Default
  private Integer failedLoginAttempts = 0;
  @Column(name = "locked_until")
  private Instant lockedUntil;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private Status status = Status.ACTIVE;
  @Column(name = "last_login_at")
  private Instant lastLoginAt;
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
