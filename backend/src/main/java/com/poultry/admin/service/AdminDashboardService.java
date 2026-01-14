package com.poultry.admin.service;

import com.poultry.admin.dto.AdminDashboardDto;
import com.poultry.analytics.dto.DateRangeRequest;
import com.poultry.analytics.repository.AnalyticsRepository;
import com.poultry.auth.entity.Buyer;
import com.poultry.auth.repository.BuyerRepository;
import com.poultry.delivery.repository.DeliveryRepository;
import com.poultry.dispute.repository.DisputeRepository;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.SellerRepository;
import com.poultry.settlement.repository.SettlementRepository;
import com.poultry.verification.repository.SellerVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService
{

  @Transactional(readOnly = true)
  @Cacheable(value = "adminDashboard", key = "'dashboard'", unless = "#result == null")
  public AdminDashboardDto getDashboard()
  {
    log.info("Fetching admin dashboard data");

    // Date ranges
    DateRangeRequest today = DateRangeRequest.today();
    DateRangeRequest thisMonth = DateRangeRequest.thisMonth();

    Instant todayStart = today.getStartInstant();
    Instant todayEnd = today.getEndInstant();
    Instant monthStart = thisMonth.getStartInstant();
    Instant monthEnd = thisMonth.getEndInstant();

    // Seller metrics
    Long totalSellers = analyticsRepository.countActiveSellers() + countInactiveSellers();
    Long activeSellers = analyticsRepository.countActiveSellers();
    Long pendingVerificationSellers = countPendingVerificationSellers();

    // Buyer metrics
    Long totalBuyers = analyticsRepository.countActiveBuyers() + countInactiveBuyers();
    Long activeBuyers = analyticsRepository.countActiveBuyers();

    // Today's metrics
    Long todayOrders = analyticsRepository.countOrdersInPeriod(todayStart, todayEnd);
    BigDecimal todayRevenue = Optional.ofNullable(analyticsRepository.sumRevenueInPeriod(todayStart, todayEnd))
                                      .orElse(BigDecimal.ZERO);
    BigDecimal todayPlatformFees = Optional.ofNullable(analyticsRepository.sumPlatformFeesInPeriod(todayStart, todayEnd))
                                           .orElse(BigDecimal.ZERO);

    // Month metrics
    Long monthOrders = analyticsRepository.countOrdersInPeriod(monthStart, monthEnd);
    BigDecimal monthRevenue = Optional.ofNullable(analyticsRepository.sumRevenueInPeriod(monthStart, monthEnd))
                                      .orElse(BigDecimal.ZERO);
    BigDecimal monthPlatformFees = Optional.ofNullable(analyticsRepository.sumPlatformFeesInPeriod(monthStart, monthEnd))
                                           .orElse(BigDecimal.ZERO);

    // Order status breakdown
    Map<String, Long> ordersByStatus = analyticsRepository.countOrdersByStatus(monthStart, monthEnd)
                                                          .stream()
                                                          .collect(Collectors.toMap(
                                                              row -> ((Order.OrderStatus) row[0]).name(),
                                                              row -> (Long) row[1]
                                                          ));

    // Pending actions
    Long pendingVerifications = countPendingVerifications();
    Long pendingSettlements = countPendingSettlements();
    Long disputedOrders = countDisputedOrders();
    Long failedDeliveries = countFailedDeliveries();

    // Build alerts
    List<AdminDashboardDto.Alert> alerts = buildAlerts(
        pendingVerifications, pendingSettlements, disputedOrders, failedDeliveries);

    return AdminDashboardDto.builder()
                            .totalSellers(totalSellers)
                            .activeSellers(activeSellers)
                            .pendingVerificationSellers(pendingVerificationSellers)
                            .totalBuyers(totalBuyers)
                            .activeBuyers(activeBuyers)
                            .todayOrders(todayOrders)
                            .todayRevenue(todayRevenue)
                            .todayPlatformFees(todayPlatformFees)
                            .monthOrders(monthOrders)
                            .monthRevenue(monthRevenue)
                            .monthPlatformFees(monthPlatformFees)
                            .ordersByStatus(ordersByStatus)
                            .pendingVerifications(pendingVerifications)
                            .pendingSettlements(pendingSettlements)
                            .disputedOrders(disputedOrders)
                            .failedDeliveries(failedDeliveries)
                            .alerts(alerts)
                            .recentActivities(Collections.emptyList())
                            .build();
  }

  private Long countInactiveSellers()
  {
    return sellerRepository.countByStatusIn(
        List.of(Seller.SellerStatus.INACTIVE, Seller.SellerStatus.SUSPENDED));
  }

  private Long countInactiveBuyers()
  {
    return buyerRepository.countByStatusIn(
        List.of(Buyer.BuyerStatus.INACTIVE, Buyer.BuyerStatus.BLOCKED));
  }

  private Long countPendingVerificationSellers()
  {
    return verificationRepository.countPendingVerifications();
  }

  private Long countPendingVerifications()
  {
    return verificationRepository.countPendingVerifications();
  }

  private Long countPendingSettlements()
  {
    return settlementRepository.countPendingSettlements();
  }

  private Long countDisputedOrders()
  {
    return disputeRepository.countOpenDisputes();
  }

  private Long countFailedDeliveries()
  {
    return deliveryRepository.countFailedDeliveries();
  }

  private List<AdminDashboardDto.Alert> buildAlerts(Long pendingVerifications, Long pendingSettlements,
                                                    Long disputedOrders, Long failedDeliveries)
  {
    List<AdminDashboardDto.Alert> alerts = new ArrayList<>();

    if (pendingVerifications > 10)
    {
      alerts.add(AdminDashboardDto.Alert.builder()
                                        .severity("WARNING")
                                        .title("Pending Verifications")
                                        .message(pendingVerifications + " seller verifications pending review")
                                        .actionUrl("/admin/verifications")
                                        .build());
    }

    if (pendingSettlements > 0)
    {
      alerts.add(AdminDashboardDto.Alert.builder()
                                        .severity("INFO")
                                        .title("Pending Settlements")
                                        .message(pendingSettlements + " settlements ready for processing")
                                        .actionUrl("/admin/settlements")
                                        .build());
    }

    if (disputedOrders > 0)
    {
      alerts.add(AdminDashboardDto.Alert.builder()
                                        .severity("CRITICAL")
                                        .title("Disputed Orders")
                                        .message(disputedOrders + " orders require attention")
                                        .actionUrl("/admin/disputes")
                                        .build());
    }

    if (failedDeliveries > 5)
    {
      alerts.add(AdminDashboardDto.Alert.builder()
                                        .severity("WARNING")
                                        .title("Failed Deliveries")
                                        .message(failedDeliveries + " deliveries failed today")
                                        .actionUrl("/admin/deliveries?status=FAILED")
                                        .build());
    }

    return alerts;
  }
  private final AnalyticsRepository analyticsRepository;
  private final OrderRepository orderRepository;
  private final SellerVerificationRepository verificationRepository;
  private final SellerRepository sellerRepository;
  private final BuyerRepository buyerRepository;
  private final SettlementRepository settlementRepository;
  private final DisputeRepository disputeRepository;
  private final DeliveryRepository deliveryRepository;
}
