package com.byteBazar.catalog.exception;

/**
 * Exception thrown when a category is not found
 */
public class CategoryNotFoundException extends CatalogException {
    
    public CategoryNotFoundException(String message) {
        super(message);
    }
    
    public CategoryNotFoundException(String categoryId) {
        super("Category not found with id: " + categoryId);
    }
}
