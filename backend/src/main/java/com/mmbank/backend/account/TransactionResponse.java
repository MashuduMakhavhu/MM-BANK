package com.mmbank.backend.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long transactionId,
    String accountNumber,
    TransactionType type,
    BigDecimal amount,
    BigDecimal balanceAfter,
    LocalDateTime createdAt
) {
}
