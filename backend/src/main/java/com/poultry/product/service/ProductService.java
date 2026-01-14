package com.poultry.product.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.product.dto.*;
import com.poultry.product.entity.*;
import com.poultry.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final SellerRepository sellerRepository;
    private final BuyerFavoriteRepository buyerFavoriteRepository;

    @Transactional
    public ProductDto createProduct(UUID sellerId, CreateProductRequest request) {
        // Verify seller exists
        sellerRepository.findByIdAndStatus(sellerId, Seller.SellerStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("Seller", sellerId));

        // Check SKU uniqueness
        if (productRepository.existsBySellerIdAndSku(sellerId, request.getSku())) {
            throw new BusinessException(
                    "Product with SKU '" + request.getSku() + "' already exists",
                    "DUPLICATE_SKU",
                    HttpStatus.CONFLICT
            );
        }

        // Get category
        Category category = categoryRepository.findByCode(request.getCategoryCode())
                .orElseThrow(() -> BusinessException.notFound("Category", request.getCategoryCode()));

        // Create product
        Product product = Product.builder()
                .sellerId(sellerId)
                .category(category)
                .name(request.getName())
                .nameHi(request.getNameHi())
                .sku(request.getSku())
                .description(request.getDescription())
                .unit(request.getUnit())
                .minOrderQty(request.getMinOrderQty())
                .maxOrderQty(request.getMaxOrderQty())
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls() : List.of())
                .attributes(request.getAttributes() != null ? request.getAttributes() : Map.of())
                .status(Product.ProductStatus.ACTIVE)
                .build();

        product = productRepository.save(product);

        // Create initial price
        PriceHistory price = createPrice(product.getId(), request.getBasePrice(),
                request.getBulkDiscountSlabs(), Instant.now(), null);

        log.info("Product created: {} for seller: {}", product.getId(), sellerId);

        return mapToDto(product, price, null);
    }

    @Transactional
    public ProductDto updateProduct(UUID sellerId, UUID productId, UpdateProductRequest request) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> BusinessException.notFound("Product", productId));

        if (!product.getSellerId().equals(sellerId)) {
            throw BusinessException.forbidden("You don't have access to this product");
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getNameHi() != null) product.setNameHi(request.getNameHi());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getMinOrderQty() != null) product.setMinOrderQty(request.getMinOrderQty());
        if (request.getMaxOrderQty() != null) product.setMaxOrderQty(request.getMaxOrderQty());
        if (request.getImageUrls() != null) product.setImageUrls(request.getImageUrls());
        if (request.getAttributes() != null) product.setAttributes(request.getAttributes());
        if (request.getStatus() != null) product.setStatus(request.getStatus());

        product = productRepository.save(product);

        PriceHistory currentPrice = priceHistoryRepository
                .findCurrentPrice(productId, Instant.now())
                .orElse(null);

        log.info("Product updated: {} by seller: {}", productId, sellerId);

        return mapToDto(product, currentPrice, null);
    }

    @Transactional
    public void deleteProduct(UUID sellerId, UUID productId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> BusinessException.notFound("Product", productId));

        if (!product.getSellerId().equals(sellerId)) {
            throw BusinessException.forbidden("You don't have access to this product");
        }

        product.softDelete();
        productRepository.save(product);

        log.info("Product soft-deleted: {} by seller: {}", productId, sellerId);
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID productId) {
        Product product = productRepository.findByIdAndNotDeleted(productId)
                .orElseThrow(() -> BusinessException.notFound("Product", productId));

        PriceHistory currentPrice = priceHistoryRepository
                .findCurrentPrice(productId, Instant.now())
                .orElse(null);

        Seller seller = sellerRepository.findById(product.getSellerId()).orElse(null);

        return mapToDto(product, currentPrice, seller);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getSellerProducts(UUID sellerId) {
        List<Product> products = productRepository.findBySellerIdAndNotDeleted(sellerId);

        List<UUID> productIds = products.stream().map(Product::getId).toList();
        Map<UUID, PriceHistory> priceMap = priceHistoryRepository
                .findCurrentPricesForProducts(productIds, Instant.now())
                .stream()
                .collect(Collectors.toMap(PriceHistory::getProductId, p -> p));

        Seller seller = sellerRepository.findById(sellerId).orElse(null);

        return products.stream()
                .map(p -> mapToDto(p, priceMap.get(p.getId()), seller))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> searchProducts(String query, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(query, pageable);

        List<UUID> productIds = products.getContent().stream().map(Product::getId).toList();
        Map<UUID, PriceHistory> priceMap = priceHistoryRepository
                .findCurrentPricesForProducts(productIds, Instant.now())
                .stream()
                .collect(Collectors.toMap(PriceHistory::getProductId, p -> p));

        return products.map(p -> mapToDto(p, priceMap.get(p.getId()), null));
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsByCategory(UUID categoryId, Pageable pageable) {
        Page<Product> products = productRepository.findActiveByCategory(categoryId, pageable);

        List<UUID> productIds = products.getContent().stream().map(Product::getId).toList();
        Map<UUID, PriceHistory> priceMap = priceHistoryRepository
                .findCurrentPricesForProducts(productIds, Instant.now())
                .stream()
                .collect(Collectors.toMap(PriceHistory::getProductId, p -> p));

        return products.map(p -> mapToDto(p, priceMap.get(p.getId()), null));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAllActiveOrdered().stream()
                .map(this::mapCategoryToDto)
                .toList();
    }

    private PriceHistory createPrice(UUID productId, java.math.BigDecimal basePrice,
                                     List<CreateProductRequest.BulkDiscountSlabRequest> slabs,
                                     Instant effectiveFrom, UUID createdBy) {
        List<PriceHistory.BulkDiscountSlab> bulkSlabs = slabs != null
                ? slabs.stream()
                .map(s -> PriceHistory.BulkDiscountSlab.builder()
                        .minQty(s.getMinQty())
                        .maxQty(s.getMaxQty())
                        .discountPercent(s.getDiscountPercent())
                        .build())
                .toList()
                : List.of();

        PriceHistory price = PriceHistory.builder()
                .productId(productId)
                .basePrice(basePrice)
                .bulkDiscountSlabs(bulkSlabs)
                .effectiveFrom(effectiveFrom)
                .createdBy(createdBy)
                .build();

        return priceHistoryRepository.save(price);
    }

    private ProductDto mapToDto(Product product, PriceHistory price, Seller seller) {
        return ProductDto.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .sellerName(seller != null ? seller.getBusinessName() : null)
                .category(mapCategoryToDto(product.getCategory()))
                .name(product.getName())
                .nameHi(product.getNameHi())
                .sku(product.getSku())
                .description(product.getDescription())
                .unit(product.getUnit())
                .minOrderQty(product.getMinOrderQty())
                .maxOrderQty(product.getMaxOrderQty())
                .imageUrls(product.getImageUrls())
                .attributes(product.getAttributes())
                .status(product.getStatus())
                .currentPrice(price != null ? mapPriceToDto(price) : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private CategoryDto mapCategoryToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .nameHi(category.getNameHi())
                .hsnCode(category.getHsnCode())
                .description(category.getDescription())
                .gstRate(category.getGstRate())
                .build();
    }

    private PriceDto mapPriceToDto(PriceHistory price) {
        return PriceDto.builder()
                .id(price.getId())
                .productId(price.getProductId())
                .basePrice(price.getBasePrice())
                .bulkDiscountSlabs(price.getBulkDiscountSlabs())
                .effectiveFrom(price.getEffectiveFrom())
                .effectiveTo(price.getEffectiveTo())
                .isCurrentlyActive(price.isCurrentlyActive())
                .isScheduled(price.isFuture())
                .createdAt(price.getCreatedAt())
                .build();
    }
}
