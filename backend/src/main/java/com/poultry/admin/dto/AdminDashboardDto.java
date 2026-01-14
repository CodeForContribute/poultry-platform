package com.poultry.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto
{

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentActivity
  {
    private String type;
    private String description;
    private String timestamp;
    private String entityId;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Alert
  {
    private String severity; // INFO, WARNING, CRITICAL
    private String title;
    private String message;
    private String actionUrl;
  }
  // Overview metrics
  private Long totalSellers;
  private Long activeSellers;
  private Long pendingVerificationSellers;
  private Long totalBuyers;
  private Long activeBuyers;
  // Order metrics (today)
  private Long todayOrders;
  private BigDecimal todayRevenue;
  private BigDecimal todayPlatformFees;
  // Order metrics (this month)
  private Long monthOrders;
  private BigDecimal monthRevenue;
  private BigDecimal monthPlatformFees;
  // Order status breakdown
  private Map<String, Long> ordersByStatus;
  // Pending actions
  private Long pendingVerifications;
  private Long pendingSettlements;
  private Long disputedOrders;
  private Long failedDeliveries;
  // Recent activity
  private List<RecentActivity> recentActivities;
  // Alerts
  private List<Alert> alerts;
}
