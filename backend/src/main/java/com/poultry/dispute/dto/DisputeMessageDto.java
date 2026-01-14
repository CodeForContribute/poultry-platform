package com.poultry.dispute.dto;

import com.poultry.dispute.entity.DisputeMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeMessageDto
{
  public static DisputeMessageDto fromEntity(DisputeMessage message)
  {
    return DisputeMessageDto.builder()
                            .id(message.getId())
                            .disputeId(message.getDispute().getId())
                            .senderType(message.getSenderType())
                            .senderId(message.getSenderId())
                            .message(message.getMessage())
                            .attachmentUrls(message.getAttachmentUrls())
                            .isInternal(message.getIsInternal())
                            .createdAt(message.getCreatedAt())
                            .build();
  }
  private UUID id;
  private UUID disputeId;
  private String senderType;
  private UUID senderId;
  private String senderName;
  private String message;
  private List<String> attachmentUrls;
  private Boolean isInternal;
  private Instant createdAt;
}
