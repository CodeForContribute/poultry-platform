package com.poultry.cart.service;

import com.poultry.cart.dto.AddToCartRequest;
import com.poultry.cart.dto.CartDto;
import com.poultry.cart.dto.CartItemDto;
import com.poultry.cart.dto.UpdateCartItemRequest;
import com.poultry.cart.entity.Cart;
import com.poultry.cart.entity.CartItem;
import com.poultry.cart.repository.CartItemRepository;
import com.poultry.cart.repository.CartRepository;
import com.poultry.common.exception.BusinessException;
import com.poultry.product.entity.PriceHistory;
import com.poultry.product.entity.Product;
import com.poultry.product.repository.PriceHistoryRepository;
import com.poultry.product.repository.ProductRepository;
import com.poultry.product.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService
{

  @Transactional(readOnly = true)
  public List<CartDto> getBuyerCarts(UUID buyerId)
  {
    log.info("Fetching all carts for buyer: {}", buyerId);
    List<Cart> carts = cartRepository.findByBuyerIdWithItems(buyerId);
    return carts.stream()
                .map(this::enrichCartDto)
                .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public CartDto getCart(UUID buyerId, UUID sellerId)
  {
    log.info("Fetching cart for buyer: {} and seller: {}", buyerId, sellerId);
    Cart cart = cartRepository.findByBuyerIdAndSellerIdWithItems(buyerId, sellerId)
                              .orElse(null);

    if (cart == null)
    {
      return CartDto.builder()
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .items(List.of())
                    .itemCount(0)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
    }

    return enrichCartDto(cart);
  }

  @Transactional
  public CartDto addToCart(UUID buyerId, UUID sellerId, AddToCartRequest request)
  {
    log.info("Adding product {} to cart for buyer: {}, seller: {}", request.getProductId(), buyerId, sellerId);

    // Validate product exists and is active
    Product product = productRepository.findByIdAndNotDeleted(request.getProductId())
                                       .orElseThrow(() -> new BusinessException("Product not found", "PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (!product.isActive())
    {
      throw new BusinessException("Product is not available", "PRODUCT_NOT_AVAILABLE", HttpStatus.BAD_REQUEST);
    }

    // Validate product belongs to seller
    if (!product.getSellerId().equals(sellerId))
    {
      throw new BusinessException("Product does not belong to this seller", "PRODUCT_SELLER_MISMATCH", HttpStatus.BAD_REQUEST);
    }

    // Validate quantity constraints
    if (request.getQuantity().compareTo(product.getMinOrderQty()) < 0)
    {
      throw new BusinessException(
          "Minimum order quantity is " + product.getMinOrderQty(),
          "MIN_QUANTITY_NOT_MET", HttpStatus.BAD_REQUEST);
    }

    if (product.getMaxOrderQty() != null && request.getQuantity().compareTo(product.getMaxOrderQty()) > 0)
    {
      throw new BusinessException(
          "Maximum order quantity is " + product.getMaxOrderQty(),
          "MAX_QUANTITY_EXCEEDED", HttpStatus.BAD_REQUEST);
    }

    // Get or create cart
    Cart cart = cartRepository.findByBuyerIdAndSellerId(buyerId, sellerId)
                              .orElseGet(() ->
                                         {
                                           Cart newCart = Cart.builder()
                                                              .buyerId(buyerId)
                                                              .sellerId(sellerId)
                                                              .build();
                                           return cartRepository.save(newCart);
                                         });

    // Get current price
    BigDecimal unitPrice = getCurrentPrice(request.getProductId());

    // Check if item already exists
    CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.getProductId())
                                              .orElse(null);

    if (existingItem != null)
    {
      // Update quantity
      existingItem.setQuantity(existingItem.getQuantity().add(request.getQuantity()));
      existingItem.setUnitPrice(unitPrice);
      if (request.getNotes() != null)
      {
        existingItem.setNotes(request.getNotes());
      }
      cartItemRepository.save(existingItem);
    }
    else
    {
      // Add new item
      CartItem newItem = CartItem.builder()
                                 .cart(cart)
                                 .productId(request.getProductId())
                                 .quantity(request.getQuantity())
                                 .unitPrice(unitPrice)
                                 .notes(request.getNotes())
                                 .build();
      cart.addItem(newItem);
      cartRepository.save(cart);
    }

    log.info("Product {} added to cart", request.getProductId());
    return getCart(buyerId, sellerId);
  }

  @Transactional
  public CartDto updateCartItem(UUID buyerId, UUID sellerId, UUID productId, UpdateCartItemRequest request)
  {
    log.info("Updating cart item for buyer: {}, seller: {}, product: {}", buyerId, sellerId, productId);

    Cart cart = cartRepository.findByBuyerIdAndSellerId(buyerId, sellerId)
                              .orElseThrow(() -> new BusinessException("Cart not found", "CART_NOT_FOUND", HttpStatus.NOT_FOUND));

    CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                                      .orElseThrow(() -> new BusinessException("Item not in cart", "ITEM_NOT_FOUND", HttpStatus.NOT_FOUND));

    // Validate product constraints
    Product product = productRepository.findByIdAndNotDeleted(productId)
                                       .orElseThrow(() -> new BusinessException("Product not found", "PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (request.getQuantity().compareTo(product.getMinOrderQty()) < 0)
    {
      throw new BusinessException(
          "Minimum order quantity is " + product.getMinOrderQty(),
          "MIN_QUANTITY_NOT_MET", HttpStatus.BAD_REQUEST);
    }

    if (product.getMaxOrderQty() != null && request.getQuantity().compareTo(product.getMaxOrderQty()) > 0)
    {
      throw new BusinessException(
          "Maximum order quantity is " + product.getMaxOrderQty(),
          "MAX_QUANTITY_EXCEEDED", HttpStatus.BAD_REQUEST);
    }

    item.setQuantity(request.getQuantity());
    item.setUnitPrice(getCurrentPrice(productId));
    if (request.getNotes() != null)
    {
      item.setNotes(request.getNotes());
    }

    cartItemRepository.save(item);
    log.info("Cart item updated");

    return getCart(buyerId, sellerId);
  }

  @Transactional
  public void removeFromCart(UUID buyerId, UUID sellerId, UUID productId)
  {
    log.info("Removing product {} from cart for buyer: {}, seller: {}", productId, buyerId, sellerId);

    Cart cart = cartRepository.findByBuyerIdAndSellerId(buyerId, sellerId)
                              .orElseThrow(() -> new BusinessException("Cart not found", "CART_NOT_FOUND", HttpStatus.NOT_FOUND));

    CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                                      .orElseThrow(() -> new BusinessException("Item not in cart", "ITEM_NOT_FOUND", HttpStatus.NOT_FOUND));

    cart.removeItem(item);
    cartItemRepository.delete(item);

    // Delete cart if empty
    if (cart.getItems().isEmpty())
    {
      cartRepository.delete(cart);
    }

    log.info("Product removed from cart");
  }

  @Transactional
  public void clearCart(UUID buyerId, UUID sellerId)
  {
    log.info("Clearing cart for buyer: {}, seller: {}", buyerId, sellerId);

    Cart cart = cartRepository.findByBuyerIdAndSellerId(buyerId, sellerId)
                              .orElse(null);

    if (cart != null)
    {
      cartRepository.delete(cart);
      log.info("Cart cleared");
    }
  }

  @Transactional
  public void clearAllCarts(UUID buyerId)
  {
    log.info("Clearing all carts for buyer: {}", buyerId);
    List<Cart> carts = cartRepository.findByBuyerId(buyerId);
    cartRepository.deleteAll(carts);
  }

  private BigDecimal getCurrentPrice(UUID productId)
  {
    return priceHistoryRepository.findCurrentPrice(productId, Instant.now())
                                 .map(PriceHistory::getBasePrice)
                                 .orElse(BigDecimal.ZERO);
  }

  private CartDto enrichCartDto(Cart cart)
  {
    CartDto dto = CartDto.fromEntity(cart);

    // Enrich with seller info
    sellerRepository.findById(cart.getSellerId()).ifPresent(seller ->
                                                            {
                                                              dto.setSellerName(seller.getBusinessName());
                                                              dto.setSellerBusinessName(seller.getBusinessName());
                                                            });

    // Enrich items with product info
    if (!cart.getItems().isEmpty())
    {
      List<UUID> productIds = cart.getItems().stream()
                                  .map(CartItem::getProductId)
                                  .collect(Collectors.toList());

      // Get products
      List<Product> products = productRepository.findAllById(productIds);
      var productMap = products.stream()
                               .collect(Collectors.toMap(Product::getId, p -> p));

      // Get current prices
      List<PriceHistory> prices = priceHistoryRepository.findCurrentPricesForProducts(productIds, Instant.now());
      var priceMap = prices.stream()
                           .collect(Collectors.toMap(PriceHistory::getProductId, p -> p, (a, b) -> a));

      // Enrich each item
      for (CartItemDto itemDto : dto.getItems())
      {
        Product product = productMap.get(itemDto.getProductId());
        if (product != null)
        {
          itemDto.setProductName(product.getName());
          itemDto.setProductSku(product.getSku());
          itemDto.setProductUnit(product.getUnit().name());
        }

        PriceHistory price = priceMap.get(itemDto.getProductId());
        if (price != null)
        {
          itemDto.setUnitPrice(price.getBasePrice());
          itemDto.setLineTotal(price.getBasePrice().multiply(itemDto.getQuantity()));
        }
      }

      // Recalculate total
      BigDecimal total = dto.getItems().stream()
                            .map(CartItemDto::getLineTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
      dto.setTotalAmount(total);
    }

    return dto;
  }
  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final ProductRepository productRepository;
  private final PriceHistoryRepository priceHistoryRepository;
  private final SellerRepository sellerRepository;
}
