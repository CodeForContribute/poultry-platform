package com.poultry.product.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.product.dto.PriceDto;
import com.poultry.product.dto.SetPriceRequest;
import com.poultry.product.entity.PriceHistory;
import com.poultry.product.entity.Product;
import com.poultry.product.repository.BuyerFavoriteRepository;
import com.poultry.product.repository.PriceHistoryRepository;
import com.poultry.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    private final BuyerFavoriteRepository buyerFavoriteRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public PriceDto setPrice(UUID sellerId, UUID productId, SetPriceRequest request, UUID userId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> BusinessException.notFound("Product", productId));

        if (!product.getSellerId().equals(sellerId)) {
            throw BusinessException.forbidden("You don't have access to this product");
        }

        Instant effectiveFrom = request.getEffectiveFrom() != null
                ? request.getEffectiveFrom()
                : Instant.now();

        // If scheduling future price, close current active price at that time
        if (effectiveFrom.isAfter(Instant.now())) {
            priceHistoryRepository.findCurrentPrice(productId, Instant.now())
                    .ifPresent(currentPrice -> {
                        if (currentPrice.getEffectiveTo() == null
                                || currentPrice.getEffectiveTo().isAfter(effectiveFrom)) {
                            // We can't update, so we need to handle this differently
                            log.info("Scheduling price change for product: {} from: {}",
                                    productId, effectiveFrom);
                        }
                    });
        }

        // Create bulk slabs
        List<PriceHistory.BulkDiscountSlab> bulkSlabs = request.getBulkDiscountSlabs() != null
                ? request.getBulkDiscountSlabs().stream()
                .map(s -> PriceHistory.BulkDiscountSlab.builder()
                        .minQty(s.getMinQty())
                        .maxQty(s.getMaxQty())
                        .discountPercent(s.getDiscountPercent())
                        .build())
                .toList()
                : List.of();

        // Create new price entry
        PriceHistory newPrice = PriceHistory.builder()
                .productId(productId)
                .basePrice(request.getBasePrice())
                .bulkDiscountSlabs(bulkSlabs)
                .effectiveFrom(effectiveFrom)
                .createdBy(userId)
                .build();

        newPrice = priceHistoryRepository.save(newPrice);

        log.info("Price set for product: {}, effectiveFrom: {}, basePrice: {}",
                productId, effectiveFrom, request.getBasePrice());

        // Send price change notification if effective immediately
        if (!effectiveFrom.isAfter(Instant.now())) {
            sendPriceChangeNotifications(product, newPrice);
        }

        return mapToDto(newPrice);
    }

    @Transactional(readOnly = true)
    public PriceDto getCurrentPrice(UUID productId) {
        return priceHistoryRepository.findCurrentPrice(productId, Instant.now())
                .map(this::mapToDto)
                .orElseThrow(() -> BusinessException.notFound("Price", productId));
    }

    @Transactional(readOnly = true)
    public List<PriceDto> getPriceHistory(UUID sellerId, UUID productId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> BusinessException.notFound("Product", productId));

        if (!product.getSellerId().equals(sellerId)) {
            throw BusinessException.forbidden("You don't have access to this product");
        }

        return priceHistoryRepository.findByProductIdOrderByEffectiveFromDesc(productId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PriceDto> getScheduledPrices(UUID sellerId, UUID productId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> BusinessException.notFound("Product", productId));

        if (!product.getSellerId().equals(sellerId)) {
            throw BusinessException.forbidden("You don't have access to this product");
        }

        return priceHistoryRepository.findScheduledPrices(productId, Instant.now())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private void sendPriceChangeNotifications(Product product, PriceHistory newPrice) {
        try {
            List<UUID> buyerIds = buyerFavoriteRepository.findBuyersToNotifyForProduct(product.getId());
            buyerIds.addAll(buyerFavoriteRepository.findBuyersToNotifyForSeller(product.getSellerId()));

            if (!buyerIds.isEmpty()) {
                Map<String, Object> event = Map.of(
                        "type", "PRICE_CHANGE",
                        "productId", product.getId(),
                        "productName", product.getName(),
                        "sellerId", product.getSellerId(),
                        "newPrice", newPrice.getBasePrice(),
                        "unit", product.getUnit(),
                        "buyerIds", buyerIds.stream().distinct().toList()
                );

                kafkaTemplate.send("price-change-notifications", product.getId().toString(), event);
                log.info("Price change notification sent for product: {}, buyers: {}",
                        product.getId(), buyerIds.size());
            }
        } catch (Exception e) {
            log.error("Failed to send price change notification for product: {}", product.getId(), e);
        }
    }

    private PriceDto mapToDto(PriceHistory price) {
        return PriceDto.builder()
                .id(price.getId())
                .productId(price.getProductId())
                .basePrice(price.getBasePrice())
                .bulkDiscountSlabs(price.getBulkDiscountSlabs())
                .effectiveFrom(price.getEffectiveFrom())
                .effectiveTo(price.getEffectiveTo())
                .isCurrentlyActive(price.isCurrentlyActive())
                .isScheduled(price.isFuture())
                .createdAt(price.getCreatedAt())
                .build();
    }
}
