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
public class OrderListDto
{
  private UUID id;
  private String orderNumber;
  private UUID sellerId;
  private String sellerName;
  private UUID buyerId;
  private String buyerName;
  private String buyerPhone;
  private String status;
  private String orderType;
  private BigDecimal totalAmount;
  private BigDecimal platformFee;
  private String deliveryStatus;
  private String paymentStatus;
  private Instant createdAt;
  private Instant updatedAt;
}
