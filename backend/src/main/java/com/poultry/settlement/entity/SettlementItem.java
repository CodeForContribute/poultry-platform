package com.poultry.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementItem
{

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "settlement_id", nullable = false)
  private Settlement settlement;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "order_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal orderAmount;

  @Column(name = "platform_fee", nullable = false, precision = 12, scale = 2)
  private BigDecimal platformFee;

  @Column(name = "seller_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal sellerAmount;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
