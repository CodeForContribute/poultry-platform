package com.poultry.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerListDto
{
  private UUID id;
  private String businessName;
  private String gstin;
  private String email;
  private String phone;
  private String city;
  private String status;
  private String verificationStatus;
  private BigDecimal platformFeePercent;
  private Long totalOrders;
  private BigDecimal totalRevenue;
  private Instant createdAt;
}
