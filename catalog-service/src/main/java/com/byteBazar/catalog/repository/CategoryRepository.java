package com.byteBazar.catalog.repository;

import com.byteBazar.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Category entities with advanced query capabilities.
 * Supports hierarchical category operations and dynamic querying.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    /**
     * Find category by slug (URL-friendly identifier)
     */
    Optional<Category> findBySlugIgnoreCase(String slug);

    /**
     * Find all active categories
     */
    List<Category> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    /**
     * Find all root categories (categories with no parent)
     */
    List<Category> findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAscNameAsc();

    /**
     * Find all child categories of a specific parent
     */
    List<Category> findByParentAndIsActiveTrueOrderByDisplayOrderAscNameAsc(Category parent);

    /**
     * Find categories by parent ID
     */
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.isActive = true ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findByParentIdAndIsActiveTrue(@Param("parentId") UUID parentId);

    /**
     * Check if slug exists (for uniqueness validation)
     */
    boolean existsBySlugIgnoreCase(String slug);

    /**
     * Check if slug exists excluding a specific category (for updates)
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.slug) = LOWER(:slug) AND c.id != :excludeId")
    boolean existsBySlugIgnoreCaseAndIdNot(@Param("slug") String slug, @Param("excludeId") UUID excludeId);

    /**
     * Find categories by name containing (case-insensitive search)
     */
    List<Category> findByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(String name);

    /**
     * Get category hierarchy path (all ancestors + current category)
     */
    @Query("""
        WITH RECURSIVE category_path AS (
            SELECT c.id, c.name, c.parent_id, c.slug, 0 as level, c.name as path
            FROM categories c 
            WHERE c.id = :categoryId
            
            UNION ALL
            
            SELECT p.id, p.name, p.parent_id, p.slug, cp.level + 1, p.name || ' > ' || cp.path
            FROM categories p
            INNER JOIN category_path cp ON p.id = cp.parent_id
        )
        SELECT * FROM category_path ORDER BY level DESC
        """, nativeQuery = true)
    List<Object[]> getCategoryPath(@Param("categoryId") UUID categoryId);

    /**
     * Get all descendant categories (children, grandchildren, etc.)
     */
    @Query("""
        WITH RECURSIVE category_descendants AS (
            SELECT c.id, c.name, c.parent_id, c.slug, c.is_active, 0 as level
            FROM categories c 
            WHERE c.parent_id = :parentId
            
            UNION ALL
            
            SELECT child.id, child.name, child.parent_id, child.slug, child.is_active, cd.level + 1
            FROM categories child
            INNER JOIN category_descendants cd ON child.parent_id = cd.id
        )
        SELECT * FROM category_descendants WHERE is_active = true ORDER BY level, name
        """, nativeQuery = true)
    List<Object[]> getDescendantCategories(@Param("parentId") UUID parentId);

    /**
     * Find categories with product count
     */
    @Query("""
        SELECT c, COUNT(p) as productCount 
        FROM Category c 
        LEFT JOIN c.products p ON p.isActive = true
        WHERE c.isActive = true 
        GROUP BY c 
        ORDER BY c.displayOrder ASC, c.name ASC
        """)
    List<Object[]> findCategoriesWithProductCount();

    /**
     * Find featured categories with their product counts
     */
    @Query("""
        SELECT c, COUNT(p) as productCount 
        FROM Category c 
        LEFT JOIN c.products p ON p.isActive = true AND p.isFeatured = true
        WHERE c.isActive = true 
        GROUP BY c 
        HAVING COUNT(p) > 0
        ORDER BY productCount DESC, c.name ASC
        """)
    List<Object[]> findCategoriesWithFeaturedProducts();

    /**
     * Get categories that have low stock products
     */
    @Query("""
        SELECT DISTINCT c FROM Category c 
        INNER JOIN c.products p 
        WHERE c.isActive = true 
        AND p.isActive = true 
        AND p.trackInventory = true 
        AND p.stockQuantity <= p.lowStockThreshold
        ORDER BY c.name ASC
        """)
    List<Category> findCategoriesWithLowStockProducts();

    /**
     * Count total products in category including subcategories
     */
    @Query(value = """
        WITH RECURSIVE category_tree AS (
            SELECT id FROM categories WHERE id = :categoryId
            UNION ALL
            SELECT c.id FROM categories c
            INNER JOIN category_tree ct ON c.parent_id = ct.id
        )
        SELECT COUNT(*) FROM products p
        INNER JOIN category_tree ct ON p.category_id = ct.id
        WHERE p.is_active = true
        """, nativeQuery = true)
    long countProductsInCategoryTree(@Param("categoryId") UUID categoryId);

    /**
     * Find categories by level in hierarchy
     */
    @Query(value = """
        WITH RECURSIVE category_levels AS (
            SELECT id, name, parent_id, slug, is_active, 0 as level
            FROM categories 
            WHERE parent_id IS NULL
            
            UNION ALL
            
            SELECT c.id, c.name, c.parent_id, c.slug, c.is_active, cl.level + 1
            FROM categories c
            INNER JOIN category_levels cl ON c.parent_id = cl.id
        )
        SELECT * FROM category_levels 
        WHERE level = :level AND is_active = true 
        ORDER BY name
        """, nativeQuery = true)
    List<Object[]> findCategoriesByLevel(@Param("level") int level);
}
