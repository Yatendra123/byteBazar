package com.byteBazar.catalog.exception;

/**
 * Exception thrown when attempting to perform operations on inactive categories
 */
public class InactiveCategoryException extends CatalogException {
    
    public InactiveCategoryException(String message) {
        super(message);
    }
}
