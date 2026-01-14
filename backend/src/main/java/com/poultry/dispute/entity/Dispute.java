package com.poultry.dispute.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "disputes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute
{

  public enum RaisedBy
  {
    BUYER,
    SELLER
  }

  public enum DisputeType
  {
    QUALITY_ISSUE,
    QUANTITY_MISMATCH,
    DELIVERY_ISSUE,
    PAYMENT_ISSUE,
    WRONG_PRODUCT,
    DAMAGED_GOODS,
    OTHER
  }

  public enum DisputeStatus
  {
    OPEN,
    UNDER_REVIEW,
    AWAITING_RESPONSE,
    RESOLVED,
    ESCALATED,
    CLOSED
  }

  public enum DisputeResolution
  {
    REFUND_FULL,
    REFUND_PARTIAL,
    REPLACEMENT,
    CREDIT_NOTE,
    NO_ACTION,
    MUTUAL_AGREEMENT
  }

  public boolean isOpen()
  {
    return status == DisputeStatus.OPEN || status == DisputeStatus.UNDER_REVIEW
        || status == DisputeStatus.AWAITING_RESPONSE || status == DisputeStatus.ESCALATED;
  }

  public boolean canBeEscalated()
  {
    return status == DisputeStatus.OPEN || status == DisputeStatus.UNDER_REVIEW
        || status == DisputeStatus.AWAITING_RESPONSE;
  }

  public void addMessage(DisputeMessage message)
  {
    messages.add(message);
    message.setDispute(this);
  }
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(name = "dispute_number", nullable = false, unique = true)
  private String disputeNumber;
  @Column(name = "order_id", nullable = false)
  private UUID orderId;
  @Column(name = "buyer_id", nullable = false)
  private UUID buyerId;
  @Column(name = "seller_id", nullable = false)
  private UUID sellerId;
  @Enumerated(EnumType.STRING)
  @Column(name = "raised_by", nullable = false, columnDefinition = "dispute_raised_by")
  private RaisedBy raisedBy;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "dispute_type")
  private DisputeType type;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "dispute_status")
  @Builder.Default
  private DisputeStatus status = DisputeStatus.OPEN;
  @Column(nullable = false, length = 200)
  private String title;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "evidence_urls", columnDefinition = "jsonb")
  @Builder.Default
  private List<String> evidenceUrls = new ArrayList<>();
  @Enumerated(EnumType.STRING)
  @Column(name = "requested_resolution", columnDefinition = "dispute_resolution")
  private DisputeResolution requestedResolution;
  @Column(name = "requested_amount", precision = 12, scale = 2)
  private BigDecimal requestedAmount;
  @Enumerated(EnumType.STRING)
  @Column(name = "final_resolution", columnDefinition = "dispute_resolution")
  private DisputeResolution finalResolution;
  @Column(name = "resolution_amount", precision = 12, scale = 2)
  private BigDecimal resolutionAmount;
  @Column(name = "resolution_notes", columnDefinition = "TEXT")
  private String resolutionNotes;
  @Column(name = "assigned_to")
  private UUID assignedTo;
  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 2;
  @Column(name = "escalated_at")
  private Instant escalatedAt;
  @Column(name = "resolved_at")
  private Instant resolvedAt;
  @Column(name = "resolved_by")
  private UUID resolvedBy;
  @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<DisputeMessage> messages = new ArrayList<>();
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
