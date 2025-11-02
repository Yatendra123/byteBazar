package com.byteBazar.catalog.web.controller;

import com.byteBazar.catalog.service.ProductService;
import com.byteBazar.catalog.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Product management.
 * Provides comprehensive CRUD operations, search, and inventory management.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Product management operations")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve paginated list of active products")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("GET /api/v1/products - Get all products, page: {}, size: {}", page, size);
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        // For now, using advanced search with no filters (equivalent to "get all active")
        ProductSearchRequest searchRequest = new ProductSearchRequest(
            null, null, null, null, null, null, null, null, null, null, null, null, null,
            sortBy, sortDirection, page, size
        );
        
        Page<ProductResponse> products = productService.advancedSearch(searchRequest);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products", description = "Retrieve paginated list of featured products")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Featured products retrieved successfully")
    })
    public ResponseEntity<Page<ProductResponse>> getFeaturedProducts(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/v1/products/featured - Get featured products, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getFeaturedProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/on-sale")
    @Operation(summary = "Get products on sale", description = "Retrieve paginated list of products currently on sale")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sale products retrieved successfully")
    })
    public ResponseEntity<Page<ProductResponse>> getProductsOnSale(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/v1/products/on-sale - Get products on sale, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getProductsOnSale(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a specific product by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id) {
        log.info("GET /api/v1/products/{} - Get product by ID", id);
        ProductResponse product = productService.getById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU", description = "Retrieve a product by its SKU")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductBySku(
            @Parameter(description = "Product SKU", required = true)
            @PathVariable String sku) {
        log.info("GET /api/v1/products/sku/{} - Get product by SKU", sku);
        ProductResponse product = productService.getBySku(sku);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug", description = "Retrieve a product by its URL-friendly slug")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductBySlug(
            @Parameter(description = "Product slug", required = true)
            @PathVariable String slug) {
        log.info("GET /api/v1/products/slug/{} - Get product by slug", slug);
        ProductResponse product = productService.getBySlug(slug);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER')")
    @Operation(summary = "Create new product", description = "Create a new product in the catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Product SKU or slug already exists"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Product creation data", required = true)
            @Valid @RequestBody ProductCreateRequest request) {
        log.info("POST /api/v1/products - Create product: {}", request.name());
        ProductResponse product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER')")
    @Operation(summary = "Update product", description = "Update an existing product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "409", description = "Product SKU or slug already exists"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Product update data", required = true)
            @Valid @RequestBody ProductUpdateRequest request) {
        log.info("PUT /api/v1/products/{} - Update product", id);
        ProductResponse product = productService.update(id, request);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Delete a product from the catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id) {
        log.info("DELETE /api/v1/products/{} - Delete product", id);
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Advanced product search with multiple filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @Parameter(description = "Search query")
            @RequestParam(value = "q", required = false) String query,
            @Parameter(description = "Category ID filter")
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @Parameter(description = "Minimum price filter")
            @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
            @Parameter(description = "Featured products only")
            @RequestParam(value = "featured", required = false) Boolean isFeatured,
            @Parameter(description = "In stock products only")
            @RequestParam(value = "inStock", required = false) Boolean isInStock,
            @Parameter(description = "On sale products only")
            @RequestParam(value = "onSale", required = false) Boolean isOnSale,
            @Parameter(description = "Digital products filter")
            @RequestParam(value = "digital", required = false) Boolean isDigital,
            @Parameter(description = "Sort by field", example = "name")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "asc")
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        log.info("GET /api/v1/products/search - Advanced search with query: {}", query);
        
        ProductSearchRequest searchRequest = new ProductSearchRequest(
            query, categoryId, minPrice, maxPrice, isFeatured, isInStock, isOnSale, isDigital,
            null, null, null, null, null, sortBy, sortDirection, page, size
        );
        
        Page<ProductResponse> products = productService.advancedSearch(searchRequest);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category", description = "Retrieve products in a specific category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID categoryId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/v1/products/category/{} - Get products by category", categoryId);
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Get related products", description = "Get products related to a specific product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Related products retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<List<ProductResponse>> getRelatedProducts(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Number of related products to return", example = "6")
            @RequestParam(defaultValue = "6") int limit) {
        
        log.info("GET /api/v1/products/{}/related - Get related products", id);
        List<ProductResponse> relatedProducts = productService.getRelatedProducts(id, limit);
        return ResponseEntity.ok(relatedProducts);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent products", description = "Get recently created products")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent products retrieved successfully")
    })
    public ResponseEntity<List<ProductResponse>> getRecentProducts(
            @Parameter(description = "Number of days to look back", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("GET /api/v1/products/recent - Get products from last {} days", days);
        List<ProductResponse> products = productService.getRecentProducts(days);
        return ResponseEntity.ok(products);
    }

    // ========== INVENTORY MANAGEMENT ENDPOINTS ==========

    @GetMapping("/inventory/low-stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER') or hasRole('INVENTORY_MANAGER')")
    @Operation(summary = "Get low stock products", description = "Retrieve products with low inventory levels")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Low stock products retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<ProductResponse>> getLowStockProducts() {
        log.info("GET /api/v1/products/inventory/low-stock - Get low stock products");
        List<ProductResponse> products = productService.getLowStockProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/inventory/out-of-stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER') or hasRole('INVENTORY_MANAGER')")
    @Operation(summary = "Get out of stock products", description = "Retrieve products that are out of stock")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Out of stock products retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<ProductResponse>> getOutOfStockProducts() {
        log.info("GET /api/v1/products/inventory/out-of-stock - Get out of stock products");
        List<ProductResponse> products = productService.getOutOfStockProducts();
        return ResponseEntity.ok(products);
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER') or hasRole('INVENTORY_MANAGER')")
    @Operation(summary = "Update product stock", description = "Update the stock quantity of a product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid stock quantity or inventory not tracked"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ProductResponse> updateStock(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "New stock quantity", required = true)
            @RequestParam("quantity") Integer quantity) {
        
        log.info("PATCH /api/v1/products/{}/stock - Update stock to {}", id, quantity);
        ProductResponse product = productService.updateStock(id, quantity);
        return ResponseEntity.ok(product);
    }

    // ========== ANALYTICS ENDPOINTS ==========

    @GetMapping("/analytics/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER')")
    @Operation(summary = "Get product statistics", description = "Retrieve overall product statistics and analytics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ProductService.ProductStatistics> getProductStatistics() {
        log.info("GET /api/v1/products/analytics/statistics - Get product statistics");
        ProductService.ProductStatistics stats = productService.getProductStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/analytics/price-range")
    @Operation(summary = "Get price range", description = "Get the minimum and maximum product prices")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Price range retrieved successfully")
    })
    public ResponseEntity<ProductService.PriceRange> getPriceRange() {
        log.info("GET /api/v1/products/analytics/price-range - Get price range");
        ProductService.PriceRange priceRange = productService.getPriceRange();
        return ResponseEntity.ok(priceRange);
    }
}
