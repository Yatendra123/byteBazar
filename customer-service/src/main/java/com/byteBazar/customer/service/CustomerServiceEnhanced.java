package com.byteBazar.customer.service;

import com.byteBazar.customer.domain.Customer;
import com.byteBazar.customer.repository.CustomerRepository;
import com.byteBazar.customer.repository.CustomerSpecifications;
import com.byteBazar.customer.web.dto.CustomerCreateRequest;
import com.byteBazar.customer.web.dto.CustomerResponse;
import com.byteBazar.customer.web.dto.CustomerSearchRequest;
import com.byteBazar.customer.web.dto.CustomerUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enhanced CustomerService demonstrating expert-level Spring Boot patterns:
 * - JPA Specifications for dynamic queries
 * - Advanced pagination and sorting
 * - Transactional management
 * - Performance-optimized projections
 * - Business logic encapsulation
 */
@Service
@Transactional(readOnly = true) // Default to read-only for performance
public class CustomerServiceEnhanced {

    private final CustomerRepository repository;

    public CustomerServiceEnhanced(CustomerRepository repository) {
        this.repository = repository;
    }

    // ========== BASIC CRUD OPERATIONS ==========

    public CustomerResponse getById(UUID id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));
        return CustomerResponse.from(customer);
    }

    @Transactional // Override read-only for write operations
    public CustomerResponse create(CustomerCreateRequest request) {
        // Business validation
        validateEmailUniqueness(request.email());
        
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        
        Customer saved = repository.save(customer);
        return CustomerResponse.from(saved);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerUpdateRequest request) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));
        
        // Update only provided fields (partial update pattern)
        if (request.firstName() != null && !request.firstName().isBlank()) {
            customer.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            customer.setLastName(request.lastName());
        }
        if (request.email() != null && !request.email().isBlank()) {
            validateEmailUniqueness(request.email(), id);
            customer.setEmail(request.email());
        }
        if (request.phone() != null) {
            customer.setPhone(request.phone());
        }
        
        Customer saved = repository.save(customer);
        return CustomerResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new CustomerNotFoundException("Customer not found: " + id);
        }
        repository.deleteById(id);
    }

    // ========== ADVANCED SEARCH WITH SPECIFICATIONS ==========

    /**
     * Advanced search using JPA Specifications.
     * This demonstrates how to build complex, dynamic queries.
     */
    public Page<CustomerResponse> advancedSearch(CustomerSearchRequest searchRequest) {
        // Apply default values
        CustomerSearchRequest request = searchRequest.withDefaults();
        
        // Build dynamic specification
        Specification<Customer> spec = CustomerSpecifications.searchCustomers(
            request.query(),
            request.createdAfter(),
            request.createdBefore(),
            request.hasPhone()
        );
        
        // Add email domain filter if provided
        if (request.emailDomain() != null && !request.emailDomain().isBlank()) {
            spec = spec.and(CustomerSpecifications.byEmailDomain(request.emailDomain()));
        }
        
        // Create pageable with sorting
        Sort sort = createSort(request.sortBy(), request.sortDirection());
        Pageable pageable = PageRequest.of(request.page(), request.size(), sort);
        
        // Execute query and transform results
        Page<Customer> customers = repository.findAll(spec, pageable);
        return customers.map(CustomerResponse::from);
    }

    // ========== ANALYTICS AND REPORTING METHODS ==========

    /**
     * Get customer registration statistics.
     * Demonstrates business logic methods using repository analytics.
     */
    public CustomerRegistrationStats getRegistrationStats(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        
        long totalCustomers = repository.count();
        long recentCustomers = repository.countRecentCustomers(cutoff);
        
        return new CustomerRegistrationStats(
            totalCustomers,
            recentCustomers,
            days,
            calculateGrowthRate(recentCustomers, totalCustomers - recentCustomers)
        );
    }

    /**
     * Find customers by company domain.
     * Useful for B2B analysis and targeted marketing.
     */
    public List<CustomerResponse> findByCompanyDomain(String domain) {
        List<Customer> customers = repository.findByEmailDomain(domain);
        return customers.stream()
                       .map(CustomerResponse::from)
                       .collect(Collectors.toList());
    }

    /**
     * Get lightweight customer summaries for performance.
     * Demonstrates projection usage for optimized data transfer.
     */
    public List<CustomerSummary> getCustomerSummaries() {
        return repository.findAllSummaries()
                        .stream()
                        .map(projection -> new CustomerSummary(
                            projection.getId(),
                            projection.getEmail(),
                            projection.getFirstName() + " " + projection.getLastName()
                        ))
                        .collect(Collectors.toList());
    }

    /**
     * Find recently active customers.
     * Demonstrates combining specifications for business logic.
     */
    public List<CustomerResponse> findRecentlyActive(String emailDomain) {
        Specification<Customer> spec = CustomerSpecifications.activeCustomersFromDomain(emailDomain);
        List<Customer> customers = repository.findAll(spec);
        return customers.stream()
                       .map(CustomerResponse::from)
                       .collect(Collectors.toList());
    }

    // ========== PRIVATE HELPER METHODS ==========

    @Transactional(readOnly = true)
    private void validateEmailUniqueness(String email) {
        validateEmailUniqueness(email, null);
    }

    @Transactional(readOnly = true)
    private void validateEmailUniqueness(String email, UUID excludeId) {
        repository.findByEmailIgnoreCase(email)
                 .ifPresent(existing -> {
                     if (excludeId == null || !existing.getId().equals(excludeId)) {
                         throw new CustomerEmailAlreadyExistsException("Email already exists: " + email);
                     }
                 });
    }

    private Sort createSort(String sortBy, String sortDirection) {
        // Validate sort field to prevent injection attacks
        String validatedSortBy = validateSortField(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return Sort.by(direction, validatedSortBy);
    }

    private String validateSortField(String sortBy) {
        // Whitelist allowed sort fields for security
        return switch (sortBy) {
            case "firstName", "lastName", "email", "createdAt" -> sortBy;
            default -> "createdAt"; // Default safe fallback
        };
    }

    private double calculateGrowthRate(long recent, long older) {
        if (older == 0) return 0.0;
        return ((double) recent / older) * 100.0;
    }

    // ========== INNER CLASSES / RECORDS ==========

    public record CustomerRegistrationStats(
        long totalCustomers,
        long recentCustomers,
        int periodDays,
        double growthRate
    ) {}

    public record CustomerSummary(
        UUID id,
        String email,
        String fullName
    ) {}

    // ========== CUSTOM EXCEPTIONS ==========

    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(String message) {
            super(message);
        }
    }

    public static class CustomerEmailAlreadyExistsException extends RuntimeException {
        public CustomerEmailAlreadyExistsException(String message) {
            super(message);
        }
    }
}
