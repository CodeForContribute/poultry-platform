package com.poultry.seller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSellerProfileRequest {

    @Size(max = 255, message = "Business name must be at most 255 characters")
    private String businessName;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private Map<String, Object> address;

    @Pattern(regexp = "^[0-9]{14}$", message = "FSSAI number must be 14 digits")
    private String fssaiNumber;
}
