package com.poultry.dispute.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispute_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeHistory
{

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "dispute_id", nullable = false)
  private UUID disputeId;

  @Column(nullable = false, length = 100)
  private String action;

  @Enumerated(EnumType.STRING)
  @Column(name = "old_status", columnDefinition = "dispute_status")
  private Dispute.DisputeStatus oldStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", columnDefinition = "dispute_status")
  private Dispute.DisputeStatus newStatus;

  @Column(name = "performed_by")
  private UUID performedBy;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
