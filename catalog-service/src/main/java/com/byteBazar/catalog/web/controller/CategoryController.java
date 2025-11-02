package com.byteBazar.catalog.web.controller;

import com.byteBazar.catalog.service.CategoryService;
import com.byteBazar.catalog.web.dto.CategoryCreateRequest;
import com.byteBazar.catalog.web.dto.CategoryResponse;
import com.byteBazar.catalog.web.dto.CategoryUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Category management.
 * Provides comprehensive CRUD operations and hierarchy management.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categories", description = "Category management operations")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all active categories", description = "Retrieve all active categories ordered by display order and name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.info("GET /api/v1/categories - Get all active categories");
        List<CategoryResponse> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/root")
    @Operation(summary = "Get root categories", description = "Retrieve root categories with their immediate children")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Root categories retrieved successfully")
    })
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        log.info("GET /api/v1/categories/root - Get root categories");
        List<CategoryResponse> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve a specific category by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getCategoryById(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID id) {
        log.info("GET /api/v1/categories/{} - Get category by ID", id);
        CategoryResponse category = categoryService.getById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieve a category by its URL-friendly slug")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getCategoryBySlug(
            @Parameter(description = "Category slug", required = true)
            @PathVariable String slug) {
        log.info("GET /api/v1/categories/slug/{} - Get category by slug", slug);
        CategoryResponse category = categoryService.getBySlug(slug);
        return ResponseEntity.ok(category);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER')")
    @Operation(summary = "Create new category", description = "Create a new product category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Category created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Category slug already exists"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<CategoryResponse> createCategory(
            @Parameter(description = "Category creation data", required = true)
            @Valid @RequestBody CategoryCreateRequest request) {
        log.info("POST /api/v1/categories - Create category: {}", request.name());
        CategoryResponse category = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER')")
    @Operation(summary = "Update category", description = "Update an existing category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "409", description = "Category slug already exists"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<CategoryResponse> updateCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Category update data", required = true)
            @Valid @RequestBody CategoryUpdateRequest request) {
        log.info("PUT /api/v1/categories/{} - Update category", id);
        CategoryResponse category = categoryService.update(id, request);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category", description = "Delete a category (only if no products or children exist)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "409", description = "Category has products or children"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID id) {
        log.info("DELETE /api/v1/categories/{} - Delete category", id);
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get child categories", description = "Retrieve all child categories of a specific parent")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Child categories retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Parent category not found")
    })
    public ResponseEntity<List<CategoryResponse>> getChildCategories(
            @Parameter(description = "Parent category ID", required = true)
            @PathVariable UUID id) {
        log.info("GET /api/v1/categories/{}/children - Get child categories", id);
        List<CategoryResponse> children = categoryService.getChildCategories(id);
        return ResponseEntity.ok(children);
    }

    @GetMapping("/{id}/hierarchy")
    @Operation(summary = "Get category with full hierarchy", description = "Retrieve category with all its children and product counts")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category hierarchy retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getCategoryWithHierarchy(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID id) {
        log.info("GET /api/v1/categories/{}/hierarchy - Get category with hierarchy", id);
        CategoryResponse category = categoryService.getCategoryWithFullHierarchy(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/search")
    @Operation(summary = "Search categories", description = "Search categories by name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    public ResponseEntity<List<CategoryResponse>> searchCategories(
            @Parameter(description = "Search query")
            @RequestParam(value = "q", required = false) String query) {
        log.info("GET /api/v1/categories/search - Search categories with query: {}", query);
        List<CategoryResponse> categories = categoryService.searchCategories(query);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/analytics/product-counts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER')")
    @Operation(summary = "Get categories with product counts", description = "Retrieve categories with their product counts for analytics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics data retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<CategoryService.CategoryAnalytics>> getCategoriesWithProductCounts() {
        log.info("GET /api/v1/categories/analytics/product-counts - Get category analytics");
        List<CategoryService.CategoryAnalytics> analytics = categoryService.getCategoriesWithProductCounts();
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/analytics/low-stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER')")
    @Operation(summary = "Get categories with low stock products", description = "Retrieve categories that have products with low stock")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Low stock categories retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<CategoryResponse>> getCategoriesWithLowStock() {
        log.info("GET /api/v1/categories/analytics/low-stock - Get categories with low stock");
        List<CategoryResponse> categories = categoryService.getCategoriesWithLowStockProducts();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/analytics/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CATALOG_MANAGER')")
    @Operation(summary = "Get category statistics", description = "Retrieve overall category statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<CategoryService.CategoryStatistics> getCategoryStatistics() {
        log.info("GET /api/v1/categories/analytics/statistics - Get category statistics");
        CategoryService.CategoryStatistics stats = categoryService.getCategoryStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/product-count")
    @Operation(summary = "Get total products in category tree", description = "Count total products in category including subcategories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product count retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<Long> getTotalProductCount(
            @Parameter(description = "Category ID", required = true)
            @PathVariable UUID id) {
        log.info("GET /api/v1/categories/{}/product-count - Get total product count", id);
        long count = categoryService.getTotalProductsInCategoryTree(id);
        return ResponseEntity.ok(count);
    }
}
