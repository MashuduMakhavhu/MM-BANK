package com.mmbank.backend.account;

import java.math.BigDecimal;

public record AccountBalanceResponse(
        String accountNumber,
        String accountHolderName,
        AccountType accountType,
        BigDecimal balance
) {
}
