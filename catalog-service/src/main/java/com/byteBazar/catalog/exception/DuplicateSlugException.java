package com.byteBazar.catalog.exception;

/**
 * Exception thrown when attempting to create a duplicate slug
 */
public class DuplicateSlugException extends CatalogException {
    
    public DuplicateSlugException(String message) {
        super(message);
    }
    
    public DuplicateSlugException(String entityType, String slug) {
        super(String.format("Duplicate slug '%s' for %s", slug, entityType));
    }
}
