package com.byteBazar.catalog.service;

import com.byteBazar.catalog.domain.Category;
import com.byteBazar.catalog.repository.CategoryRepository;
import com.byteBazar.catalog.web.dto.CategoryCreateRequest;
import com.byteBazar.catalog.web.dto.CategoryResponse;
import com.byteBazar.catalog.web.dto.CategoryUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enhanced CategoryService with Redis caching and comprehensive business logic.
 * Follows the same patterns as CustomerServiceEnhanced.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ========== BASIC CRUD OPERATIONS ==========

    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse getById(UUID id) {
        log.debug("Fetching category by ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
        return CategoryResponse.from(category);
    }

    @Cacheable(value = "categories", key = "'slug:' + #slug")
    public CategoryResponse getBySlug(String slug) {
        log.debug("Fetching category by slug: {}", slug);
        Category category = categoryRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with slug: " + slug));
        return CategoryResponse.from(category);
    }

    @Cacheable(value = "category-lists", key = "'all-active'")
    public List<CategoryResponse> getAllActiveCategories() {
        log.debug("Fetching all active categories");
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        return categories.stream()
                        .map(CategoryResponse::simple)
                        .collect(Collectors.toList());
    }

    @Cacheable(value = "category-lists", key = "'root-categories'")
    public List<CategoryResponse> getRootCategories() {
        log.debug("Fetching root categories");
        List<Category> rootCategories = categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAscNameAsc();
        return rootCategories.stream()
                            .map(category -> CategoryResponse.from(category, true, true))
                            .collect(Collectors.toList());
    }

    @Transactional
    @Caching(
        put = @CachePut(value = "categories", key = "#result.id"),
        evict = {
            @CacheEvict(value = "category-lists", allEntries = true),
            @CacheEvict(value = "category-hierarchy", allEntries = true)
        }
    )
    public CategoryResponse create(CategoryCreateRequest request) {
        log.info("Creating new category: {}", request.name());
        
        // Apply defaults
        CategoryCreateRequest requestWithDefaults = request.withDefaults();
        
        // Business validations
        validateSlugUniqueness(requestWithDefaults.slug());
        
        // Validate parent category exists if provided
        Category parentCategory = null;
        if (requestWithDefaults.parentId() != null) {
            parentCategory = categoryRepository.findById(requestWithDefaults.parentId())
                    .orElseThrow(() -> new CategoryNotFoundException("Parent category not found: " + requestWithDefaults.parentId()));
        }
        
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(requestWithDefaults.name());
        category.setDescription(requestWithDefaults.description());
        category.setSlug(requestWithDefaults.slug());
        category.setImageUrl(requestWithDefaults.imageUrl());
        category.setParent(parentCategory);
        category.setDisplayOrder(requestWithDefaults.displayOrder());
        category.setIsActive(requestWithDefaults.isActive());
        
        Category saved = categoryRepository.save(category);
        log.info("Created category with ID: {}", saved.getId());
        
        return CategoryResponse.from(saved);
    }

    @Transactional
    @Caching(
        put = @CachePut(value = "categories", key = "#id"),
        evict = {
            @CacheEvict(value = "category-lists", allEntries = true),
            @CacheEvict(value = "category-hierarchy", allEntries = true),
            @CacheEvict(value = "categories", key = "'slug:' + #result.slug")
        }
    )
    public CategoryResponse update(UUID id, CategoryUpdateRequest request) {
        log.info("Updating category: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
        
        // Update only provided fields (partial update pattern)
        if (request.name() != null && !request.name().isBlank()) {
            category.setName(request.name());
        }
        
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        
        if (request.slug() != null && !request.slug().isBlank()) {
            validateSlugUniqueness(request.slug(), id);
            category.setSlug(request.slug());
        }
        
        if (request.imageUrl() != null) {
            category.setImageUrl(request.imageUrl());
        }
        
        if (request.parentId() != null) {
            // Validate parent category exists and prevent circular references
            Category parentCategory = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new CategoryNotFoundException("Parent category not found: " + request.parentId()));
            
            validateNoCircularReference(id, parentCategory);
            category.setParent(parentCategory);
        }
        
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }
        
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        
        Category saved = categoryRepository.save(category);
        log.info("Updated category: {}", saved.getId());
        
        return CategoryResponse.from(saved);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categories", key = "#id"),
        @CacheEvict(value = "category-lists", allEntries = true),
        @CacheEvict(value = "category-hierarchy", allEntries = true)
    })
    public void delete(UUID id) {
        log.info("Deleting category: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
        
        // Check if category has products
        if (!category.getProducts().isEmpty()) {
            throw new CategoryHasProductsException("Cannot delete category with products. Move or delete products first.");
        }
        
        // Check if category has children
        if (category.hasChildren()) {
            throw new CategoryHasChildrenException("Cannot delete category with child categories. Delete or move children first.");
        }
        
        categoryRepository.deleteById(id);
        log.info("Deleted category: {}", id);
    }

    // ========== HIERARCHY AND NAVIGATION METHODS ==========

    @Cacheable(value = "category-hierarchy", key = "'children:' + #parentId")
    public List<CategoryResponse> getChildCategories(UUID parentId) {
        log.debug("Fetching child categories for parent: {}", parentId);
        List<Category> children = categoryRepository.findByParentIdAndIsActiveTrue(parentId);
        return children.stream()
                      .map(CategoryResponse::simple)
                      .collect(Collectors.toList());
    }

    @Cacheable(value = "category-hierarchy", key = "'full-hierarchy:' + #categoryId")
    public CategoryResponse getCategoryWithFullHierarchy(UUID categoryId) {
        log.debug("Fetching category with full hierarchy: {}", categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + categoryId));
        return CategoryResponse.withFullHierarchy(category);
    }

    @Cacheable(value = "category-analytics", key = "'with-product-counts'")
    public List<CategoryAnalytics> getCategoriesWithProductCounts() {
        log.debug("Fetching categories with product counts");
        List<Object[]> results = categoryRepository.findCategoriesWithProductCount();
        return results.stream()
                     .map(result -> new CategoryAnalytics(
                         CategoryResponse.simple((Category) result[0]),
                         ((Number) result[1]).longValue()
                     ))
                     .collect(Collectors.toList());
    }

    public List<CategoryResponse> searchCategories(String query) {
        log.debug("Searching categories with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return getAllActiveCategories();
        }
        
        List<Category> categories = categoryRepository.findByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(query.trim());
        return categories.stream()
                        .map(CategoryResponse::simple)
                        .collect(Collectors.toList());
    }

    // ========== ANALYTICS AND REPORTING METHODS ==========

    @Cacheable(value = "category-analytics", key = "'low-stock-categories'")
    public List<CategoryResponse> getCategoriesWithLowStockProducts() {
        log.debug("Fetching categories with low stock products");
        List<Category> categories = categoryRepository.findCategoriesWithLowStockProducts();
        return categories.stream()
                        .map(CategoryResponse::simple)
                        .collect(Collectors.toList());
    }

    public long getTotalProductsInCategoryTree(UUID categoryId) {
        log.debug("Counting products in category tree: {}", categoryId);
        return categoryRepository.countProductsInCategoryTree(categoryId);
    }

    @Cacheable(value = "category-analytics", key = "'category-stats'")
    public CategoryStatistics getCategoryStatistics() {
        log.debug("Fetching category statistics");
        long totalCategories = categoryRepository.count();
        long activeCategories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc().size();
        long rootCategories = categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAscNameAsc().size();
        
        return new CategoryStatistics(totalCategories, activeCategories, rootCategories);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateSlugUniqueness(String slug) {
        validateSlugUniqueness(slug, null);
    }

    private void validateSlugUniqueness(String slug, UUID excludeId) {
        boolean exists = (excludeId == null) 
            ? categoryRepository.existsBySlugIgnoreCase(slug)
            : categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, excludeId);
            
        if (exists) {
            throw new CategorySlugAlreadyExistsException("Category slug already exists: " + slug);
        }
    }

    private void validateNoCircularReference(UUID categoryId, Category proposedParent) {
        Category current = proposedParent;
        while (current != null) {
            if (current.getId().equals(categoryId)) {
                throw new CircularCategoryReferenceException("Circular reference detected: category cannot be its own ancestor");
            }
            current = current.getParent();
        }
    }

    // ========== INNER CLASSES / RECORDS ==========

    public record CategoryAnalytics(
        CategoryResponse category,
        long productCount
    ) {}

    public record CategoryStatistics(
        long totalCategories,
        long activeCategories,
        long rootCategories
    ) {}

    // ========== CUSTOM EXCEPTIONS ==========

    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException(String message) {
            super(message);
        }
    }

    public static class CategorySlugAlreadyExistsException extends RuntimeException {
        public CategorySlugAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class CategoryHasProductsException extends RuntimeException {
        public CategoryHasProductsException(String message) {
            super(message);
        }
    }

    public static class CategoryHasChildrenException extends RuntimeException {
        public CategoryHasChildrenException(String message) {
            super(message);
        }
    }

    public static class CircularCategoryReferenceException extends RuntimeException {
        public CircularCategoryReferenceException(String message) {
            super(message);
        }
    }
}
