package com.poultry.product.dto;

import com.poultry.product.entity.Category;
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
public class CategoryDto {

    private UUID id;
    private Category.CategoryCode code;
    private String name;
    private String nameHi;
    private String hsnCode;
    private String description;
    private BigDecimal gstRate;
}
