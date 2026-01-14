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
public class BuyerListDto
{
  private UUID id;
  private String name;
  private String phone;
  private String businessName;
  private String city;
  private String status;
  private Long totalOrders;
  private BigDecimal totalSpent;
  private Instant lastOrderAt;
  private Instant createdAt;
}
