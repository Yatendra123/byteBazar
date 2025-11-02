package com.byteBazar.catalog.service;

import com.byteBazar.catalog.domain.Category;
import com.byteBazar.catalog.domain.Product;
import com.byteBazar.catalog.repository.CategoryRepository;
import com.byteBazar.catalog.repository.ProductRepository;
import com.byteBazar.catalog.repository.ProductSpecifications;
import com.byteBazar.catalog.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enhanced ProductService with Redis caching and comprehensive business logic.
 * Demonstrates expert-level Spring Boot patterns following CustomerServiceEnhanced.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ========== BASIC CRUD OPERATIONS ==========

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getById(UUID id) {
        log.debug("Fetching product by ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        return ProductResponse.detailed(product);
    }

    @Cacheable(value = "products", key = "'sku:' + #sku")
    public ProductResponse getBySku(String sku) {
        log.debug("Fetching product by SKU: {}", sku);
        Product product = productRepository.findBySkuIgnoreCase(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with SKU: " + sku));
        return ProductResponse.detailed(product);
    }

    @Cacheable(value = "products", key = "'slug:' + #slug")
    public ProductResponse getBySlug(String slug) {
        log.debug("Fetching product by slug: {}", slug);
        Product product = productRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with slug: " + slug));
        return ProductResponse.detailed(product);
    }

    @Cacheable(value = "product-lists", key = "'featured-products:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ProductResponse> getFeaturedProducts(Pageable pageable) {
        log.debug("Fetching featured products, page: {}", pageable.getPageNumber());
        Page<Product> products = productRepository.findByIsActiveTrueAndIsFeaturedTrueOrderByCreatedAtDesc(pageable);
        return products.map(ProductResponse::simple);
    }

    @Transactional
    @Caching(
        put = @CachePut(value = "products", key = "#result.id"),
        evict = {
            @CacheEvict(value = "product-lists", allEntries = true),
            @CacheEvict(value = "product-analytics", allEntries = true)
        }
    )
    public ProductResponse create(ProductCreateRequest request) {
        log.info("Creating new product: {}", request.name());
        
        // Apply defaults
        ProductCreateRequest requestWithDefaults = request.withDefaults();
        
        // Business validations
        validateSkuUniqueness(requestWithDefaults.sku());
        validateSlugUniqueness(requestWithDefaults.slug());
        
        // Validate category exists
        Category category = categoryRepository.findById(requestWithDefaults.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + requestWithDefaults.categoryId()));
        
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new InactiveCategoryException("Cannot create product in inactive category");
        }
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(requestWithDefaults.name());
        product.setDescription(requestWithDefaults.description());
        product.setShortDescription(requestWithDefaults.shortDescription());
        product.setSku(requestWithDefaults.sku());
        product.setSlug(requestWithDefaults.slug());
        product.setPrice(requestWithDefaults.price());
        product.setComparePrice(requestWithDefaults.comparePrice());
        product.setCostPrice(requestWithDefaults.costPrice());
        product.setStockQuantity(requestWithDefaults.stockQuantity());
        product.setLowStockThreshold(requestWithDefaults.lowStockThreshold());
        product.setTrackInventory(requestWithDefaults.trackInventory());
        product.setAllowBackorder(requestWithDefaults.allowBackorder());
        product.setWeight(requestWithDefaults.weight());
        product.setDimensionsLength(requestWithDefaults.dimensionsLength());
        product.setDimensionsWidth(requestWithDefaults.dimensionsWidth());
        product.setDimensionsHeight(requestWithDefaults.dimensionsHeight());
        product.setIsActive(requestWithDefaults.isActive());
        product.setIsFeatured(requestWithDefaults.isFeatured());
        product.setIsDigital(requestWithDefaults.isDigital());
        product.setMetaTitle(requestWithDefaults.metaTitle());
        product.setMetaDescription(requestWithDefaults.metaDescription());
        product.setTags(requestWithDefaults.tags());
        product.setCategory(category);
        
        Product saved = productRepository.save(product);
        log.info("Created product with ID: {}", saved.getId());
        
        return ProductResponse.from(saved);
    }

    @Transactional
    @Caching(
        put = @CachePut(value = "products", key = "#id"),
        evict = {
            @CacheEvict(value = "product-lists", allEntries = true),
            @CacheEvict(value = "product-analytics", allEntries = true),
            @CacheEvict(value = "products", key = "'sku:' + #result.sku"),
            @CacheEvict(value = "products", key = "'slug:' + #result.slug")
        }
    )
    public ProductResponse update(UUID id, ProductUpdateRequest request) {
        log.info("Updating product: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        
        // Update only provided fields (partial update pattern)
        if (request.name() != null && !request.name().isBlank()) {
            product.setName(request.name());
        }
        
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        
        if (request.shortDescription() != null) {
            product.setShortDescription(request.shortDescription());
        }
        
        if (request.sku() != null && !request.sku().isBlank()) {
            validateSkuUniqueness(request.sku(), id);
            product.setSku(request.sku());
        }
        
        if (request.slug() != null && !request.slug().isBlank()) {
            validateSlugUniqueness(request.slug(), id);
            product.setSlug(request.slug());
        }
        
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        
        if (request.comparePrice() != null) {
            product.setComparePrice(request.comparePrice());
        }
        
        if (request.costPrice() != null) {
            product.setCostPrice(request.costPrice());
        }
        
        if (request.stockQuantity() != null) {
            product.setStockQuantity(request.stockQuantity());
        }
        
        if (request.lowStockThreshold() != null) {
            product.setLowStockThreshold(request.lowStockThreshold());
        }
        
        if (request.trackInventory() != null) {
            product.setTrackInventory(request.trackInventory());
        }
        
        if (request.allowBackorder() != null) {
            product.setAllowBackorder(request.allowBackorder());
        }
        
        if (request.weight() != null) {
            product.setWeight(request.weight());
        }
        
        if (request.dimensionsLength() != null) {
            product.setDimensionsLength(request.dimensionsLength());
        }
        
        if (request.dimensionsWidth() != null) {
            product.setDimensionsWidth(request.dimensionsWidth());
        }
        
        if (request.dimensionsHeight() != null) {
            product.setDimensionsHeight(request.dimensionsHeight());
        }
        
        if (request.isActive() != null) {
            product.setIsActive(request.isActive());
        }
        
        if (request.isFeatured() != null) {
            product.setIsFeatured(request.isFeatured());
        }
        
        if (request.isDigital() != null) {
            product.setIsDigital(request.isDigital());
        }
        
        if (request.metaTitle() != null) {
            product.setMetaTitle(request.metaTitle());
        }
        
        if (request.metaDescription() != null) {
            product.setMetaDescription(request.metaDescription());
        }
        
        if (request.tags() != null) {
            product.setTags(request.tags());
        }
        
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + request.categoryId()));
            product.setCategory(category);
        }
        
        Product saved = productRepository.save(product);
        log.info("Updated product: {}", saved.getId());
        
        return ProductResponse.from(saved);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "product-lists", allEntries = true),
        @CacheEvict(value = "product-analytics", allEntries = true)
    })
    public void delete(UUID id) {
        log.info("Deleting product: {}", id);
        
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found: " + id);
        }
        
        productRepository.deleteById(id);
        log.info("Deleted product: {}", id);
    }

    // ========== ADVANCED SEARCH WITH SPECIFICATIONS ==========

    public Page<ProductResponse> advancedSearch(ProductSearchRequest searchRequest) {
        log.debug("Performing advanced product search with filters");
        
        // Apply default values
        ProductSearchRequest request = searchRequest.withDefaults();
        
        // Build dynamic specification
        Specification<Product> spec = ProductSpecifications.searchProducts(
            request.query(),
            request.categoryId(),
            request.minPrice(),
            request.maxPrice(),
            request.isFeatured(),
            request.isInStock(),
            request.isOnSale(),
            request.tags(),
            request.createdAfter(),
            request.createdBefore()
        );
        
        // Create pageable with sorting
        Sort sort = createSort(request.sortBy(), request.sortDirection());
        Pageable pageable = PageRequest.of(request.page(), request.size(), sort);
        
        // Execute query and transform results
        Page<Product> products = productRepository.findAll(spec, pageable);
        return products.map(ProductResponse::forSearch);
    }

    @Cacheable(value = "product-search", key = "'search:' + #query + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        log.debug("Searching products with query: {}", query);
        Page<Product> products = productRepository.searchProducts(query, pageable);
        return products.map(ProductResponse::simple);
    }

    // ========== CATEGORY-BASED QUERIES ==========

    @Cacheable(value = "product-lists", key = "'category:' + #categoryId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable) {
        log.debug("Fetching products by category: {}", categoryId);
        Page<Product> products = productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
        return products.map(ProductResponse::simple);
    }

    @Cacheable(value = "product-lists", key = "'related:' + #productId + ':' + #limit")
    public List<ProductResponse> getRelatedProducts(UUID productId, int limit) {
        log.debug("Fetching related products for: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> relatedProducts = productRepository.findRelatedProducts(product.getCategory(), productId, pageable);
        
        return relatedProducts.stream()
                             .map(ProductResponse::simple)
                             .collect(Collectors.toList());
    }

    // ========== INVENTORY AND STOCK MANAGEMENT ==========

    @Cacheable(value = "product-analytics", key = "'low-stock-products'")
    public List<ProductResponse> getLowStockProducts() {
        log.debug("Fetching low stock products");
        List<Product> products = productRepository.findLowStockProducts();
        return products.stream()
                      .map(ProductResponse::simple)
                      .collect(Collectors.toList());
    }

    @Cacheable(value = "product-analytics", key = "'out-of-stock-products'")
    public List<ProductResponse> getOutOfStockProducts() {
        log.debug("Fetching out of stock products");
        List<Product> products = productRepository.findOutOfStockProducts();
        return products.stream()
                      .map(ProductResponse::simple)
                      .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public ProductResponse updateStock(UUID productId, Integer newQuantity) {
        log.info("Updating stock for product: {} to quantity: {}", productId, newQuantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        
        if (!product.getTrackInventory()) {
            throw new InventoryNotTrackedException("Inventory tracking is disabled for this product");
        }
        
        Integer oldQuantity = product.getStockQuantity();
        product.setStockQuantity(newQuantity);
        
        Product saved = productRepository.save(product);
        log.info("Updated stock for product {} from {} to {}", productId, oldQuantity, newQuantity);
        
        return ProductResponse.from(saved);
    }

    // ========== ANALYTICS AND REPORTING METHODS ==========

    @Cacheable(value = "product-analytics", key = "'product-stats'")
    public ProductStatistics getProductStatistics() {
        log.debug("Fetching product statistics");
        Object[] stats = productRepository.getProductAnalytics();
        
        return new ProductStatistics(
            ((Number) stats[0]).longValue(),  // total_products
            ((Number) stats[1]).longValue(),  // featured_products
            ((Number) stats[2]).longValue(),  // low_stock_products
            ((Number) stats[3]).longValue(),  // out_of_stock_products
            (BigDecimal) stats[4],            // average_price
            (BigDecimal) stats[5],            // min_price
            (BigDecimal) stats[6]             // max_price
        );
    }

    @Cacheable(value = "product-analytics", key = "'price-range'")
    public PriceRange getPriceRange() {
        log.debug("Fetching product price range");
        Object[] range = productRepository.getPriceRange();
        return new PriceRange((BigDecimal) range[0], (BigDecimal) range[1]);
    }

    public List<ProductResponse> getRecentProducts(int days) {
        log.debug("Fetching products created in last {} days", days);
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Product> products = productRepository.findByIsActiveTrueAndCreatedAtAfterOrderByCreatedAtDesc(cutoff);
        return products.stream()
                      .map(ProductResponse::simple)
                      .collect(Collectors.toList());
    }

    @Cacheable(value = "product-lists", key = "'on-sale:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ProductResponse> getProductsOnSale(Pageable pageable) {
        log.debug("Fetching products on sale");
        Page<Product> products = productRepository.findProductsOnSale(pageable);
        return products.map(ProductResponse::simple);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateSkuUniqueness(String sku) {
        validateSkuUniqueness(sku, null);
    }

    private void validateSkuUniqueness(String sku, UUID excludeId) {
        boolean exists = (excludeId == null) 
            ? productRepository.existsBySkuIgnoreCase(sku)
            : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, excludeId);
            
        if (exists) {
            throw new ProductSkuAlreadyExistsException("Product SKU already exists: " + sku);
        }
    }

    private void validateSlugUniqueness(String slug) {
        validateSlugUniqueness(slug, null);
    }

    private void validateSlugUniqueness(String slug, UUID excludeId) {
        boolean exists = (excludeId == null) 
            ? productRepository.existsBySlugIgnoreCase(slug)
            : productRepository.existsBySlugIgnoreCaseAndIdNot(slug, excludeId);
            
        if (exists) {
            throw new ProductSlugAlreadyExistsException("Product slug already exists: " + slug);
        }
    }

    private Sort createSort(String sortBy, String sortDirection) {
        // Validate sort field to prevent injection attacks
        String validatedSortBy = validateSortField(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return Sort.by(direction, validatedSortBy);
    }

    private String validateSortField(String sortBy) {
        // Whitelist allowed sort fields for security
        return switch (sortBy) {
            case "name", "price", "createdAt", "updatedAt", "stockQuantity", "sku" -> sortBy;
            default -> "createdAt"; // Default safe fallback
        };
    }

    // ========== INNER CLASSES / RECORDS ==========

    public record ProductStatistics(
        long totalProducts,
        long featuredProducts,
        long lowStockProducts,
        long outOfStockProducts,
        BigDecimal averagePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {}

    public record PriceRange(
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {}

    // ========== CUSTOM EXCEPTIONS ==========

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message) {
            super(message);
        }
    }

    public static class ProductSkuAlreadyExistsException extends RuntimeException {
        public ProductSkuAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class ProductSlugAlreadyExistsException extends RuntimeException {
        public ProductSlugAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException(String message) {
            super(message);
        }
    }

    public static class InactiveCategoryException extends RuntimeException {
        public InactiveCategoryException(String message) {
            super(message);
        }
    }

    public static class InventoryNotTrackedException extends RuntimeException {
        public InventoryNotTrackedException(String message) {
            super(message);
        }
    }
}
