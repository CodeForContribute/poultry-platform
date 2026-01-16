package com.poultry.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Helper class for MockMvc-based API testing.
 * Provides convenient methods for making HTTP requests with proper authentication.
 */
public class MockMvcHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public MockMvcHelper(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public MockMvcHelper(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    // ============ GET Requests ============

    public ResultActions get(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    public ResultActions getWithAuth(String url, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    public ResultActions getWithAuth(String url, String token, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);

        params.forEach(request::param);

        return mockMvc.perform(request).andDo(print());
    }

    // ============ POST Requests ============

    public ResultActions post(String url, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    public ResultActions postWithAuth(String url, Object body, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    public ResultActions postWithAuth(String url, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    // ============ PUT Requests ============

    public ResultActions put(String url, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    public ResultActions putWithAuth(String url, Object body, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    // ============ PATCH Requests ============

    public ResultActions patch(String url, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    public ResultActions patchWithAuth(String url, Object body, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print());
    }

    // ============ DELETE Requests ============

    public ResultActions delete(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    public ResultActions deleteWithAuth(String url, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    // ============ Webhook Requests ============

    public ResultActions postWebhook(String url, Object payload, String signature) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print());
    }

    // ============ Pagination Helpers ============

    public ResultActions getPage(String url, String token, int page, int size) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .header("Authorization", "Bearer " + token)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    public ResultActions getPageWithSort(String url, String token, int page, int size, String sortBy, String direction) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .header("Authorization", "Bearer " + token)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sort", sortBy + "," + direction)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    // ============ Path Variable Helpers ============

    public ResultActions getById(String baseUrl, UUID id) throws Exception {
        return get(baseUrl + "/" + id);
    }

    public ResultActions getByIdWithAuth(String baseUrl, UUID id, String token) throws Exception {
        return getWithAuth(baseUrl + "/" + id, token);
    }

    public ResultActions deleteById(String baseUrl, UUID id, String token) throws Exception {
        return deleteWithAuth(baseUrl + "/" + id, token);
    }

    // ============ Common API Patterns ============

    // Buyer Auth APIs
    public ResultActions requestOtp(Object otpRequest) throws Exception {
        return post("/api/v1/buyer/auth/otp/request", otpRequest);
    }

    public ResultActions verifyOtp(Object verifyRequest) throws Exception {
        return post("/api/v1/buyer/auth/otp/verify", verifyRequest);
    }

    public ResultActions refreshToken(Object refreshRequest) throws Exception {
        return post("/api/v1/buyer/auth/token/refresh", refreshRequest);
    }

    public ResultActions logout(String token) throws Exception {
        return postWithAuth("/api/v1/buyer/auth/logout", token);
    }

    // Cart APIs
    public ResultActions getCarts(String token) throws Exception {
        return getWithAuth("/api/v1/buyer/carts", token);
    }

    public ResultActions getCart(UUID sellerId, String token) throws Exception {
        return getWithAuth("/api/v1/buyer/carts/" + sellerId, token);
    }

    public ResultActions addToCart(UUID sellerId, Object request, String token) throws Exception {
        return postWithAuth("/api/v1/buyer/carts/" + sellerId + "/items", request, token);
    }

    public ResultActions removeFromCart(UUID sellerId, UUID productId, String token) throws Exception {
        return deleteWithAuth("/api/v1/buyer/carts/" + sellerId + "/items/" + productId, token);
    }

    // Order APIs
    public ResultActions createOrder(Object request, String token) throws Exception {
        return postWithAuth("/api/v1/buyer/orders", request, token);
    }

    public ResultActions getOrders(String token, int page, int size) throws Exception {
        return getPage("/api/v1/buyer/orders", token, page, size);
    }

    public ResultActions getOrder(UUID orderId, String token) throws Exception {
        return getByIdWithAuth("/api/v1/buyer/orders", orderId, token);
    }

    public ResultActions placeOrder(UUID orderId, String token) throws Exception {
        return postWithAuth("/api/v1/buyer/orders/" + orderId + "/place", token);
    }

    public ResultActions updateOrderStatus(UUID orderId, Object request, String token) throws Exception {
        return patchWithAuth("/api/v1/orders/" + orderId + "/status", request, token);
    }

    // Payment APIs
    public ResultActions createPayment(Object request, String token) throws Exception {
        return postWithAuth("/api/v1/buyer/payments", request, token);
    }

    public ResultActions getPayment(UUID paymentId, String token) throws Exception {
        return getByIdWithAuth("/api/v1/payments", paymentId, token);
    }

    public ResultActions handleWebhook(Object payload, String signature) throws Exception {
        return postWebhook("/api/v1/payments/webhook/razorpay", payload, signature);
    }

    // ============ ObjectMapper Access ============

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public <T> T parseResponse(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
}
