package com.poultry.cart.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.cart.dto.AddToCartRequest;
import com.poultry.cart.dto.CartDto;
import com.poultry.cart.dto.UpdateCartItemRequest;
import com.poultry.cart.service.CartService;
import com.poultry.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/carts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUYER')")
@Tag(name = "Cart", description = "Shopping cart management APIs")
public class CartController
{

  @GetMapping
  @Operation(summary = "Get all carts", description = "Get all shopping carts for the authenticated buyer")
  public ResponseEntity<ApiResponse<List<CartDto>>> getAllCarts(
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    List<CartDto> carts = cartService.getBuyerCarts(principal.getId());
    return ResponseEntity.ok(ApiResponse.success(carts));
  }

  @GetMapping("/{sellerId}")
  @Operation(summary = "Get cart for seller", description = "Get shopping cart for a specific seller")
  public ResponseEntity<ApiResponse<CartDto>> getCart(
      @PathVariable
      UUID sellerId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    CartDto cart = cartService.getCart(principal.getId(), sellerId);
    return ResponseEntity.ok(ApiResponse.success(cart));
  }

  @PostMapping("/{sellerId}/items")
  @Operation(summary = "Add item to cart", description = "Add a product to the cart for a specific seller")
  public ResponseEntity<ApiResponse<CartDto>> addToCart(
      @PathVariable
      UUID sellerId,
      @Valid
      @RequestBody
      AddToCartRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    CartDto cart = cartService.addToCart(principal.getId(), sellerId, request);
    return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart"));
  }

  @PutMapping("/{sellerId}/items/{productId}")
  @Operation(summary = "Update cart item", description = "Update quantity of an item in the cart")
  public ResponseEntity<ApiResponse<CartDto>> updateCartItem(
      @PathVariable
      UUID sellerId,
      @PathVariable
      UUID productId,
      @Valid
      @RequestBody
      UpdateCartItemRequest request,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    CartDto cart = cartService.updateCartItem(principal.getId(), sellerId, productId, request);
    return ResponseEntity.ok(ApiResponse.success(cart, "Cart updated"));
  }

  @DeleteMapping("/{sellerId}/items/{productId}")
  @Operation(summary = "Remove item from cart", description = "Remove a product from the cart")
  public ResponseEntity<ApiResponse<Void>> removeFromCart(
      @PathVariable
      UUID sellerId,
      @PathVariable
      UUID productId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    cartService.removeFromCart(principal.getId(), sellerId, productId);
    return ResponseEntity.ok(ApiResponse.success(null, "Item removed from cart"));
  }

  @DeleteMapping("/{sellerId}")
  @Operation(summary = "Clear cart", description = "Clear all items from the cart for a specific seller")
  public ResponseEntity<ApiResponse<Void>> clearCart(
      @PathVariable
      UUID sellerId,
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    cartService.clearCart(principal.getId(), sellerId);
    return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared"));
  }

  @DeleteMapping
  @Operation(summary = "Clear all carts", description = "Clear all shopping carts for the authenticated buyer")
  public ResponseEntity<ApiResponse<Void>> clearAllCarts(
      @AuthenticationPrincipal
      UserPrincipal principal)
  {

    cartService.clearAllCarts(principal.getId());
    return ResponseEntity.ok(ApiResponse.success(null, "All carts cleared"));
  }
  private final CartService cartService;
}
