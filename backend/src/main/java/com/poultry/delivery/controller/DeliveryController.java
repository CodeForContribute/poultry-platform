package com.poultry.delivery.controller;

import com.poultry.common.dto.ApiResponse;
import com.poultry.delivery.dto.AssignAgentRequest;
import com.poultry.delivery.dto.DeliveryDto;
import com.poultry.delivery.dto.UpdateLocationRequest;
import com.poultry.delivery.entity.Delivery;
import com.poultry.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/deliveries")
@RequiredArgsConstructor
@Tag(name = "Delivery", description = "Delivery tracking and management APIs")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/orders/{orderId}")
    @Operation(summary = "Create delivery", description = "Create a delivery for an order")
    public ResponseEntity<ApiResponse<Delivery>> createDelivery(@PathVariable UUID orderId) {
        Delivery delivery = deliveryService.createDelivery(orderId);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }

    @PostMapping("/assign")
    @Operation(summary = "Assign agent", description = "Assign a delivery agent to a delivery")
    public ResponseEntity<ApiResponse<Delivery>> assignAgent(@Valid @RequestBody AssignAgentRequest request) {
        Delivery delivery = deliveryService.assignDeliveryAgent(request.getOrderId(), request.getAgentId());
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }

    @PutMapping("/{deliveryId}/status")
    @Operation(summary = "Update status", description = "Update delivery status")
    public ResponseEntity<ApiResponse<Delivery>> updateStatus(
            @PathVariable UUID deliveryId,
            @RequestParam Delivery.DeliveryStatus status,
            @RequestBody(required = false) Map<String, Object> location) {
        Delivery delivery = deliveryService.updateDeliveryStatus(deliveryId, status, location);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }

    @PostMapping("/{deliveryId}/otp/generate")
    @Operation(summary = "Generate OTP", description = "Generate delivery OTP")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateOtp(@PathVariable UUID deliveryId) {
        String otp = deliveryService.generateDeliveryOtp(deliveryId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("otp", otp)));
    }

    @PostMapping("/{deliveryId}/otp/verify")
    @Operation(summary = "Verify OTP", description = "Verify delivery OTP")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyOtp(
            @PathVariable UUID deliveryId,
            @RequestParam String otp) {
        boolean valid = deliveryService.verifyDeliveryOtp(deliveryId, otp);
        return ResponseEntity.ok(ApiResponse.success(Map.of("valid", valid)));
    }

    @PostMapping("/{deliveryId}/complete")
    @Operation(summary = "Complete delivery", description = "Mark delivery as completed with photo proof")
    public ResponseEntity<ApiResponse<Delivery>> completeDelivery(
            @PathVariable UUID deliveryId,
            @RequestParam(required = false) String photoProofUrl) {
        Delivery delivery = deliveryService.completeDelivery(deliveryId, photoProofUrl);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }

    @PostMapping("/{deliveryId}/fail")
    @Operation(summary = "Mark failed", description = "Mark delivery as failed")
    public ResponseEntity<ApiResponse<Delivery>> markFailed(
            @PathVariable UUID deliveryId,
            @RequestParam String reason) {
        Delivery delivery = deliveryService.markDeliveryFailed(deliveryId, reason);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }

    @GetMapping("/orders/{orderId}/tracking")
    @Operation(summary = "Get tracking", description = "Get delivery tracking information")
    public ResponseEntity<ApiResponse<DeliveryDto>> getTracking(@PathVariable UUID orderId) {
        DeliveryDto tracking = deliveryService.getDeliveryTracking(orderId);
        return ResponseEntity.ok(ApiResponse.success(tracking));
    }
}
