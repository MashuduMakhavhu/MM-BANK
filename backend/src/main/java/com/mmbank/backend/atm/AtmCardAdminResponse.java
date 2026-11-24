package com.mmbank.backend.atm;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AtmCardAdminResponse(
        String cardNumber,
        String accountNumber,
        AtmCardStatus status,
        LocalDate expiryDate,
        LocalDateTime createdAt
) {}
