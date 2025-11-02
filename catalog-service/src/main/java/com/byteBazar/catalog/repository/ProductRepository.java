package com.byteBazar.catalog.repository;

import com.byteBazar.catalog.domain.Category;
import com.byteBazar.catalog.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entities with advanced query capabilities.
 * Supports dynamic querying, search, filtering, and analytics.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    /**
     * Find product by SKU
     */
    Optional<Product> findBySkuIgnoreCase(String sku);

    /**
     * Find product by slug
     */
    Optional<Product> findBySlugIgnoreCase(String slug);

    /**
     * Find all active products
     */
    Page<Product> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find featured products
     */
    Page<Product> findByIsActiveTrueAndIsFeaturedTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find products by category
     */
    Page<Product> findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(Category category, Pageable pageable);

    /**
     * Find products by category ID
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true ORDER BY p.createdAt DESC")
    Page<Product> findByCategoryIdAndIsActiveTrue(@Param("categoryId") UUID categoryId, Pageable pageable);

    /**
     * Search products by name or description
     */
    @Query("""
        SELECT p FROM Product p 
        WHERE p.isActive = true 
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) 
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY p.createdAt DESC
        """)
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    /**
     * Find products in price range
     */
    Page<Product> findByIsActiveTrueAndPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Find products on sale (have compare price higher than current price)
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.comparePrice > p.price ORDER BY ((p.comparePrice - p.price) / p.comparePrice) DESC")
    Page<Product> findProductsOnSale(Pageable pageable);

    /**
     * Find low stock products
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.trackInventory = true AND p.stockQuantity <= p.lowStockThreshold AND p.stockQuantity > 0")
    List<Product> findLowStockProducts();

    /**
     * Find out of stock products
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.trackInventory = true AND p.stockQuantory <= 0")
    List<Product> findOutOfStockProducts();

    /**
     * Check if SKU exists
     */
    boolean existsBySkuIgnoreCase(String sku);

    /**
     * Check if SKU exists excluding a specific product
     */
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.sku) = LOWER(:sku) AND p.id != :excludeId")
    boolean existsBySkuIgnoreCaseAndIdNot(@Param("sku") String sku, @Param("excludeId") UUID excludeId);

    /**
     * Check if slug exists
     */
    boolean existsBySlugIgnoreCase(String slug);

    /**
     * Check if slug exists excluding a specific product
     */
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.slug) = LOWER(:slug) AND p.id != :excludeId")
    boolean existsBySlugIgnoreCaseAndIdNot(@Param("slug") String slug, @Param("excludeId") UUID excludeId);

    /**
     * Find related products (same category, excluding current product)
     */
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.id != :excludeId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findRelatedProducts(@Param("category") Category category, @Param("excludeId") UUID excludeId, Pageable pageable);

    /**
     * Find products created after a specific date
     */
    List<Product> findByIsActiveTrueAndCreatedAtAfterOrderByCreatedAtDesc(Instant after);

    /**
     * Count products by category
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    long countByCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Count products in price range
     */
    long countByIsActiveTrueAndPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Get price range for active products
     */
    @Query("SELECT MIN(p.price) as minPrice, MAX(p.price) as maxPrice FROM Product p WHERE p.isActive = true")
    Object[] getPriceRange();

    /**
     * Find top selling products (would need order data integration)
     */
    @Query(value = """
        SELECT p.*, COALESCE(order_stats.total_sold, 0) as total_sold
        FROM products p
        LEFT JOIN (
            SELECT product_id, SUM(quantity) as total_sold
            FROM order_items oi
            INNER JOIN orders o ON oi.order_id = o.id
            WHERE o.status = 'COMPLETED' AND o.created_at >= :since
            GROUP BY product_id
        ) order_stats ON p.id = order_stats.product_id
        WHERE p.is_active = true
        ORDER BY total_sold DESC NULLS LAST
        """, nativeQuery = true)
    List<Object[]> findTopSellingProducts(@Param("since") Instant since, Pageable pageable);

    /**
     * Product analytics - sales stats
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_products,
            COUNT(CASE WHEN is_featured = true THEN 1 END) as featured_products,
            COUNT(CASE WHEN track_inventory = true AND stock_quantity <= low_stock_threshold THEN 1 END) as low_stock_products,
            COUNT(CASE WHEN track_inventory = true AND stock_quantity <= 0 THEN 1 END) as out_of_stock_products,
            AVG(price) as average_price,
            MIN(price) as min_price,
            MAX(price) as max_price
        FROM products 
        WHERE is_active = true
        """, nativeQuery = true)
    Object[] getProductAnalytics();

    /**
     * Find products by tags (comma-separated search)
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND LOWER(p.tags) LIKE LOWER(CONCAT('%', :tag, '%')) ORDER BY p.createdAt DESC")
    Page<Product> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * Find recently updated products
     */
    List<Product> findTop10ByIsActiveTrueOrderByUpdatedAtDesc();

    /**
     * Find products with no images
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.images IS EMPTY")
    List<Product> findProductsWithoutImages();

    /**
     * Find products with variants
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND SIZE(p.variants) > 0")
    Page<Product> findProductsWithVariants(Pageable pageable);

    /**
     * Find products without variants
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND SIZE(p.variants) = 0")
    Page<Product> findProductsWithoutVariants(Pageable pageable);

    /**
     * Find products by weight range
     */
    Page<Product> findByIsActiveTrueAndWeightBetween(BigDecimal minWeight, BigDecimal maxWeight, Pageable pageable);

    /**
     * Find digital products
     */
    Page<Product> findByIsActiveTrueAndIsDigitalTrue(Pageable pageable);

    /**
     * Find physical products
     */
    Page<Product> findByIsActiveTrueAndIsDigitalFalse(Pageable pageable);

    /**
     * Custom projection for product summary (performance optimization)
     */
    public interface ProductSummaryProjection {
        UUID getId();
        String getName();
        String getSku();
        BigDecimal getPrice();
        Integer getStockQuantity();
        Boolean getIsActive();
        Boolean getIsFeatured();
        String getCategoryName();
    }

    /**
     * Get product summaries for admin dashboard
     */
    @Query("""
        SELECT p.id as id, p.name as name, p.sku as sku, p.price as price, 
               p.stockQuantity as stockQuantity, p.isActive as isActive, 
               p.isFeatured as isFeatured, c.name as categoryName
        FROM Product p JOIN p.category c 
        WHERE p.isActive = true 
        ORDER BY p.updatedAt DESC
        """)
    Page<ProductSummaryProjection> findProductSummaries(Pageable pageable);

    /**
     * Get product summaries by category
     */
    @Query("""
        SELECT p.id as id, p.name as name, p.sku as sku, p.price as price, 
               p.stockQuantity as stockQuantity, p.isActive as isActive, 
               p.isFeatured as isFeatured, c.name as categoryName
        FROM Product p JOIN p.category c 
        WHERE p.isActive = true AND c.id = :categoryId
        ORDER BY p.updatedAt DESC
        """)
    Page<ProductSummaryProjection> findProductSummariesByCategory(@Param("categoryId") UUID categoryId, Pageable pageable);
}
