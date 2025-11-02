package com.byteBazar.customer.web.dto;

import jakarta.validation.constraints.Email;

public record CustomerUpdateRequest(
        String firstName,
        String lastName,
        @Email String email,
        String phone
) {}
