package com.poultry.product.dto;

import com.poultry.product.entity.Category;
import com.poultry.product.entity.Product;
import jakarta.validation.constraints.*;
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
public class CreateProductRequest {

    @NotNull(message = "Category is required")
    private Category.CategoryCode categoryCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Hindi name cannot exceed 255 characters")
    private String nameHi;

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9-_]+$", message = "SKU can only contain letters, numbers, hyphens and underscores")
    private String sku;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Unit is required")
    private Product.ProductUnit unit;

    @NotNull(message = "Minimum order quantity is required")
    @DecimalMin(value = "0.001", message = "Minimum quantity must be greater than 0")
    private BigDecimal minOrderQty;

    @DecimalMin(value = "0.001", message = "Maximum quantity must be greater than 0")
    private BigDecimal maxOrderQty;

    @Size(max = 10, message = "Maximum 10 images allowed")
    private List<String> imageUrls;

    private Map<String, Object> attributes;

    // Initial pricing
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal basePrice;

    private List<BulkDiscountSlabRequest> bulkDiscountSlabs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscountSlabRequest {
        @NotNull
        @DecimalMin(value = "1")
        private BigDecimal minQty;

        private BigDecimal maxQty;

        @NotNull
        @DecimalMin(value = "0")
        @DecimalMax(value = "100")
        private BigDecimal discountPercent;
    }
}
