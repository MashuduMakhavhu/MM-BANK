package com.mmbank.backend.atm;

import com.mmbank.backend.account.AccountType;

public record AtmLoginResponse(
        String accountNumber,
        String accountHolderName,
        AccountType accountType,
        String maskedCardNumber
) {
}
