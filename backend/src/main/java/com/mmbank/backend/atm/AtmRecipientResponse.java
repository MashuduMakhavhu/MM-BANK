package com.mmbank.backend.atm;

public record AtmRecipientResponse(
        String accountNumber,
        String accountHolderName
) {
}
