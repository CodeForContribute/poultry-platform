package com.poultry.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSellerDto {
    private UUID sellerId;
    private String businessName;
    private Long totalOrders;
    private Long completedOrders;
    private BigDecimal totalRevenue;
    private BigDecimal platformFees;
    private BigDecimal averageOrderValue;
    private Long uniqueCustomers;
    private BigDecimal orderCompletionRate;
    private Integer rank;
}
