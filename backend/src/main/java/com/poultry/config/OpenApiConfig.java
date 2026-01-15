package com.poultry.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Poultry B2B Platform API.
 * Provides grouped APIs for Buyer, Seller, and Admin endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Poultry B2B Platform API")
                        .version("1.0.0")
                        .description("""
                                # Poultry B2B Platform API Documentation

                                This API powers the Poultry B2B marketplace platform, enabling:
                                - **Buyers**: Browse products, manage carts, place orders, track deliveries
                                - **Sellers**: Manage products, process orders, track settlements
                                - **Admins**: Manage users, settlements, disputes, and analytics

                                ## Authentication

                                - **Buyers**: OTP-based authentication via phone number
                                - **Sellers**: Email/password authentication
                                - **Admins**: Email/password authentication

                                All authenticated endpoints require a Bearer token in the Authorization header.

                                ## Rate Limiting

                                - Default: 100 requests/minute
                                - Auth endpoints: 20 requests/minute
                                - Admin endpoints: 200 requests/minute
                                """)
                        .contact(new Contact()
                                .name("Poultry Platform Team")
                                .email("support@poultryplatform.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://poultryplatform.com/terms")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.poultryplatform.com").description("Production")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        JWT token obtained from authentication endpoints:
                                        - Buyers: POST /v1/auth/buyer/otp/verify
                                        - Sellers: POST /v1/auth/seller/login
                                        - Admins: POST /v1/admin/auth/login

                                        Token expiry: 15 minutes (access), 7 days (refresh)
                                        """)))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    /**
     * Grouped API for Buyer endpoints.
     * Includes authentication, products, cart, orders, payments, reviews, and disputes.
     */
    @Bean
    public GroupedOpenApi buyerApi() {
        return GroupedOpenApi.builder()
                .group("1. Buyer API")
                .displayName("Buyer API")
                .pathsToMatch(
                        "/v1/auth/buyer/**",
                        "/v1/products/**",
                        "/v1/categories/**",
                        "/v1/carts/**",
                        "/v1/buyer/**",
                        "/v1/reviews/**",
                        "/v1/disputes/buyer",
                        "/v1/disputes/my/buyer",
                        "/v1/disputes/{disputeId}",
                        "/v1/disputes/{disputeId}/messages"
                )
                .build();
    }

    /**
     * Grouped API for Seller endpoints.
     * Includes authentication, product management, order management, and disputes.
     */
    @Bean
    public GroupedOpenApi sellerApi() {
        return GroupedOpenApi.builder()
                .group("2. Seller API")
                .displayName("Seller API")
                .pathsToMatch(
                        "/v1/auth/seller/**",
                        "/v1/seller/**",
                        "/v1/disputes/seller",
                        "/v1/disputes/my/seller",
                        "/v1/verifications/**"
                )
                .build();
    }

    /**
     * Grouped API for Admin endpoints.
     * Includes user management, settlements, reconciliation, analytics, and disputes.
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("3. Admin API")
                .displayName("Admin API")
                .pathsToMatch(
                        "/v1/admin/**"
                )
                .build();
    }

    /**
     * Grouped API for Webhook endpoints.
     * Payment gateway webhooks (Razorpay).
     */
    @Bean
    public GroupedOpenApi webhookApi() {
        return GroupedOpenApi.builder()
                .group("4. Webhooks")
                .displayName("Webhooks")
                .pathsToMatch(
                        "/v1/webhooks/**"
                )
                .build();
    }
}
