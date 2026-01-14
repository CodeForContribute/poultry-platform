package com.poultry.dispute.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dispute_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeMessage
{

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dispute_id", nullable = false)
  private Dispute dispute;

  @Column(name = "sender_type", nullable = false, length = 20)
  private String senderType; // BUYER, SELLER, ADMIN

  @Column(name = "sender_id", nullable = false)
  private UUID senderId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String message;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "attachment_urls", columnDefinition = "jsonb")
  @Builder.Default
  private List<String> attachmentUrls = new ArrayList<>();

  @Column(name = "is_internal", nullable = false)
  @Builder.Default
  private Boolean isInternal = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
