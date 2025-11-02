package com.byteBazar.catalog.exception;

/**
 * Exception thrown when a product is not found
 */
public class ProductNotFoundException extends CatalogException {
    
    public ProductNotFoundException(String message) {
        super(message);
    }
    
    public ProductNotFoundException(String productId) {
        super("Product not found with id: " + productId);
    }
}
