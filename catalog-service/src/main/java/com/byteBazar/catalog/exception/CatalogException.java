package com.byteBazar.catalog.exception;

/**
 * Base exception class for Catalog Service
 */
public class CatalogException extends RuntimeException {
    
    public CatalogException(String message) {
        super(message);
    }
    
    public CatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
