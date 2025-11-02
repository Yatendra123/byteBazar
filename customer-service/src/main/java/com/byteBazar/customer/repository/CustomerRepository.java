package com.byteBazar.customer.repository;

import com.byteBazar.customer.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced CustomerRepository demonstrating expert-level Spring Data JPA features:
 * - JpaSpecificationExecutor for dynamic queries
 * - Custom JPQL queries with @Query
 * - Method query derivation
 * - Performance-optimized projections
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
    
    // Basic finder methods (existing)
    Optional<Customer> findByEmailIgnoreCase(String email);
    Page<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String first, String last, Pageable pageable);
    
    // Advanced custom queries using @Query annotation
    @Query("SELECT c FROM Customer c WHERE c.createdAt >= :since ORDER BY c.createdAt DESC")
    List<Customer> findRecentCustomers(@Param("since") Instant since);
    
    @Query("SELECT c FROM Customer c WHERE LOWER(c.email) LIKE LOWER(CONCAT('%@', :domain, '%'))")
    List<Customer> findByEmailDomain(@Param("domain") String domain);
    
    // Projection for lightweight data transfer (only IDs and emails)
    @Query("SELECT c.id as id, c.email as email, c.firstName as firstName, c.lastName as lastName FROM Customer c")
    List<CustomerSummaryProjection> findAllSummaries();
    
    // Count queries for analytics
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt >= :since")
    long countRecentCustomers(@Param("since") Instant since);
    
    // Advanced method query derivation
    List<Customer> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);
    boolean existsByEmailIgnoreCase(String email);
    
    /**
     * Projection interface for lightweight customer data.
     * This demonstrates Spring Data JPA projections for performance optimization.
     */
    interface CustomerSummaryProjection {
        UUID getId();
        String getEmail();
        String getFirstName();
        String getLastName();
    }
}
