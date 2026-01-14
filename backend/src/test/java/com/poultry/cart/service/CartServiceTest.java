package com.poultry.cart.service;

import com.poultry.cart.dto.AddToCartRequest;
import com.poultry.cart.dto.CartDto;
import com.poultry.cart.entity.Cart;
import com.poultry.cart.repository.CartItemRepository;
import com.poultry.cart.repository.CartRepository;
import com.poultry.common.exception.BusinessException;
import com.poultry.product.entity.PriceHistory;
import com.poultry.product.entity.Product;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.PriceHistoryRepository;
import com.poultry.product.repository.ProductRepository;
import com.poultry.product.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceTest
{

  @BeforeEach
  void setUp()
  {
    buyerId = UUID.randomUUID();
    sellerId = UUID.randomUUID();
    productId = UUID.randomUUID();

    seller = new Seller();
    seller.setId(sellerId);
    seller.setBusinessName("Test Poultry Farm");
    seller.setStatus(Seller.SellerStatus.ACTIVE);

    product = new Product();
    product.setId(productId);
    product.setSellerId(sellerId);
    product.setName("Fresh Chicken");
    product.setSku("CHK-001");
    product.setUnit(Product.Unit.KG);
    product.setMinOrderQty(BigDecimal.ONE);
    product.setMaxOrderQty(BigDecimal.valueOf(100));

    cart = new Cart();
    cart.setId(UUID.randomUUID());
    cart.setBuyerId(buyerId);
    cart.setSellerId(sellerId);
    cart.setItems(new ArrayList<>());

    priceHistory = new PriceHistory();
    priceHistory.setId(UUID.randomUUID());
    priceHistory.setProductId(productId);
    priceHistory.setBasePrice(BigDecimal.valueOf(250));
    priceHistory.setEffectiveFrom(Instant.now().minusSeconds(3600));
  }

  @Nested
  @DisplayName("getBuyerCarts")
  class GetBuyerCartsTests
  {

    @Test
    @DisplayName("should return empty list when buyer has no carts")
    void shouldReturnEmptyListWhenNoCarts()
    {
      when(cartRepository.findByBuyerIdWithItems(buyerId)).thenReturn(List.of());

      List<CartDto> result = cartService.getBuyerCarts(buyerId);

      assertThat(result).isEmpty();
      verify(cartRepository).findByBuyerIdWithItems(buyerId);
    }
  }

  @Nested
  @DisplayName("addToCart")
  class AddToCartTests
  {

    @Test
    @DisplayName("should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound()
    {
      AddToCartRequest request = new AddToCartRequest();
      request.setProductId(productId);
      request.setQuantity(BigDecimal.valueOf(5));

      when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> cartService.addToCart(buyerId, sellerId, request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("should throw exception when quantity below minimum")
    void shouldThrowExceptionWhenQuantityBelowMinimum()
    {
      product.setMinOrderQty(BigDecimal.TEN);
      AddToCartRequest request = new AddToCartRequest();
      request.setProductId(productId);
      request.setQuantity(BigDecimal.valueOf(5));

      when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));

      assertThatThrownBy(() -> cartService.addToCart(buyerId, sellerId, request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("Minimum order quantity");
    }

    @Test
    @DisplayName("should throw exception when quantity exceeds maximum")
    void shouldThrowExceptionWhenQuantityExceedsMaximum()
    {
      product.setMaxOrderQty(BigDecimal.TEN);
      AddToCartRequest request = new AddToCartRequest();
      request.setProductId(productId);
      request.setQuantity(BigDecimal.valueOf(20));

      when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));

      assertThatThrownBy(() -> cartService.addToCart(buyerId, sellerId, request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("Maximum order quantity");
    }

    @Test
    @DisplayName("should throw exception when product belongs to different seller")
    void shouldThrowExceptionWhenProductSellerMismatch()
    {
      product.setSellerId(UUID.randomUUID()); // Different seller
      AddToCartRequest request = new AddToCartRequest();
      request.setProductId(productId);
      request.setQuantity(BigDecimal.valueOf(5));

      when(productRepository.findByIdAndNotDeleted(productId)).thenReturn(Optional.of(product));

      assertThatThrownBy(() -> cartService.addToCart(buyerId, sellerId, request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("does not belong to this seller");
    }
  }

  @Nested
  @DisplayName("removeFromCart")
  class RemoveFromCartTests
  {

    @Test
    @DisplayName("should throw exception when cart not found")
    void shouldThrowExceptionWhenCartNotFound()
    {
      when(cartRepository.findByBuyerIdAndSellerId(buyerId, sellerId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> cartService.removeFromCart(buyerId, sellerId, productId))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("Cart not found");
    }
  }

  @Nested
  @DisplayName("clearCart")
  class ClearCartTests
  {

    @Test
    @DisplayName("should clear cart for seller")
    void shouldClearCart()
    {
      when(cartRepository.findByBuyerIdAndSellerId(buyerId, sellerId)).thenReturn(Optional.of(cart));

      cartService.clearCart(buyerId, sellerId);

      verify(cartRepository).delete(cart);
    }

    @Test
    @DisplayName("should do nothing when cart does not exist")
    void shouldDoNothingWhenCartNotExists()
    {
      when(cartRepository.findByBuyerIdAndSellerId(buyerId, sellerId)).thenReturn(Optional.empty());

      cartService.clearCart(buyerId, sellerId);

      verify(cartRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("clearAllCarts")
  class ClearAllCartsTests
  {

    @Test
    @DisplayName("should clear all carts for buyer")
    void shouldClearAllCarts()
    {
      when(cartRepository.findByBuyerId(buyerId)).thenReturn(List.of(cart));

      cartService.clearAllCarts(buyerId);

      verify(cartRepository).deleteAll(List.of(cart));
    }
  }
  @Mock
  private CartRepository cartRepository;
  @Mock
  private CartItemRepository cartItemRepository;
  @Mock
  private ProductRepository productRepository;
  @Mock
  private PriceHistoryRepository priceHistoryRepository;
  @Mock
  private SellerRepository sellerRepository;
  @InjectMocks
  private CartService cartService;
  private UUID buyerId;
  private UUID sellerId;
  private UUID productId;
  private Product product;
  private Seller seller;
  private Cart cart;
  private PriceHistory priceHistory;
}
