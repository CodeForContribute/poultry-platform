package com.poultry.product.dto;

import com.poultry.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private UUID id;
    private UUID sellerId;
    private String sellerName;
    private CategoryDto category;
    private String name;
    private String nameHi;
    private String sku;
    private String description;
    private Product.ProductUnit unit;
    private BigDecimal minOrderQty;
    private BigDecimal maxOrderQty;
    private List<String> imageUrls;
    private Map<String, Object> attributes;
    private Product.ProductStatus status;

    // Current pricing
    private PriceDto currentPrice;

    private Instant createdAt;
    private Instant updatedAt;
}
