package com.byteBazar.catalog.service;

import com.byteBazar.catalog.domain.Product;
import com.byteBazar.catalog.repository.ProductRepository;
import com.byteBazar.catalog.web.dto.ProductCreateRequest;
import com.byteBazar.catalog.web.dto.ProductResponse;
import com.byteBazar.catalog.web.dto.ProductUpdateRequest;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CatalogService {

    private final ProductRepository productRepository;

    public CatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(cacheNames = "product", key = "#id")
    public ProductResponse getById(UUID id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return ProductResponse.from(p);
    }

    @Cacheable(cacheNames = "product_search", key = "(#query?:'') + ':' + #page + ':' + #size")
    public Page<ProductResponse> search(String query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Product> result;
        if (query == null || query.isBlank()) {
            result = productRepository.findAll(pageable);
        } else {
            result = productRepository.searchFullText(query.trim(), pageable);
        }
        return result.map(ProductResponse::from);
    }

    @Cacheable(cacheNames = "product_search", key = "'seller:' + #sellerId + ':' + #page + ':' + #size")
    public Page<ProductResponse> listBySeller(UUID sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return productRepository.findBySellerId(sellerId, pageable).map(ProductResponse::from);
    }

    @CacheEvict(cacheNames = "product_search", allEntries = true)
    @CachePut(cacheNames = "product", key = "#result.id")
    public ProductResponse create(ProductCreateRequest req) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setSellerId(req.sellerId());
        p.setTitle(req.title());
        p.setDescription(req.description());
        p.setPrice(req.price());
        return ProductResponse.from(productRepository.save(p));
    }

    @Caching(
        evict = {
            @CacheEvict(cacheNames = "product_search", allEntries = true)
        },
        put = {
            @CachePut(cacheNames = "product", key = "#result.id")
        }
    )
    public ProductResponse update(UUID id, ProductUpdateRequest req) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        if (req.title() != null && !req.title().isBlank()) {
            p.setTitle(req.title());
        }
        if (req.description() != null) {
            p.setDescription(req.description());
        }
        if (req.price() != null) {
            p.setPrice(req.price());
        }
        return ProductResponse.from(productRepository.save(p));
    }

    @Caching(
        evict = {
            @CacheEvict(cacheNames = "product", key = "#id"),
            @CacheEvict(cacheNames = "product_search", allEntries = true)
        }
    )
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }
}
