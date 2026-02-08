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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Poultry B2B Platform API.
 * Provides grouped APIs for Buyer, Seller, and Admin endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${openapi.production-url:https://api.poultry-platform.com}")
    private String productionUrl;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

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
                .servers(getServers())
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
                .group("buyer")
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
                .group("seller")
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
                .group("admin")
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
                .group("webhooks")
                .displayName("Webhooks")
                .pathsToMatch(
                        "/v1/webhooks/**"
                )
                .build();
    }

    /**
     * Returns appropriate server URLs based on active profile.
     * In production, only production URL is shown.
     * In development, both local and production URLs are shown.
     */
    private List<Server> getServers() {
        List<Server> servers = new ArrayList<>();

        if (activeProfile.contains("production") || activeProfile.contains("kubernetes")) {
            // Production: only show production server
            servers.add(new Server()
                    .url(productionUrl)
                    .description("Production"));
        } else {
            // Development: show local server first, then production
            servers.add(new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development"));
            servers.add(new Server()
                    .url(productionUrl)
                    .description("Production"));
        }

        return servers;
    }
}
