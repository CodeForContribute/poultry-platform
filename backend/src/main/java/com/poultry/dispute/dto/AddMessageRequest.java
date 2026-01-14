package com.poultry.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMessageRequest
{

  @NotBlank(message = "Message is required")
  private String message;

  private List<String> attachmentUrls;

  private Boolean isInternal;
}
