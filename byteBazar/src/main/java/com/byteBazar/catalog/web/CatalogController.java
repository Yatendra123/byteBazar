package com.byteBazar.catalog.web;

import com.byteBazar.catalog.service.CatalogService;
import com.byteBazar.catalog.web.dto.ProductCreateRequest;
import com.byteBazar.catalog.web.dto.ProductResponse;
import com.byteBazar.catalog.web.dto.ProductUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return catalogService.getById(id);
    }

    @GetMapping
    public Page<ProductResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return catalogService.search(query, page, size);
    }

    @GetMapping("/seller/{sellerId}")
    public Page<ProductResponse> listBySeller(
            @PathVariable UUID sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return catalogService.listBySeller(sellerId, page, size);
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest req) {
        return catalogService.create(req);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @RequestBody ProductUpdateRequest req) {
        return catalogService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        catalogService.delete(id);
    }
}
