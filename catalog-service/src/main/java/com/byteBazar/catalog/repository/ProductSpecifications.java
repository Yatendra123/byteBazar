package com.byteBazar.catalog.repository;

import com.byteBazar.catalog.domain.Category;
import com.byteBazar.catalog.domain.Product;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specifications for dynamic Product queries.
 * Demonstrates advanced Criteria API usage for complex search functionality.
 */
public class ProductSpecifications {

    /**
     * Create a specification for searching products with multiple criteria
     */
    public static Specification<Product> searchProducts(
            String query,
            UUID categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isFeatured,
            Boolean isInStock,
            Boolean isOnSale,
            List<String> tags,
            Instant createdAfter,
            Instant createdBefore) {
        
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Always filter for active products
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            
            // Text search across multiple fields
            if (query != null && !query.trim().isEmpty()) {
                String searchPattern = "%" + query.toLowerCase() + "%";
                Predicate nameLike = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate descriptionLike = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchPattern);
                Predicate shortDescriptionLike = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("shortDescription")), searchPattern);
                Predicate skuLike = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("sku")), searchPattern);
                Predicate tagsLike = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("tags")), searchPattern);
                
                predicates.add(criteriaBuilder.or(
                    nameLike, descriptionLike, shortDescriptionLike, skuLike, tagsLike));
            }
            
            // Category filter
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            
            // Price range filter
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            
            // Featured filter
            if (isFeatured != null) {
                predicates.add(criteriaBuilder.equal(root.get("isFeatured"), isFeatured));
            }
            
            // Stock availability filter
            if (isInStock != null && isInStock) {
                // Complex stock logic: in stock if not tracking inventory OR has stock OR allows backorder
                Predicate notTrackingInventory = criteriaBuilder.isFalse(root.get("trackInventory"));
                Predicate hasStock = criteriaBuilder.greaterThan(root.get("stockQuantity"), 0);
                Predicate allowsBackorder = criteriaBuilder.isTrue(root.get("allowBackorder"));
                
                predicates.add(criteriaBuilder.or(
                    notTrackingInventory,
                    hasStock,
                    criteriaBuilder.and(
                        criteriaBuilder.lessThanOrEqualTo(root.get("stockQuantity"), 0),
                        allowsBackorder
                    )
                ));
            }
            
            // On sale filter (compare price > current price)
            if (isOnSale != null && isOnSale) {
                predicates.add(criteriaBuilder.and(
                    criteriaBuilder.isNotNull(root.get("comparePrice")),
                    criteriaBuilder.greaterThan(root.get("comparePrice"), root.get("price"))
                ));
            }
            
            // Tags filter (any of the provided tags)
            if (tags != null && !tags.isEmpty()) {
                List<Predicate> tagPredicates = new ArrayList<>();
                for (String tag : tags) {
                    tagPredicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("tags")), 
                        "%" + tag.toLowerCase() + "%"));
                }
                predicates.add(criteriaBuilder.or(tagPredicates.toArray(new Predicate[0])));
            }
            
            // Date range filters
            if (createdAfter != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }
            if (createdBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Find products in specific category including subcategories
     */
    public static Specification<Product> inCategoryTree(UUID categoryId) {
        return (root, query, criteriaBuilder) -> {
            // This would need a recursive CTE in a real implementation
            // For now, just match direct category
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.equal(root.get("category").get("id"), categoryId)
            );
        };
    }

    /**
     * Find products with low stock
     */
    public static Specification<Product> withLowStock() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isTrue(root.get("trackInventory")),
                criteriaBuilder.lessThanOrEqualTo(
                    root.get("stockQuantity"), 
                    root.get("lowStockThreshold")
                ),
                criteriaBuilder.greaterThan(root.get("stockQuantity"), 0)
            );
        };
    }

    /**
     * Find products that are out of stock
     */
    public static Specification<Product> outOfStock() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isTrue(root.get("trackInventory")),
                criteriaBuilder.lessThanOrEqualTo(root.get("stockQuantity"), 0)
            );
        };
    }

    /**
     * Find products on sale
     */
    public static Specification<Product> onSale() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isNotNull(root.get("comparePrice")),
                criteriaBuilder.greaterThan(root.get("comparePrice"), BigDecimal.ZERO),
                criteriaBuilder.greaterThan(root.get("comparePrice"), root.get("price"))
            );
        };
    }

    /**
     * Find featured products
     */
    public static Specification<Product> featured() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isTrue(root.get("isFeatured"))
            );
        };
    }

    /**
     * Find products by price range
     */
    public static Specification<Product> priceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Find products by weight range
     */
    public static Specification<Product> weightRange(BigDecimal minWeight, BigDecimal maxWeight) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            
            if (minWeight != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("weight"), minWeight));
            }
            if (maxWeight != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("weight"), maxWeight));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Find digital products
     */
    public static Specification<Product> digital() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isTrue(root.get("isDigital"))
            );
        };
    }

    /**
     * Find physical products
     */
    public static Specification<Product> physical() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isFalse(root.get("isDigital"))
            );
        };
    }

    /**
     * Find products with variants
     */
    public static Specification<Product> withVariants() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isNotEmpty(root.get("variants"))
            );
        };
    }

    /**
     * Find products without images
     */
    public static Specification<Product> withoutImages() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isEmpty(root.get("images"))
            );
        };
    }

    /**
     * Find products by tag
     */
    public static Specification<Product> withTag(String tag) {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("tags")), 
                    "%" + tag.toLowerCase() + "%"
                )
            );
        };
    }

    /**
     * Find products created in date range
     */
    public static Specification<Product> createdBetween(Instant startDate, Instant endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Find products similar to a given product (same category, different product)
     */
    public static Specification<Product> similarTo(UUID productId, Category category) {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.equal(root.get("category"), category),
                criteriaBuilder.notEqual(root.get("id"), productId)
            );
        };
    }

    /**
     * Find products with profit margin above threshold
     */
    public static Specification<Product> withProfitMarginAbove(BigDecimal minMarginPercentage) {
        return (root, query, criteriaBuilder) -> {
            // Calculate profit margin: ((price - costPrice) / price) * 100
            Expression<BigDecimal> profitMargin = criteriaBuilder.prod(
                criteriaBuilder.quot(
                    criteriaBuilder.diff(root.get("price"), root.get("costPrice")),
                    root.get("price")
                ),
                criteriaBuilder.literal(100)
            );
            
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isNotNull(root.get("costPrice")),
                criteriaBuilder.greaterThan(root.get("costPrice"), BigDecimal.ZERO),
                criteriaBuilder.greaterThanOrEqualTo(profitMargin, minMarginPercentage)
            );
        };
    }

    /**
     * Complex specification combining multiple business rules
     */
    public static Specification<Product> recommendedProducts(UUID categoryId, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.equal(root.get("category").get("id"), categoryId),
                criteriaBuilder.or(
                    criteriaBuilder.isTrue(root.get("isFeatured")),
                    criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("comparePrice")),
                        criteriaBuilder.greaterThan(root.get("comparePrice"), root.get("price"))
                    )
                ),
                maxPrice != null ? 
                    criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice) :
                    criteriaBuilder.conjunction(),
                // In stock condition
                criteriaBuilder.or(
                    criteriaBuilder.isFalse(root.get("trackInventory")),
                    criteriaBuilder.greaterThan(root.get("stockQuantity"), 0),
                    criteriaBuilder.isTrue(root.get("allowBackorder"))
                )
            );
        };
    }
}
