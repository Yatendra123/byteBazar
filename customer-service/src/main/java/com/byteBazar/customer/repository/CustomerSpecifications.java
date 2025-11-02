package com.byteBazar.customer.repository;

import com.byteBazar.customer.domain.Customer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic Customer queries.
 * This demonstrates expert-level Spring Data JPA usage with type-safe queries.
 */
public class CustomerSpecifications {

    /**
     * Creates a specification for searching customers by multiple criteria.
     * This is a powerful pattern for building dynamic, type-safe queries.
     */
    public static Specification<Customer> searchCustomers(String query, 
                                                         Instant createdAfter, 
                                                         Instant createdBefore,
                                                         Boolean hasPhone) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Text search across name and email (demonstrates LIKE queries)
            if (query != null && !query.trim().isEmpty()) {
                String searchPattern = "%" + query.trim().toLowerCase() + "%";
                Predicate nameSearch = criteriaBuilder.or(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("firstName")), 
                        searchPattern
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("lastName")), 
                        searchPattern
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")), 
                        searchPattern
                    )
                );
                predicates.add(nameSearch);
            }

            // Date range filtering (demonstrates date comparisons)
            if (createdAfter != null) {
                predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAfter)
                );
            }
            
            if (createdBefore != null) {
                predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdBefore)
                );
            }

            // Conditional filtering (demonstrates null checks)
            if (hasPhone != null) {
                if (hasPhone) {
                    predicates.add(
                        criteriaBuilder.isNotNull(root.get("phone"))
                    );
                } else {
                    predicates.add(
                        criteriaBuilder.isNull(root.get("phone"))
                    );
                }
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification for finding customers by email domain.
     * Useful for B2B scenarios where you want customers from specific companies.
     */
    public static Specification<Customer> byEmailDomain(String domain) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (domain == null || domain.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("email")), 
                "%@" + domain.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification for finding recently registered customers.
     * Demonstrates date calculations and business logic in specifications.
     */
    public static Specification<Customer> recentlyRegistered(int days) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Instant cutoffDate = Instant.now().minusSeconds(days * 24L * 60L * 60L);
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), cutoffDate);
        };
    }

    /**
     * Complex specification combining multiple conditions.
     * This shows how to build reusable, composable query logic.
     */
    public static Specification<Customer> activeCustomersFromDomain(String domain) {
        return Specification.where(byEmailDomain(domain))
                          .and(recentlyRegistered(90)); // Active = registered in last 90 days
    }
}
