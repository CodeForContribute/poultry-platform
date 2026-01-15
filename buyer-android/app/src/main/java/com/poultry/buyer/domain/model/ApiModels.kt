package com.poultry.buyer.domain.model

import com.google.gson.annotations.SerializedName

// API Response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val error: ErrorDetails?,
    val timestamp: String?,
    val correlationId: String?
)

data class ErrorDetails(
    val code: String,
    val message: String,
    val details: Map<String, String>?
)

data class PaginatedResponse<T>(
    val content: List<T>,
    val totalElements: Int,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
)

// Auth models
data class OtpRequest(
    val phone: String,
    val deviceId: String?,
    val deviceFingerprint: String?,
    val deviceInfo: String?
)

data class OtpVerifyRequest(
    val phone: String,
    val otp: String,
    val deviceId: String,
    val deviceFingerprint: String?,
    val deviceInfo: String?,
    val fcmToken: String?
)

data class OtpSentResponse(
    val maskedPhone: String,
    val expiresInSeconds: Int,
    val attemptsRemaining: Int,
    val newUser: Boolean
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val expiresAt: String,
    val userId: String,
    val userType: String,
    val name: String?,
    val email: String?,
    val role: String,
    val mustChangePassword: Boolean,
    val newUser: Boolean
)

data class RefreshTokenRequest(
    val refreshToken: String,
    val deviceId: String?
)

// Product models
data class Product(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val category: Category,
    val name: String,
    val nameHi: String?,
    val sku: String,
    val description: String?,
    val unit: String,
    val minOrderQty: Double,
    val maxOrderQty: Double?,
    val imageUrls: List<String>,
    val status: String,
    val currentPrice: Price?,
    val createdAt: String,
    val updatedAt: String
)

data class Category(
    val id: String,
    val code: String,
    val name: String,
    val nameHi: String?,
    val hsnCode: String?,
    val description: String?,
    val gstRate: Double
)

data class Price(
    val id: String,
    val productId: String,
    val basePrice: Double,
    val bulkDiscountSlabs: List<BulkDiscountSlab>?,
    val effectiveFrom: String,
    val effectiveTo: String?,
    val isCurrentlyActive: Boolean,
    val isScheduled: Boolean,
    val createdAt: String
)

data class BulkDiscountSlab(
    val minQty: Double,
    val maxQty: Double?,
    val discountPercent: Double
)

// Cart models
data class Cart(
    val id: String,
    val buyerId: String,
    val sellerId: String,
    val sellerName: String,
    val sellerBusinessName: String?,
    val items: List<CartItem>,
    val itemCount: Int,
    val totalAmount: Double,
    val createdAt: String,
    val updatedAt: String
)

data class CartItem(
    val id: String,
    val productId: String,
    val productName: String,
    val productSku: String,
    val productUnit: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double,
    val notes: String?,
    val createdAt: String
)

data class AddToCartRequest(
    val productId: String,
    val quantity: Double,
    val notes: String?
)

data class UpdateCartItemRequest(
    val quantity: Double,
    val notes: String?
)

// Order models
data class Order(
    val id: String,
    val orderNumber: String,
    val buyerId: String,
    val buyerName: String,
    val sellerId: String,
    val sellerName: String,
    val type: String,
    val status: String,
    val subtotal: Double,
    val discountAmount: Double,
    val gstAmount: Double,
    val deliveryCharge: Double,
    val totalAmount: Double,
    val platformFee: Double,
    val deliveryDate: String?,
    val deliverySlot: String?,
    val deliveryAddress: DeliveryAddress,
    val deliveryInstructions: String?,
    val items: List<OrderItem>,
    val version: Int,
    val expiresAt: String?,
    val cancelledAt: String?,
    val cancelledBy: String?,
    val cancellationReason: String?,
    val createdAt: String,
    val updatedAt: String,
    val allowedNextStates: List<String>,
    val canCancel: Boolean
)

data class OrderItem(
    val id: String,
    val productId: String,
    val productName: String,
    val productSku: String,
    val quantity: Double,
    val unitPrice: Double,
    val discountPercent: Double?,
    val discountAmount: Double,
    val gstPercent: Double,
    val gstAmount: Double,
    val lineTotal: Double,
    val deliveredQuantity: Double?,
    val status: String
)

data class DeliveryAddress(
    val label: String?,
    val line1: String,
    val line2: String?,
    val city: String,
    val state: String,
    val pincode: String,
    val landmark: String?,
    val latitude: Double?,
    val longitude: Double?,
    val contactName: String,
    val contactPhone: String
)

data class CreateOrderRequest(
    val idempotencyKey: String,
    val sellerId: String,
    val type: String,
    val items: List<CreateOrderItemRequest>,
    val deliveryAddress: DeliveryAddress,
    val deliveryDate: String?,
    val deliverySlot: String?,
    val deliveryInstructions: String?
)

data class CreateOrderItemRequest(
    val productId: String,
    val quantity: Double
)

// Payment models
data class Payment(
    val id: String,
    val orderId: String,
    val gateway: String,
    val gatewayOrderId: String,
    val gatewayPaymentId: String?,
    val amount: Double,
    val currency: String,
    val status: String,
    val razorpayKey: String?,
    val razorpayOrderId: String?,
    val errorCode: String?,
    val errorDescription: String?,
    val paidAt: String?,
    val createdAt: String
)

data class CreatePaymentRequest(
    val orderId: String
)

// Review models
data class Review(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val buyerId: String,
    val buyerName: String,
    val orderId: String,
    val rating: Int,
    val title: String?,
    val comment: String?,
    val qualityRating: Int?,
    val deliveryRating: Int?,
    val communicationRating: Int?,
    val status: String,
    val isVerifiedPurchase: Boolean,
    val helpfulCount: Int,
    val createdAt: String
)

data class CreateReviewRequest(
    val orderId: String,
    val rating: Int,
    val title: String?,
    val comment: String?,
    val qualityRating: Int?,
    val deliveryRating: Int?,
    val communicationRating: Int?
)

// Dispute models
data class Dispute(
    val id: String,
    val disputeNumber: String,
    val orderId: String,
    val orderNumber: String,
    val buyerId: String,
    val buyerName: String,
    val sellerId: String,
    val sellerName: String,
    val raisedBy: String,
    val type: String,
    val status: String,
    val title: String,
    val description: String,
    val evidenceUrls: List<String>?,
    val requestedResolution: String?,
    val requestedAmount: Double?,
    val finalResolution: String?,
    val resolutionAmount: Double?,
    val resolutionNotes: String?,
    val assignedTo: String?,
    val priority: Int,
    val escalatedAt: String?,
    val resolvedAt: String?,
    val messageCount: Int,
    val createdAt: String,
    val updatedAt: String
)

data class DisputeMessage(
    val id: String,
    val disputeId: String,
    val senderType: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val attachmentUrls: List<String>?,
    val isInternal: Boolean,
    val createdAt: String
)

data class CreateDisputeRequest(
    val orderId: String,
    val type: String,
    val title: String,
    val description: String,
    val evidenceUrls: List<String>?,
    val requestedResolution: String?,
    val requestedAmount: Double?
)

data class AddMessageRequest(
    val message: String,
    val attachmentUrls: List<String>?,
    val isInternal: Boolean
)
