package com.poultry.product.dto;

import com.poultry.product.entity.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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
public class UpdateProductRequest {

    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Hindi name cannot exceed 255 characters")
    private String nameHi;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @DecimalMin(value = "0.001", message = "Minimum quantity must be greater than 0")
    private BigDecimal minOrderQty;

    @DecimalMin(value = "0.001", message = "Maximum quantity must be greater than 0")
    private BigDecimal maxOrderQty;

    @Size(max = 10, message = "Maximum 10 images allowed")
    private List<String> imageUrls;

    private Map<String, Object> attributes;

    private Product.ProductStatus status;
}
