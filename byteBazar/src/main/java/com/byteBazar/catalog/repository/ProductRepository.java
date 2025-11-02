package com.byteBazar.catalog.repository;

import com.byteBazar.catalog.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Product> findBySellerId(UUID sellerId, Pageable pageable);

    @Query(value = "SELECT * FROM products WHERE search_tsv @@ plainto_tsquery('english', :q) ORDER BY ts_rank(search_tsv, plainto_tsquery('english', :q)) DESC",
           countQuery = "SELECT count(*) FROM products WHERE search_tsv @@ plainto_tsquery('english', :q)",
           nativeQuery = true)
    Page<Product> searchFullText(@Param("q") String query, Pageable pageable);
}
