package com.mmbank.backend.customer;

import java.time.LocalDateTime;

public record CustomerResponse(
        String customerNumber,
        String fullName,
        String email,
        String phoneNumber,
        LocalDateTime createdAt
) {}
