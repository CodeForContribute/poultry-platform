package com.poultry.dispute.dto;

import com.poultry.dispute.entity.Dispute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeDto
{
  public static DisputeDto fromEntity(Dispute dispute)
  {
    return DisputeDto.builder()
                     .id(dispute.getId())
                     .disputeNumber(dispute.getDisputeNumber())
                     .orderId(dispute.getOrderId())
                     .buyerId(dispute.getBuyerId())
                     .sellerId(dispute.getSellerId())
                     .raisedBy(dispute.getRaisedBy().name())
                     .type(dispute.getType().name())
                     .status(dispute.getStatus().name())
                     .title(dispute.getTitle())
                     .description(dispute.getDescription())
                     .evidenceUrls(dispute.getEvidenceUrls())
                     .requestedResolution(dispute.getRequestedResolution() != null ? dispute.getRequestedResolution().name() : null)
                     .requestedAmount(dispute.getRequestedAmount())
                     .finalResolution(dispute.getFinalResolution() != null ? dispute.getFinalResolution().name() : null)
                     .resolutionAmount(dispute.getResolutionAmount())
                     .resolutionNotes(dispute.getResolutionNotes())
                     .assignedTo(dispute.getAssignedTo())
                     .priority(dispute.getPriority())
                     .escalatedAt(dispute.getEscalatedAt())
                     .resolvedAt(dispute.getResolvedAt())
                     .resolvedBy(dispute.getResolvedBy())
                     .createdAt(dispute.getCreatedAt())
                     .updatedAt(dispute.getUpdatedAt())
                     .build();
  }
  private UUID id;
  private String disputeNumber;
  private UUID orderId;
  private String orderNumber;
  private UUID buyerId;
  private String buyerName;
  private UUID sellerId;
  private String sellerName;
  private String raisedBy;
  private String type;
  private String status;
  private String title;
  private String description;
  private List<String> evidenceUrls;
  private String requestedResolution;
  private BigDecimal requestedAmount;
  private String finalResolution;
  private BigDecimal resolutionAmount;
  private String resolutionNotes;
  private UUID assignedTo;
  private String assignedToName;
  private Integer priority;
  private Instant escalatedAt;
  private Instant resolvedAt;
  private UUID resolvedBy;
  private int messageCount;
  private Instant createdAt;
  private Instant updatedAt;
}
