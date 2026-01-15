package com.poultry.buyer.core.network

import com.poultry.buyer.domain.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/buyer/otp/request")
    suspend fun requestOtp(@Body request: OtpRequest): Response<ApiResponse<OtpSentResponse>>

    @POST("auth/buyer/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/buyer/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/buyer/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    // Products
    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PaginatedResponse<Product>>>

    @GET("products/{productId}")
    suspend fun getProduct(@Path("productId") productId: String): Response<ApiResponse<Product>>

    @GET("categories")
    suspend fun getCategories(): Response<ApiResponse<List<Category>>>

    @GET("categories/{categoryId}/products")
    suspend fun getProductsByCategory(
        @Path("categoryId") categoryId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PaginatedResponse<Product>>>

    // Cart
    @GET("carts")
    suspend fun getCarts(): Response<ApiResponse<List<Cart>>>

    @GET("carts/{sellerId}")
    suspend fun getCart(@Path("sellerId") sellerId: String): Response<ApiResponse<Cart>>

    @POST("carts/{sellerId}/items")
    suspend fun addToCart(
        @Path("sellerId") sellerId: String,
        @Body request: AddToCartRequest
    ): Response<ApiResponse<Cart>>

    @PUT("carts/{sellerId}/items/{productId}")
    suspend fun updateCartItem(
        @Path("sellerId") sellerId: String,
        @Path("productId") productId: String,
        @Body request: UpdateCartItemRequest
    ): Response<ApiResponse<Cart>>

    @DELETE("carts/{sellerId}/items/{productId}")
    suspend fun removeFromCart(
        @Path("sellerId") sellerId: String,
        @Path("productId") productId: String
    ): Response<ApiResponse<Unit>>

    // Orders
    @POST("buyer/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<ApiResponse<Order>>

    @POST("buyer/orders/{orderId}/place")
    suspend fun placeOrder(@Path("orderId") orderId: String): Response<ApiResponse<Order>>

    @POST("buyer/orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Path("orderId") orderId: String,
        @Query("reason") reason: String?
    ): Response<ApiResponse<Order>>

    @GET("buyer/orders")
    suspend fun getOrders(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PaginatedResponse<Order>>>

    @GET("buyer/orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: String): Response<ApiResponse<Order>>

    // Payments
    @POST("buyer/payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<ApiResponse<Payment>>

    @GET("buyer/payments/{paymentId}")
    suspend fun getPayment(@Path("paymentId") paymentId: String): Response<ApiResponse<Payment>>

    // Reviews
    @POST("reviews")
    suspend fun createReview(@Body request: CreateReviewRequest): Response<ApiResponse<Review>>

    @GET("reviews/seller/{sellerId}")
    suspend fun getSellerReviews(
        @Path("sellerId") sellerId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PaginatedResponse<Review>>>

    // Disputes
    @POST("disputes/buyer")
    suspend fun createDispute(@Body request: CreateDisputeRequest): Response<ApiResponse<Dispute>>

    @GET("disputes/my/buyer")
    suspend fun getMyDisputes(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PaginatedResponse<Dispute>>>

    @GET("disputes/{disputeId}")
    suspend fun getDispute(@Path("disputeId") disputeId: String): Response<ApiResponse<Dispute>>

    @POST("disputes/{disputeId}/messages")
    suspend fun addDisputeMessage(
        @Path("disputeId") disputeId: String,
        @Body request: AddMessageRequest
    ): Response<ApiResponse<DisputeMessage>>

    @GET("disputes/{disputeId}/messages")
    suspend fun getDisputeMessages(
        @Path("disputeId") disputeId: String
    ): Response<ApiResponse<List<DisputeMessage>>>
}
