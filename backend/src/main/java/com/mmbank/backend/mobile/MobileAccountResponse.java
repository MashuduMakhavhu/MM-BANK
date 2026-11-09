package com.mmbank.backend.mobile;

import java.math.BigDecimal;

public record MobileAccountResponse(
    String customerNumber,
    String fullName,
    String accountNumber,
    String accountType,
    BigDecimal balance
) {}
