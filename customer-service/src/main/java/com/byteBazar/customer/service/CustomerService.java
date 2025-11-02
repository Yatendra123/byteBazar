package com.byteBazar.customer.service;

import com.byteBazar.customer.domain.Customer;
import com.byteBazar.customer.repository.CustomerRepository;
import com.byteBazar.customer.web.dto.CustomerCreateRequest;
import com.byteBazar.customer.web.dto.CustomerResponse;
import com.byteBazar.customer.web.dto.CustomerUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public CustomerResponse getById(UUID id) {
        Customer c = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        return CustomerResponse.from(c);
    }

    public Page<CustomerResponse> search(String query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Customer> result;
        if (query == null || query.isBlank()) {
            result = repository.findAll(pageable);
        } else {
            String q = query.trim();
            result = repository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(q, q, pageable);
        }
        return result.map(CustomerResponse::from);
    }

    public CustomerResponse create(CustomerCreateRequest req) {
        repository.findByEmailIgnoreCase(req.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("Email already exists: " + req.email());
        });
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setFirstName(req.firstName());
        c.setLastName(req.lastName());
        c.setEmail(req.email());
        c.setPhone(req.phone());
        return CustomerResponse.from(repository.save(c));
    }

    public CustomerResponse update(UUID id, CustomerUpdateRequest req) {
        Customer c = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        if (req.firstName() != null && !req.firstName().isBlank()) {
            c.setFirstName(req.firstName());
        }
        if (req.lastName() != null && !req.lastName().isBlank()) {
            c.setLastName(req.lastName());
        }
        if (req.email() != null && !req.email().isBlank()) {
            repository.findByEmailIgnoreCase(req.email()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Email already exists: " + req.email());
                }
            });
            c.setEmail(req.email());
        }
        if (req.phone() != null) {
            c.setPhone(req.phone());
        }
        return CustomerResponse.from(repository.save(c));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found: " + id);
        }
        repository.deleteById(id);
    }
}
