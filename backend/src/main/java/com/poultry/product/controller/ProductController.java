package com.poultry.product.controller;

import com.poultry.auth.security.UserPrincipal;
import com.poultry.common.dto.ApiResponse;
import com.poultry.common.exception.BusinessException;
import com.poultry.product.dto.*;
import com.poultry.product.service.PricingService;
import com.poultry.product.service.ProductService;
import com.poultry.storage.dto.FileUploadDto;
import com.poultry.storage.entity.FileUpload;
import com.poultry.storage.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;
    private final PricingService pricingService;
    private final FileUploadService fileUploadService;

    private static final String PRODUCT_IMAGES_BUCKET = "product-images";
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    // ============ Seller Endpoints ============

    @PostMapping("/seller/products")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Create product", description = "Create a new product (seller only)")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProductRequest request) {

        ProductDto product = productService.createProduct(principal.getSellerId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    @PutMapping("/seller/products/{productId}")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Update product", description = "Update product details (seller only)")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {

        ProductDto product = productService.updateProduct(principal.getSellerId(), productId, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    @DeleteMapping("/seller/products/{productId}")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Delete product", description = "Soft delete a product (seller only)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {

        productService.deleteProduct(principal.getSellerId(), productId);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }

    @GetMapping("/seller/products")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get seller's products", description = "Get all products for the seller")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getSellerProducts(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ProductDto> products = productService.getSellerProducts(principal.getSellerId());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping("/seller/products/{productId}/price")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Set product price", description = "Set or schedule product price")
    public ResponseEntity<ApiResponse<PriceDto>> setPrice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody SetPriceRequest request) {

        PriceDto price = pricingService.setPrice(
                principal.getSellerId(), productId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(price, "Price set successfully"));
    }

    @GetMapping("/seller/products/{productId}/prices")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get price history", description = "Get product price history")
    public ResponseEntity<ApiResponse<List<PriceDto>>> getPriceHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {

        List<PriceDto> prices = pricingService.getPriceHistory(principal.getSellerId(), productId);
        return ResponseEntity.ok(ApiResponse.success(prices));
    }

    @GetMapping("/seller/products/{productId}/scheduled-prices")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get scheduled prices", description = "Get scheduled future prices")
    public ResponseEntity<ApiResponse<List<PriceDto>>> getScheduledPrices(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {

        List<PriceDto> prices = pricingService.getScheduledPrices(principal.getSellerId(), productId);
        return ResponseEntity.ok(ApiResponse.success(prices));
    }

    // ============ Product Image Endpoints ============

    @PostMapping(value = "/seller/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Upload product image", description = "Upload an image for a product")
    public ResponseEntity<ApiResponse<FileUploadDto>> uploadProductImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file) {

        // Validate product belongs to seller
        ProductDto product = productService.getProduct(productId);
        if (!product.getSellerId().equals(principal.getSellerId())) {
            throw BusinessException.forbidden("You don't have access to this product");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new BusinessException("File is required", "FILE_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("File size exceeds maximum allowed (5MB)",
                    "FILE_TOO_LARGE", HttpStatus.BAD_REQUEST);
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BusinessException("Invalid file type. Allowed: JPEG, PNG, WebP, GIF",
                    "INVALID_FILE_TYPE", HttpStatus.BAD_REQUEST);
        }

        // Upload file
        FileUpload fileUpload = fileUploadService.uploadFile(
                file,
                PRODUCT_IMAGES_BUCKET,
                "PRODUCT",
                productId,
                "SELLER_USER",
                principal.getId());

        // Get presigned URL for the uploaded file
        String presignedUrl = fileUploadService.getPresignedUrl(fileUpload.getId());
        FileUploadDto dto = FileUploadDto.fromEntity(fileUpload, presignedUrl);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Image uploaded successfully"));
    }

    @GetMapping("/seller/products/{productId}/images")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Get product images", description = "Get all images for a product")
    public ResponseEntity<ApiResponse<List<FileUploadDto>>> getProductImages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {

        // Validate product belongs to seller
        ProductDto product = productService.getProduct(productId);
        if (!product.getSellerId().equals(principal.getSellerId())) {
            throw BusinessException.forbidden("You don't have access to this product");
        }

        List<FileUpload> files = fileUploadService.getFilesByReference("PRODUCT", productId);
        List<FileUploadDto> dtos = files.stream()
                .map(f -> {
                    String presignedUrl = fileUploadService.getPresignedUrl(f.getId());
                    return FileUploadDto.fromEntity(f, presignedUrl);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @DeleteMapping("/seller/products/{productId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('SELLER_ADMIN', 'SELLER_STAFF')")
    @Operation(summary = "Delete product image", description = "Delete a specific image from a product")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {

        // Validate product belongs to seller
        ProductDto product = productService.getProduct(productId);
        if (!product.getSellerId().equals(principal.getSellerId())) {
            throw BusinessException.forbidden("You don't have access to this product");
        }

        // Validate image belongs to this product
        FileUpload file = fileUploadService.getFile(imageId);
        if (!"PRODUCT".equals(file.getReferenceType()) || !productId.equals(file.getReferenceId())) {
            throw BusinessException.forbidden("Image does not belong to this product");
        }

        fileUploadService.deleteFile(imageId);

        return ResponseEntity.ok(ApiResponse.success(null, "Image deleted successfully"));
    }

    // ============ Public/Buyer Endpoints ============

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get product", description = "Get product details")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable UUID productId) {
        ProductDto product = productService.getProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/products/search")
    @Operation(summary = "Search products", description = "Search products by name or description")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> searchProducts(
            @RequestParam String q,
            Pageable pageable) {

        Page<ProductDto> products = productService.searchProducts(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get categories", description = "Get all product categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {
        List<CategoryDto> categories = productService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/categories/{categoryId}/products")
    @Operation(summary = "Get products by category", description = "Get products in a category")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> getProductsByCategory(
            @PathVariable UUID categoryId,
            Pageable pageable) {

        Page<ProductDto> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/products/{productId}/price")
    @Operation(summary = "Get current price", description = "Get product's current price")
    public ResponseEntity<ApiResponse<PriceDto>> getCurrentPrice(@PathVariable UUID productId) {
        PriceDto price = pricingService.getCurrentPrice(productId);
        return ResponseEntity.ok(ApiResponse.success(price));
    }
}
