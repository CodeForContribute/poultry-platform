package com.poultry.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardDto {

    private long totalProducts;
    private long activeProducts;
    private long pendingOrders;
    private long todayOrders;
    private BigDecimal todayRevenue;
    private BigDecimal totalRevenue;
}
