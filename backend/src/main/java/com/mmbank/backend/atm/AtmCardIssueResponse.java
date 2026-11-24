package com.mmbank.backend.atm;

import java.time.LocalDate;

public record AtmCardIssueResponse(
        String cardNumber,
        String accountNumber,
        LocalDate expiryDate,
        String message
) {
}
